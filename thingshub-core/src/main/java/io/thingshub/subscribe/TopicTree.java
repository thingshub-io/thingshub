package io.thingshub.subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

public class TopicTree<T> {

	static class TrieNode<T> {

		private final Object memberMutex = new Object();

		private final Object childrenMutex = new Object();

		private final AtomicInteger memberCount = new AtomicInteger(0);

		private TrieNode<T> parent;

		private final String token;

		private int maxMembers;

		private final int hashCode;

		private volatile Map<String, TrieNode<T>> children;

		private volatile Set<T> members;

		protected TrieNode(final TrieNode<T> parent, final String token, int maxMembers) {
			this.parent = parent;
			this.token = token;
			this.maxMembers = maxMembers;

			// hash
			if (token != null) {
				int result = token.hashCode();
				result = 31 * result + (parent != null ? parent.hashCode() : 0);
				hashCode = result;
			} else {
				hashCode = 0;
			}

//			int childSize = (this.parent != null && this.parent.getParent() == null && this.theTree.getSeparator().equals(this.token)) ? DFT_ROOT_CHILD_SIZE
//					: DFT_CHILD_SIZE;
			this.children = new HashMap<>(32);
		}

		@SuppressWarnings("unchecked")
		public TrieNode<T> addChild(final String token, final T... members) {
			TrieNode<T> child;
			synchronized (childrenMutex) {
				if (!children.containsKey(token)) {
					child = new TrieNode<>(this, token, maxMembers);
					children.put(token, child);
				} else {
					child = children.get(token);
				}
			}

			if (members != null && members.length > 0) {
				child.addMembers(members);
			}

			return child;
		}

		public TrieNode<T> getChild(final String path) {
			return children == null ? null : children.get(path);
		}

		public boolean hasChild(String path) {
			return children != null && children.containsKey(path);
		}

		public boolean hasChildren() {
			return children != null && !children.isEmpty();
		}

		public boolean hasMembers() {
			return members != null && !members.isEmpty();
		}

		public TrieNode<T> getParent() {
			return parent;
		}

		public String getToken() {
			return token;
		}

		public int getMemberCount() {
			return memberCount.get();
		}

		public void removeChild(TrieNode<T> node) {
			if (children != null && children.containsKey(node.getToken())) {
				synchronized (childrenMutex) {
					TrieNode<T> removed = children.remove(node.getToken());
					if (removed == node) {
						removed.clear();
					} else {
					}
				}
				node.parent = null;
			}
		}

		@SuppressWarnings("unchecked")
		public void addMembers(T... _members) {
			if (members == null && _members != null && _members.length > 0) {
				synchronized (memberMutex) {
					if (members == null) {
						members = ConcurrentHashMap.newKeySet();
					}
				}
			}

			if (_members != null && _members.length > 0) {
				if ((members.size() + _members.length) > maxMembers) {
					throw new TopicException("tree level member limit exceeded");
				}

				for (T m : _members) {
					if (m != null) {
						if (members.add(m)) {
							memberCount.incrementAndGet();
						}
					}
				}
			}
		}

		public void clear() {
			if (members != null) {
				members.clear();
				members = null;
				memberCount.set(0);
			}
		}

		public boolean removeMember(T member) {
			if (members != null) {
				boolean removed = members.remove(member);
				if (removed)
					memberCount.decrementAndGet();
				return removed;
			}
			return false;
		}

		public Set<T> getMembers() {
			if (members == null || parent == null) {
				return Collections.emptySet();
			}

			return members;
		}

		public Set<String> getChildPaths() {
			if (children == null) {
				return Collections.emptySet();
			}

			// TODO fix me
			synchronized (childrenMutex) {
				return Collections.unmodifiableSet(children.keySet());
			}
		}

		public boolean isLeaf() {
			return !hasChildren();
		}

		@Override
		public String toString() {
			return "TrieNode{" + "token='" + token + '\'' + ", isLeaf=" + isLeaf() + ", isRoot=" + (parent == null) + ", memberCount=" + memberCount + '}';
		}

		public String toPath(boolean climb) {
			if (climb) {
				List<String> l = new ArrayList<>();
				TrieNode<T> leaf = this;
				while (leaf != null) {
					if (leaf.getParent() == null)
						break;
					l.add(leaf.getToken());
					leaf = leaf.getParent();
				}
				StringBuilder sb = new StringBuilder();
				for (int i = l.size(); i-- > 0;) {
					sb.append(l.get(i));
				}
				return sb.toString();
			} else {
				return token;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TrieNode<?> trieNode = (TrieNode<?>) o;
			if (!token.equals(trieNode.getToken()))
				return false;
			return Objects.equals(parent, trieNode.parent);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	};

	private static final char DEFAULT_SEPARATOR_CHAR = '/';
	private static final String DEFAULT_WILDPATH = "+";
	private static final String DEFAULT_WILDCARD = "#";
	private static final int DEFAULT_MAX_TOKENS = 1024;
	private static final int DEFAULT_MAX_MEMBERS_AT_LEVEL = 1024 * 128;

	private boolean selfPruning;

	private char separatorChar;

	@Getter
	private String separator;

	private TrieNode<T> root;

	private int maxTokens;

	@Getter
	private int maxMembersAtLevel;

	private String wildCard;

	private String wildPath;

	public TopicTree() {
		this.separatorChar = DEFAULT_SEPARATOR_CHAR;
		this.separator = DEFAULT_SEPARATOR_CHAR + "";
		this.selfPruning = true;
		this.wildCard = DEFAULT_WILDCARD;
		this.wildPath = DEFAULT_WILDPATH;
		this.maxTokens = DEFAULT_MAX_TOKENS;
		this.maxMembersAtLevel = DEFAULT_MAX_MEMBERS_AT_LEVEL;
		this.root = new TrieNode<T>(null, null, maxMembersAtLevel);
	}

	public TopicTree<T> withSeparator(char separatorChar) {
		this.separatorChar = separatorChar;
		this.separator = separatorChar + "";
		return this;
	}

	public TopicTree<T> withSelfPruning(boolean selfPruning) {
		this.selfPruning = selfPruning;
		return this;
	}

	public TopicTree<T> withWildCard(String wildCard) {
		this.wildCard = wildCard;
		return this;
	}

	public TopicTree<T> withWildPath(String wildPath) {
		this.wildPath = wildPath;
		return this;
	}

	public TopicTree<T> withMaxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
		return this;
	}

	public TopicTree<T> withMaxMembersAtLevel(int maxMembersAtLevel) {
		this.maxMembersAtLevel = maxMembersAtLevel;
		return this;
	}

	@SuppressWarnings("unchecked")
	public TrieNode<T> add(final String topicFilter, final T... members) {
		String[] tokens = split(topicFilter);

		if (tokens.length > maxTokens)
			throw new TopicException("tree path limit exceeded");

		if (members != null && members.length > maxMembersAtLevel)
			throw new TopicException("tree member limit exceeded");

		TrieNode<T> node = root;
		for (int i = 0; i < tokens.length; i++) {
			if (node == null) {
				throw new TopicException("tree state error");
			}

			if (tokens[i] == null) {
				throw new TopicException("tree state error");
			}

			if (i == tokens.length - 1) {
				node = node.addChild(tokens[i], members);
			} else {
				node = node.addChild(tokens[i]);
			}
		}
		return node;
	}

	public boolean remove(final String topicFilter, T member) throws TopicException {
		if (topicFilter == null || topicFilter.length() == 0) {
			throw new TopicException("invalid subscription topic");
		}

		TrieNode<T> node = getNodeIfExists(topicFilter);
		if (node != null) {
			boolean removed = node.removeMember(member);
			if (removed && selfPruning && node.getMembers().isEmpty() && !node.hasChildren()) {
				node.getParent().removeChild(node);
			}

			return removed;
		}

		return false;
	}

	public Set<T> match(final String topic) throws TopicException {
		if (root == null || !root.hasChildren()) {
			return Collections.emptySet();
		}

		return searchInternal(topic);
	}

	private final TrieNode<T> getNodeIfExists(final String topic) {
		String[] tokens = split(topic);
		TrieNode<T> node = root;
		for (int i = 0; i < tokens.length; i++) {
			node = node.getChild(tokens[i]);
			if (node == null) {
				return null;
			}
		}

		return node;
	}

	protected final Set<T> searchInternal(final String topic) {
		Set<T> members = new HashSet<>();
		String[] tokens = split(topic);
		searchChildren(root, tokens, members);

		return members;
	}

	protected void searchChildren(TrieNode<T> node, String[] tokens, Set<T> members) {
		// root wildpath logic
		if (node.getParent() == null) {
			TrieNode<T> wildPathNode = node.getChild(wildPath);
			if (wildPathNode != null) {
				if (tokens.length <= 1) {
					if (wildPathNode != null && wildPathNode.isLeaf()) {
						copyMembersNullSafe(members, wildPathNode);
					}
				}
				// a + at root is equal to nothing at root
				searchChildren(wildPathNode, tokens, members);
			}

		}

		// wildpath leaf logic
		if (node.isLeaf() && wildPath.equals(node.getToken()) && tokens.length <= 1) {
			copyMembersNullSafe(members, node);
		}

		TrieNode<T> last = null;
		for (int i = 0; i < tokens.length; i++) {
			String currentToken = tokens[i];

			// check for wildpath token
			if (separator.equals(node.getToken()) || node.getParent() == null) {
				TrieNode<T> wildpath = node.getChild(wildPath);
				if (wildpath != null) {
					int start = i + 1;
					if (i == tokens.length - 1) {
						start = i;
					}
					String[] from = Arrays.copyOfRange(tokens, start, tokens.length);
					if (from.length > 0) {
						searchChildren(wildpath, from, members);
					} else {
						// weve run out of path tokens BUT we have a wildpath so we need to ensure we
						// dont have a sub forward
						readWildpathAtNextLevel(wildpath, members, true);
					}
				}
			}

			// check for wildcard
			if (separator.equals(node.getToken()) || node.getParent() == null) {
				TrieNode<T> wildCardNode = node.getChild(wildCard);
				if (wildCardNode != null) {
					copyMembersNullSafe(members, wildCardNode);
				}
			}

			node = node.getChild(currentToken);
			if (node == null) {
				break;
			}

			last = node;
		}

		if (last != null) {
			// check for wildcard at parent level
			readWildcardAtNextLevel(last, members);
		}

		// this is the direct match
		if (node != null) {
			copyMembersNullSafe(members, node);
		}
	}

	protected void readWildcardAtNextLevel(TrieNode<T> node, Set<T> members) {
		if (node != null) {
			if (!separator.equals(node.getToken())) {
				TrieNode<T> childNode = node.getChild(separator);
				if (childNode != null && separator.equals(childNode.getToken())) {
					childNode = childNode.getChild(wildCard);
					if (childNode != null && childNode.isLeaf()) {
						copyMembersNullSafe(members, childNode);
					}
				}
			}
		}
	}

	protected void readWildpathAtNextLevel(TrieNode<T> node, Set<T> members, boolean allowTraversal) {
		if (node != null) {
			if (separator.equals(node.getToken())) {
				TrieNode<T> childNode = node.getChild(wildPath);
				if (childNode != null && childNode.isLeaf()) {
					copyMembersNullSafe(members, childNode);
				}
			} else {
				if (allowTraversal) {
					TrieNode<T> childNode = node.getChild(separator);
					readWildpathAtNextLevel(childNode, members, false);
				}
			}
		}
	}

	protected Set<T> copyMembersNullSafe(Set<T> copyTo, TrieNode<T> copyFrom) {
		Set<T> members = null;
		if (copyFrom != null) {
			members = copyFrom.getMembers();
		}
		return copyMembersNullSafe(copyTo, members);
	}

	protected Set<T> copyMembersNullSafe(Set<T> copyTo, Set<T> copyFrom) {
		if (copyFrom != null && !copyFrom.isEmpty()) {
			if (copyTo == null)
				copyTo = new HashSet<>();
			copyTo.addAll(copyFrom);
		}
		return copyTo;
	}

	private String[] split(final String topic) {
		if (topic == null) {
			return null;
		}

		final int len = topic.length();
		if (len == 0) {
			return new String[0];
		}
		final List<String> list = new ArrayList<>();
		int i = 0;
		int start = 0;
		boolean match = false;
		while (i < len) {
			if (topic.charAt(i) == separatorChar) {
				if (match) {
					list.add(topic.substring(start, i));
					match = false;
				}
				list.add(separatorChar + "");
				start = ++i;
				continue;
			}
			match = true;
			i++;
		}
		if (match) {
			list.add(topic.substring(start, i));
		}

		return list.stream().toArray(String[]::new);
	}

}

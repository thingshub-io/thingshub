package io.thingshub.subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import io.thingshub.commons.SysException;
import lombok.Data;

public class PrefixSearchTree<T> {

	@Data
	static class TreeNode<T> {

		private String key;

		private List<TreeNode<T>> childern;

		private boolean real;

		private T value;

		public TreeNode() {
			key = "";
			childern = new ArrayList<>();
			real = false;
		}

		public int getNumberOfMatchingCharacters(String key) {
			int numberOfMatchingCharacters = 0;
			while (numberOfMatchingCharacters < key.length() && numberOfMatchingCharacters < this.getKey().length()) {
				if (key.charAt(numberOfMatchingCharacters) != this.getKey().charAt(numberOfMatchingCharacters)) {
					break;
				}
				numberOfMatchingCharacters++;
			}
			return numberOfMatchingCharacters;
		}

		@Override
		public String toString() {
			return key;
		}
	};

	static abstract class TreeVisitor<T, R> {

		protected R result;

		public TreeVisitor() {
			this.result = null;
		}

		public TreeVisitor(R initialValue) {
			this.result = initialValue;
		}

		public R getResult() {
			return result;
		}

		abstract public void visit(String key, TreeNode<T> parent, TreeNode<T> node);
	};

	static class DuplicateException extends SysException {

		private static final long serialVersionUID = 3141795907493885706L;

		public DuplicateException(String msg) {
			super(msg);
		}
	};

	protected TreeNode<T> root;

	protected long size;

	public PrefixSearchTree() {
		root = new TreeNode<>();
		root.setKey("");
		size = 0;
	}

	public T find(String key) {
		TreeVisitor<T, T> visitor = new TreeVisitor<T, T>() {

			@Override
			public void visit(String key, TreeNode<T> parent, TreeNode<T> node) {
				if (node.isReal()) {
					result = node.getValue();
				}
			}

		};

		visit(key, visitor);

		return visitor.getResult();
	}

	public boolean replace(String key, final T value) {
		TreeVisitor<T, T> visitor = new TreeVisitor<T, T>() {
			@Override
			public void visit(String key, TreeNode<T> parent, TreeNode<T> node) {
				if (node.isReal()) {
					node.setValue(value);
					result = value;
				} else {
					result = null;
				}
			}
		};

		visit(key, visitor);

		return visitor.getResult() != null;
	}

	public boolean delete(String key) {
		TreeVisitor<T, Boolean> visitor = new TreeVisitor<T, Boolean>(Boolean.FALSE) {
			@Override
			public void visit(String key, TreeNode<T> parent, TreeNode<T> node) {
				result = node.isReal();

				// if it is a real node
				if (result) {
					// If there no children of the node we need to delete it from the its parent
					// children list
					if (node.getChildern().size() == 0) {
						Iterator<TreeNode<T>> it = parent.getChildern().iterator();
						while (it.hasNext()) {
							if (it.next().getKey().equals(node.getKey())) {
								it.remove();
								break;
							}
						}

						// if parent is not real node and has only one child, then they need to be
						// merged.
						if (parent.getChildern().size() == 1 && parent.isReal() == false) {
							mergeNodes(parent, parent.getChildern().get(0));
						}
					} else if (node.getChildern().size() == 1) {
						// we need to merge the only child of this node with itself
						mergeNodes(node, node.getChildern().get(0));
					} else { // we jus need to mark the node as non real.
						node.setReal(false);
					}
				}
			}

			/**
			 * Merge a child into its parent node. Operation only valid if it is only child
			 * of the parent node and parent node is not a real node.
			 */
			private void mergeNodes(TreeNode<T> parent, TreeNode<T> child) {
				parent.setKey(parent.getKey() + child.getKey());
				parent.setReal(child.isReal());
				parent.setValue(child.getValue());
				parent.setChildern(child.getChildern());
			}

		};

		visit(key, visitor);
		if (visitor.getResult()) {
			size--;
		}

		return visitor.getResult().booleanValue();
	}

	public boolean insert(String key, T value, boolean replace) {
		try {
			insert(key, root, value, replace);
			size++;
		} catch (DuplicateException e) {
			return false;
		}

		return true;
	}

	/**
	 * Recursively insert the key in the radix tree.
	 */
	private void insert(String key, TreeNode<T> node, T value, boolean replace) throws DuplicateException {

		int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(key);

		// we are either at the root node or we need to go down the tree
		if (node.getKey().equals("") == true || numberOfMatchingCharacters == 0
				|| (numberOfMatchingCharacters < key.length() && numberOfMatchingCharacters >= node.getKey().length())) {
			boolean flag = false;
			String newText = key.substring(numberOfMatchingCharacters, key.length());
			for (TreeNode<T> child : node.getChildern()) {
				if (child.getKey().startsWith(newText.charAt(0) + "")) {
					flag = true;
					insert(newText, child, value, replace);
					break;
				}
			}

			// just add the node as the child of the current node
			if (flag == false) {
				TreeNode<T> n = new TreeNode<T>();
				n.setKey(newText);
				n.setReal(true);
				n.setValue(value);

				node.getChildern().add(n);
			}
		}
		// there is a exact match just make the current node as data node
		else if (numberOfMatchingCharacters == key.length() && numberOfMatchingCharacters == node.getKey().length()) {
			if (node.isReal() == true) {
				if (replace) {
					node.setValue(value);
				}

				throw new DuplicateException("Duplicate key");
			}

			node.setReal(true);
			node.setValue(value);
		}
		// This node need to be split as the key to be inserted
		// is a prefix of the current node key
		else if (numberOfMatchingCharacters > 0 && numberOfMatchingCharacters < node.getKey().length()) {
			TreeNode<T> n1 = new TreeNode<T>();
			n1.setKey(node.getKey().substring(numberOfMatchingCharacters, node.getKey().length()));
			n1.setReal(node.isReal());
			n1.setValue(node.getValue());
			n1.setChildern(node.getChildern());

			node.setKey(key.substring(0, numberOfMatchingCharacters));
			node.setReal(false);
			node.setChildern(new ArrayList<TreeNode<T>>());
			node.getChildern().add(n1);

			if (numberOfMatchingCharacters < key.length()) {
				TreeNode<T> n2 = new TreeNode<T>();
				n2.setKey(key.substring(numberOfMatchingCharacters, key.length()));
				n2.setReal(true);
				n2.setValue(value);

				node.getChildern().add(n2);
			} else {
				node.setValue(value);
				node.setReal(true);
			}
		}
		// this key need to be added as the child of the current node
		else {
			TreeNode<T> n = new TreeNode<T>();
			n.setKey(node.getKey().substring(numberOfMatchingCharacters, node.getKey().length()));
			n.setChildern(node.getChildern());
			n.setReal(node.isReal());
			n.setValue(node.getValue());

			node.setKey(key);
			node.setReal(true);
			node.setValue(value);
			node.getChildern().add(n);
		}
	}

	public List<T> searchByPrefix(String prefix, int limit) {
		List<T> keys = new ArrayList<T>();
		TreeNode<T> node = searchByPrefix(prefix, root);

		if (node != null) {
			if (node.isReal()) {
				keys.add(node.getValue());
			}
			getNodes(node, keys, limit);
		}

		return keys;
	}

	private void getNodes(TreeNode<T> parent, List<T> keys, int limit) {
		Queue<TreeNode<T>> queue = new LinkedList<>();
		queue.addAll(parent.getChildern());

		while (!queue.isEmpty()) {
			TreeNode<T> node = queue.remove();
			if (node.isReal() == true) {
				keys.add(node.getValue());
			}

			if (keys.size() == limit) {
				break;
			}

			queue.addAll(node.getChildern());
		}
	}

	private TreeNode<T> searchByPrefix(String prefix, TreeNode<T> currentNode) {
		TreeNode<T> result = null;

		int numberOfMatchingCharacters = currentNode.getNumberOfMatchingCharacters(prefix);

		if (numberOfMatchingCharacters == prefix.length() && numberOfMatchingCharacters <= currentNode.getKey().length()) {
			result = currentNode;
		} else if (currentNode.getKey().equals("") == true
				|| (numberOfMatchingCharacters < prefix.length() && numberOfMatchingCharacters >= currentNode.getKey().length())) {
			String newText = prefix.substring(numberOfMatchingCharacters, prefix.length());
			for (TreeNode<T> child : currentNode.getChildern()) {
				if (child.getKey().startsWith(newText.charAt(0) + "")) {
					result = searchByPrefix(newText, child);
					break;
				}
			}
		}

		return result;
	}

	public boolean contains(String key) {
		TreeVisitor<T, Boolean> visitor = new TreeVisitor<T, Boolean>(Boolean.FALSE) {
			@Override
			public void visit(String key, TreeNode<T> parent, TreeNode<T> node) {
				result = node.isReal();
			}
		};

		visit(key, visitor);

		return visitor.getResult().booleanValue();
	}

	private <R> void visit(String key, TreeVisitor<T, R> visitor) {
		if (root != null) {
			visit(key, visitor, null, root);
		}
	}

	/**
	 * recursively visit the tree based on the supplied "key". calls the Visitor for
	 * the node those key matches the given prefix
	 */
	private <R> void visit(String prefix, TreeVisitor<T, R> visitor, TreeNode<T> parent, TreeNode<T> node) {

		int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(prefix);

		// if the node key and prefix match, we found a match!
		if (numberOfMatchingCharacters == prefix.length() && numberOfMatchingCharacters == node.getKey().length()) {
			visitor.visit(prefix, parent, node);
		} else if (node.getKey().equals("") == true // either we are at the
				// root
				|| (numberOfMatchingCharacters < prefix.length() && numberOfMatchingCharacters >= node.getKey().length())) { // OR we need to
			// traverse the childern
			String newText = prefix.substring(numberOfMatchingCharacters, prefix.length());
			for (TreeNode<T> child : node.getChildern()) {
				// recursively search the child nodes
				if (child.getKey().startsWith(newText.charAt(0) + "")) {
					visit(newText, visitor, node, child);
					break;
				}
			}
		}
	}

	public long getSize() {
		return size;
	}

}
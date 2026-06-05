package io.thingshub.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>
 * 树结构处理工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class TreeUtils {

	/**
	 * 将list合成树
	 *
	 * @param list           需要合成树的List
	 * @param rootCheck      判断E中为根节点的条件，如：x->x.getPId()==-1L ,
	 *                       x->x.getParentId()==null,x->x.getParentMenuId()==0
	 * @param parentCheck    判断E中为父节点条件，如：(x,y)->x.getId().equals(y.getPId())
	 * @param setSubChildren E中设置下级数据方法，如：Menu::setSubMenus
	 * @param <E>            泛型实体对象
	 * @return 合成好的树
	 */
	public static <E> List<E> makeTree(List<E> list, Predicate<E> rootCheck, BiFunction<E, E, Boolean> parentCheck, BiConsumer<E, List<E>> setSubChildren) {
		return list.stream().filter(rootCheck).peek(x -> setSubChildren.accept(x, makeChildren(x, list, parentCheck, setSubChildren))).toList();
	}

	/**
	 * 将树打平成tree
	 * 
	 * @param tree           需要打平的树
	 * @param getSubChildren 设置下级数据方法，如：Menu::getSubMenus,x->x.setSubMenus(null)
	 * @param setSubChildren 将下级数据置空方法，如：x->x.setSubMenus(null)
	 * @return 打平后的数据
	 * @param <E> 泛型实体对象
	 */
	public static <E> List<E> flat(List<E> tree, Function<E, List<E>> getSubChildren, Consumer<E> setSubChildren) {
		List<E> res = new ArrayList<>();
		forPostOrder(tree, item -> {
			setSubChildren.accept(item);
			res.add(item);
		}, getSubChildren);
		return res;
	}

	/**
	 * 前序遍历
	 *
	 * @param tree           需要遍历的树
	 * @param consumer       遍历后对单个元素的处理方法，如：x-> System.out.println(x)、
	 *                       System.out::println打印元素
	 * @param setSubChildren 设置下级数据方法，如：Menu::getSubMenus,x->x.setSubMenus(null)
	 * @param <E>            泛型实体对象
	 */
	public static <E> void forPreOrder(List<E> tree, Consumer<E> consumer, Function<E, List<E>> setSubChildren) {
		for (E l : tree) {
			consumer.accept(l);
			List<E> es = setSubChildren.apply(l);
			if (es != null && es.size() > 0) {
				forPreOrder(es, consumer, setSubChildren);
			}
		}
	}

	/**
	 * 层序遍历
	 *
	 * @param tree           需要遍历的树
	 * @param consumer       遍历后对单个元素的处理方法，如：x-> System.out.println(x)、
	 *                       System.out::println打印元素
	 * @param setSubChildren 设置下级数据方法，如：Menu::getSubMenus,x->x.setSubMenus(null)
	 * @param <E>            泛型实体对象
	 */
	public static <E> void forLevelOrder(List<E> tree, Consumer<E> consumer, Function<E, List<E>> setSubChildren) {
		Queue<E> queue = new LinkedList<>(tree);
		while (!queue.isEmpty()) {
			E item = queue.poll();
			consumer.accept(item);
			List<E> childList = setSubChildren.apply(item);
			if (childList != null && !childList.isEmpty()) {
				queue.addAll(childList);
			}
		}
	}

	/**
	 * 后序遍历
	 *
	 * @param tree           需要遍历的树
	 * @param consumer       遍历后对单个元素的处理方法，如：x-> System.out.println(x)、
	 *                       System.out::println打印元素
	 * @param setSubChildren 设置下级数据方法，如：Menu::getSubMenus,x->x.setSubMenus(null)
	 * @param <E>            泛型实体对象
	 */
	public static <E> void forPostOrder(List<E> tree, Consumer<E> consumer, Function<E, List<E>> setSubChildren) {
		for (E item : tree) {
			List<E> childList = setSubChildren.apply(item);
			if (childList != null && !childList.isEmpty()) {
				forPostOrder(childList, consumer, setSubChildren);
			}
			consumer.accept(item);
		}
	}

	/**
	 * 对树所有子节点按comparator排序
	 *
	 * @param tree        需要排序的树
	 * @param comparator  排序规则Comparator，如：Comparator.comparing(MenuVo::getRank)按Rank正序
	 *                    ,(x,y)->y.getRank().compareTo(x.getRank())，按Rank倒序
	 * @param getChildren 获取下级数据方法，如：MenuVo::getSubMenus
	 * @return 排序好的树
	 * @param <E> 泛型实体对象
	 */
	public static <E> List<E> sort(List<E> tree, Comparator<? super E> comparator, Function<E, List<E>> getChildren) {
		for (E item : tree) {
			List<E> childList = getChildren.apply(item);
			if (childList != null && !childList.isEmpty()) {
				sort(childList, comparator, getChildren);
			}
		}
		tree.sort(comparator);
		return tree;
	}

	private static <E> List<E> makeChildren(E parent, List<E> allData, BiFunction<E, E, Boolean> parentCheck, BiConsumer<E, List<E>> children) {
		return allData.stream().filter(x -> parentCheck.apply(parent, x)).peek(x -> children.accept(x, makeChildren(x, allData, parentCheck, children))).toList();
	}

}
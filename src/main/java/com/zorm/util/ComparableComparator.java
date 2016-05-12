package com.zorm.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Delegates to Comparable
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ComparableComparator implements Comparator<Comparable>, Serializable {
	public static final Comparator INSTANCE = new ComparableComparator();

	@SuppressWarnings({ "unchecked" })
	public int compare(Comparable one, Comparable another) {
		return one.compareTo( another );
	}
}
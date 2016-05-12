package com.zorm.util;

public final class EqualsHelper {

	public static boolean equals(final Object x, final Object y) {
		return x == y || ( x != null && y != null && x.equals( y ) );
	}
	
	private EqualsHelper() {}

}
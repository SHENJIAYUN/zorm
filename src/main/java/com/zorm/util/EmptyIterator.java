package com.zorm.util;

import java.util.Iterator;

public final class EmptyIterator implements Iterator{
	public static final Iterator INSTANCE = new EmptyIterator();

	public boolean hasNext() {
		return false;
	}

	public Object next() {
		throw new UnsupportedOperationException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private EmptyIterator() {}
}

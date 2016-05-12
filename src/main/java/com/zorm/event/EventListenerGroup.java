package com.zorm.event;

import java.io.Serializable;

public interface EventListenerGroup<T> extends Serializable{
	public Iterable<T> listeners();

	public void appendListener(T listener);

	public boolean isEmpty();

	public int count();
}

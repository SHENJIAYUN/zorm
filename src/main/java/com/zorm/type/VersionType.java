package com.zorm.type;

import java.util.Comparator;

import com.zorm.session.SessionImplementor;

public interface VersionType<T> extends Type{
	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 */
	public T seed(SessionImplementor session);
	
	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 */
	public T next(T current, SessionImplementor session);
	
	/**
	 * Get a comparator for version values.
	 *
	 * @return The comparator to use to compare different version values.
	 */
	public Comparator<T> getComparator();
}

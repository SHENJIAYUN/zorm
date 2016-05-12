package com.zorm.annotations;

public enum OptimisticLockType {
	/**
	 * no optimistic locking
	 */
	NONE,
	/**
	 * use a column version
	 */
	VERSION,
	/**
	 * dirty columns are compared
	 */
	DIRTY,
	/**
	 * all columns are compared
	 */
	ALL
}

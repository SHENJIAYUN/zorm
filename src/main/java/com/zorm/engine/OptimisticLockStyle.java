package com.zorm.engine;

public enum OptimisticLockStyle {
	/**
	 * no optimistic locking
	 */
	NONE,
	/**
	 * use a dedicated version column
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

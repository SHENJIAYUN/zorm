package com.zorm.engine.transaction.spi;

public enum LocalStatus {
	/**
	 * The local transaction has not yet been begun
	 */
	NOT_ACTIVE,
	/**
	 * The local transaction has been begun, but not yet completed.
	 */
	ACTIVE,
	/**
	 * The local transaction has been competed successfully.
	 */
	COMMITTED,
	/**
	 * The local transaction has been rolled back.
	 */
	ROLLED_BACK,
	/**
	 * The local transaction attempted to commit, but failed.
	 */
	FAILED_COMMIT
}

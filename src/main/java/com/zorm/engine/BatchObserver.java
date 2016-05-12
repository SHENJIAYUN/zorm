package com.zorm.engine;

public interface BatchObserver {
	/**
	 * Indicates explicit execution of the batch via a call to {@link Batch#execute()}.
	 */
	public void batchExplicitlyExecuted();

	/**
	 * Indicates an implicit execution of the batch.
	 */
	public void batchImplicitlyExecuted();
}

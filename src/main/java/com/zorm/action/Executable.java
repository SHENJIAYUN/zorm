package com.zorm.action;

import java.io.Serializable;

import com.zorm.exception.ZormException;

public interface Executable {
	/**
	 * What spaces (tables) are affected by this action?
	 *
	 * @return The spaces affected by this action.
	 */
	public Serializable[] getPropertySpaces();

	/**
	 * Called before executing any actions.  Gives actions a chance to perform any preparation.
	 *
	 * @throws HibernateException Indicates a problem during preparation.
	 */
	public void beforeExecutions() throws ZormException;

	/**
	 * Execute this action
	 *
	 * @throws HibernateException Indicates a problem during execution.
	 */
	public void execute() throws ZormException;

	/**
	 * Get the after-transaction-completion process, if any, for this action.
	 *
	 * @return The after-transaction-completion process, or null if we have no
	 * after-transaction-completion process
	 */
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess();

	/**
	 * Get the before-transaction-completion process, if any, for this action.
	 *
	 * @return The before-transaction-completion process, or null if we have no
	 * before-transaction-completion process
	 */
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess();
}

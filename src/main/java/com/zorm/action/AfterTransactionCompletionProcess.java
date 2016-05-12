package com.zorm.action;

import com.zorm.session.SessionImplementor;

public interface AfterTransactionCompletionProcess {
	/**
	 * Perform whatever processing is encapsulated here after completion of the transaction.
	 *
	 * @param success Did the transaction complete successfully?  True means it did.
	 * @param session The session on which the transaction is completing.
	 */
	public void doAfterTransactionCompletion(boolean success, SessionImplementor session);
}

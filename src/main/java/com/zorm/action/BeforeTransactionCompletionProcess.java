package com.zorm.action;

import com.zorm.session.SessionImplementor;

public interface BeforeTransactionCompletionProcess {
	public void doBeforeTransactionCompletion(SessionImplementor session);
}

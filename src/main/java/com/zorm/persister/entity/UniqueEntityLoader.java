package com.zorm.persister.entity;

import java.io.Serializable;

import com.zorm.LockOptions;
import com.zorm.session.SessionImplementor;

public interface UniqueEntityLoader {
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions);
}

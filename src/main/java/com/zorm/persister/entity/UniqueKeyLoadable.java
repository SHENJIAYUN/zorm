package com.zorm.persister.entity;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;

public interface UniqueKeyLoadable extends Loadable{
	public int getPropertyIndex(String propertyName);

	public Object loadByUniqueKey(String uniqueKeyPropertyName, Object key,SessionImplementor session) throws ZormException;
}

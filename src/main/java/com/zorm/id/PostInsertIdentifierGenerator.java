package com.zorm.id;

import com.zorm.dialect.Dialect;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.PostInsertIdentityPersister;

public interface PostInsertIdentifierGenerator extends IdentifierGenerator{
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
	        Dialect dialect,
	        boolean isGetGeneratedKeysEnabled) throws ZormException;
}

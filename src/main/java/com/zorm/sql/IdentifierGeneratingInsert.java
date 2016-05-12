package com.zorm.sql;

import com.zorm.dialect.Dialect;

public class IdentifierGeneratingInsert extends Insert{
	public IdentifierGeneratingInsert(Dialect dialect) {
		super( dialect );
	}
}

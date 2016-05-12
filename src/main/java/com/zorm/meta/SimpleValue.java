package com.zorm.meta;

import com.zorm.dialect.Dialect;

public interface SimpleValue extends Value{
	public String getAlias(Dialect dialect);
}

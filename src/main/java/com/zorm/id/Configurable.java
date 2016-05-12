package com.zorm.id;

import java.util.Properties;

import com.zorm.dialect.Dialect;
import com.zorm.exception.MappingException;
import com.zorm.type.Type;

public interface Configurable {
	public void configure(Type type, Properties params, Dialect d) throws MappingException;
}

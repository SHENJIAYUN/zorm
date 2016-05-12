package com.zorm.dialect;

import java.sql.Connection;
import java.util.Map;

import com.zorm.exception.ZormException;
import com.zorm.service.Service;

public interface DialectFactory extends Service{
	public Dialect buildDialect(Map configValues, Connection connection) throws ZormException;
}

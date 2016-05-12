package com.zorm.jdbc;

import java.sql.Connection;

public interface SchemaNameResolver {
	public String resolveSchemaName(Connection connection);
}

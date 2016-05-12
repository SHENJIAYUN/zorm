package com.zorm.mapping;

import com.zorm.dialect.Dialect;
import com.zorm.engine.Mapping;
import com.zorm.exception.ZormException;

public interface RelationalModel {
	//获取创建和删除表的DDL语句
	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) throws ZormException;
	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema);
}

package com.zorm.mapping;

import com.zorm.dialect.Dialect;
import com.zorm.dialect.function.SQLFunctionRegistry;

public interface Selectable {
	public String getAlias(Dialect dialect);
	public String getAlias(Dialect dialect, Table table);
	public boolean isFormula();
	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry);
	public String getText(Dialect dialect);
	public String getText();
}

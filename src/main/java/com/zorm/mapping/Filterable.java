package com.zorm.mapping;

public interface Filterable {
	public void addFilter(String name, String condition, boolean autoAliasInjection, java.util.Map<String,String> aliasTableMap, java.util.Map<String,String> aliasEntityMap);

	public java.util.List getFilters();
}

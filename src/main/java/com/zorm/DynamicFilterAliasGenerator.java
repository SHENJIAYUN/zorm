package com.zorm;

import com.zorm.persister.entity.AbstractEntityPersister;

public class DynamicFilterAliasGenerator implements FilterAliasGenerator {
	
	private String[] tables;
	private String rootAlias;

	public DynamicFilterAliasGenerator(String[] tables, String rootAlias) {
		this.tables = tables;
		this.rootAlias = rootAlias;
	}

	@Override
	public String getAlias(String table) {
		if (table == null){
			return rootAlias;
		} else{
			return AbstractEntityPersister.generateTableAlias(rootAlias, AbstractEntityPersister.getTableId(table, tables));
		}
	}

}


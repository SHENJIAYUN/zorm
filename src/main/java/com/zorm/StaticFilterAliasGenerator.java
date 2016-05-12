package com.zorm;

public class StaticFilterAliasGenerator implements FilterAliasGenerator{
	
	private final String alias;

	public StaticFilterAliasGenerator(String alias) {
		this.alias = alias;
	}

	@Override
	public String getAlias(String table) {
		return alias;
	}

}
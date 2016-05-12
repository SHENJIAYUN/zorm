package com.zorm.entity;

import com.zorm.persister.entity.Loadable;

public interface EntityAliases {

	public String getRowIdAlias();

	public String[][] getSuffixedPropertyAliases();
	
	public String[][] getSuffixedPropertyAliases(Loadable persister);

	public String[] getSuffixedKeyAliases();

	public String getSuffixedDiscriminatorAlias();

}

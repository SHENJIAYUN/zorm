package com.zorm.persister.entity;

import java.util.Map;

import com.zorm.FetchMode;
import com.zorm.engine.CascadeStyle;
import com.zorm.type.AssociationType;
import com.zorm.type.Type;

public interface OuterJoinLoadable extends Loadable,Joinable{

	public String[] toColumns(String columnQualifier, int propertyIndex);

	public String[] getSubclassPropertyColumnNames(int property);

	public String[] getPropertyColumnNames(String propertyName);

	public String getPropertyTableName(String propertyName);

	public String getSubclassPropertyTableName(int propertyIndex);

	public AssociationType getEntityType();

	public String selectFragment(String alias, String string);

	public String fromTableFragment(String alias);

	public int countSubclassProperties();

	public Type getSubclassPropertyType(int i);

	public boolean isSubclassPropertyNullable(int i);

	public String getSubclassPropertyName(int i);

	public FetchMode getFetchMode(int i);

	public CascadeStyle getCascadeStyle(int i);

}

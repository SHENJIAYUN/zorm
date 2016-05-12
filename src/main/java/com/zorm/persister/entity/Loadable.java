package com.zorm.persister.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public interface Loadable extends EntityPersister {
	
	public static final String ROWID_ALIAS = "rowid_";
	
	public Type getDiscriminatorType();

	public String[] getIdentifierAliases(String suffix);

	public String getDiscriminatorAlias(String suffix);

	public String[] getPropertyAliases(String suffix, int j);

	public boolean hasSubclasses();
	
	public Object[] hydrate(
			ResultSet rs,
			Serializable id,
			Object object,
			Loadable rootLoadable,
			String[][] suffixedPropertyColumns,
			boolean allProperties, 
			SessionImplementor session)
	throws SQLException;

	public boolean hasRowId();

	public boolean isAbstract();

	public String getSubclassForDiscriminatorValue(Object value);

	public String[] getIdentifierColumnNames();
}

package com.zorm.persister;

import com.zorm.FetchMode;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Joinable;
import com.zorm.persister.entity.PropertyMapping;

public interface QueryableCollection extends PropertyMapping, Joinable, CollectionPersister{

	public abstract EntityPersister getElementPersister();

	public abstract String[] getElementColumnNames(String alias);

	public abstract String[] getElementColumnNames();

	public abstract FetchMode getFetchMode();

	public abstract String getManyToManyOrderByString(String alias);

	public abstract String selectFragment(String alias, String columnSuffix);

	public abstract String getSQLOrderByString(String alias);

}

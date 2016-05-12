package com.zorm.persister;

import java.io.Serializable;

import com.zorm.loader.CollectionInitializer;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class NamedQueryCollectionInitializer implements CollectionInitializer {

	private final String queryName;
	private final CollectionPersister persister;

	public NamedQueryCollectionInitializer(String queryName, CollectionPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}
	
	@Override
	public void initialize(Serializable id, SessionImplementor session) {

	}

}

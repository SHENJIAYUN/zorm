package com.zorm.loader;

import java.io.Serializable;

import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.OuterJoinLoader;
import com.zorm.persister.QueryableCollection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class CollectionLoader extends OuterJoinLoader implements CollectionInitializer {
	private final QueryableCollection collectionPersister;
	
	public CollectionLoader(
			QueryableCollection collectionPersister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( factory, loadQueryInfluencers );
		this.collectionPersister = collectionPersister;
	}
	
	protected Type getKeyType() {
		return collectionPersister.getKeyType();
	}

	public String toString() {
		return getClass().getName() + '(' + collectionPersister.getRole() + ')';
	}

	@Override
	public void initialize(Serializable id, SessionImplementor session) {
	}
}

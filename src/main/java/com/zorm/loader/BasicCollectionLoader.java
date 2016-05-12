package com.zorm.loader;

import java.io.Serializable;

import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.JoinWalker;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public class BasicCollectionLoader extends CollectionLoader {
	public BasicCollectionLoader(
			QueryableCollection collectionPersister,
			SessionFactoryImplementor session,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( collectionPersister, 1, session, loadQueryInfluencers );
	}

	public BasicCollectionLoader(
			QueryableCollection collectionPersister,
			int batchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( collectionPersister, batchSize, null, factory, loadQueryInfluencers );
	}

	protected BasicCollectionLoader(
			QueryableCollection collectionPersister,
			int batchSize,
			String subquery,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( collectionPersister, factory, loadQueryInfluencers );

		JoinWalker walker = new BasicCollectionJoinWalker(
				collectionPersister,
				batchSize,
				subquery,
				factory,
				loadQueryInfluencers
		);
		initFromWalker( walker );

		postInstantiate();
	}

}

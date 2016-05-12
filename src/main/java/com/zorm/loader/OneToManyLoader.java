package com.zorm.loader;

import java.io.Serializable;

import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.JoinWalker;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public class OneToManyLoader extends CollectionLoader {
	public OneToManyLoader(
			QueryableCollection oneToManyPersister,
			SessionFactoryImplementor session,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( oneToManyPersister, 1, session, loadQueryInfluencers );
	}
	
	public OneToManyLoader(
			QueryableCollection oneToManyPersister,
			int batchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( oneToManyPersister, batchSize, null, factory, loadQueryInfluencers );
	}
	
	public OneToManyLoader(
			QueryableCollection oneToManyPersister,
			int batchSize,
			String subquery,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( oneToManyPersister, factory, loadQueryInfluencers );

		JoinWalker walker = new OneToManyJoinWalker(
				oneToManyPersister,
				batchSize,
				subquery,
				factory,
				loadQueryInfluencers
		);
		initFromWalker( walker );

		postInstantiate();
	}

}

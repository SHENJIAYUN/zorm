package com.zorm.entity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.MappingException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.OuterJoinLoadable;
import com.zorm.persister.entity.UniqueEntityLoader;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class BatchingEntityLoader implements UniqueEntityLoader {

	private final Loader[] loaders;
	private final int[] batchSizes;
	private final EntityPersister persister;
	private final Type idType;

	public BatchingEntityLoader(EntityPersister persister, int[] batchSizes, Loader[] loaders) {
		this.batchSizes = batchSizes;
		this.loaders = loaders;
		this.persister = persister;
		idType = persister.getIdentifierType();
	}

	private Object getObjectFromList(List results, Serializable id, SessionImplementor session) {
		// get the right object from the list ... would it be easier to just call getEntity() ??
		Iterator iter = results.iterator();
		while ( iter.hasNext() ) {
			Object obj = iter.next();
			final boolean equal = idType.isEqual(
					id,
					session.getContextEntityIdentifier(obj),
					session.getFactory()
			);
			if ( equal ) return obj;
		}
		return null;
	}

	public static UniqueEntityLoader createBatchingEntityLoader(
		final OuterJoinLoadable persister,
		final int maxBatchSize,
		final LockMode lockMode,
		final SessionFactoryImplementor factory,
		final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new EntityLoader(persister, batchSizesToCreate[i], lockMode, factory, loadQueryInfluencers);
			}
			return new BatchingEntityLoader(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new EntityLoader(persister, lockMode, factory, loadQueryInfluencers);
		}
	}

	public static UniqueEntityLoader createBatchingEntityLoader(
		final OuterJoinLoadable persister,
		final int maxBatchSize,
		final LockOptions lockOptions,
		final SessionFactoryImplementor factory,
		final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new EntityLoader(persister, batchSizesToCreate[i], lockOptions, factory, loadQueryInfluencers);
			}
			return new BatchingEntityLoader(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new EntityLoader(persister, lockOptions, factory, loadQueryInfluencers);
		}
	}

	@Override
	public Object load(Serializable id, Object optionalObject,
			SessionImplementor session, LockOptions lockOptions) {
		// TODO Auto-generated method stub
		return null;
	}

}


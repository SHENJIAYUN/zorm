package com.zorm.loader;

import java.io.Serializable;

import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.Loader;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.ArrayHelper;

public class BatchingCollectionInitializer implements CollectionInitializer{
	private final Loader[] loaders;
	private final int[] batchSizes;
	private final CollectionPersister collectionPersister;
	
	public BatchingCollectionInitializer(CollectionPersister collPersister, int[] batchSizes, Loader[] loaders) {
		this.loaders = loaders;
		this.batchSizes = batchSizes;
		this.collectionPersister = collPersister;
	}

	public CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	public Loader[] getLoaders() {
		return loaders;
	}

	public int[] getBatchSizes() {
		return batchSizes;
	}

	@Override
	public void initialize(Serializable id, SessionImplementor session) {
		
	}

	public static CollectionInitializer createBatchingCollectionInitializer(
			final QueryableCollection persister,
			final int maxBatchSize,
			final SessionFactoryImplementor factory,
			final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		if ( maxBatchSize > 1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes( maxBatchSize );
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new BasicCollectionLoader( persister, batchSizesToCreate[i], factory, loadQueryInfluencers );
			}
			return new BatchingCollectionInitializer(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new BasicCollectionLoader( persister, factory, loadQueryInfluencers );
		}
	}

	public static CollectionInitializer createBatchingOneToManyInitializer(
			final QueryableCollection persister,
			final int maxBatchSize,
			final SessionFactoryImplementor factory,
			final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		if ( maxBatchSize > 1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new OneToManyLoader( persister, batchSizesToCreate[i], factory, loadQueryInfluencers );
			}
			return new BatchingCollectionInitializer( persister, batchSizesToCreate, loadersToCreate );
		}
		else {
			return new OneToManyLoader( persister, factory, loadQueryInfluencers );
		}
	}
}

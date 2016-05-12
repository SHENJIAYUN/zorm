package com.zorm.stat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.zorm.service.Service;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.ArrayHelper;

public class ConcurrentStatisticsImpl implements StatisticsImplementor, Service{
	private SessionFactoryImplementor sessionFactory;

	private volatile boolean isStatisticsEnabled;
	private volatile long startTime;
	private AtomicLong sessionOpenCount = new AtomicLong();
	private AtomicLong sessionCloseCount = new AtomicLong();
	private AtomicLong flushCount = new AtomicLong();
	private AtomicLong connectCount = new AtomicLong();

	private AtomicLong prepareStatementCount = new AtomicLong();
	private AtomicLong closeStatementCount = new AtomicLong();

	private AtomicLong entityLoadCount = new AtomicLong();
	private AtomicLong entityUpdateCount = new AtomicLong();
	private AtomicLong entityInsertCount = new AtomicLong();
	private AtomicLong entityDeleteCount = new AtomicLong();
	private AtomicLong entityFetchCount = new AtomicLong();
	private AtomicLong collectionLoadCount = new AtomicLong();
	private AtomicLong collectionUpdateCount = new AtomicLong();
	private AtomicLong collectionRemoveCount = new AtomicLong();
	private AtomicLong collectionRecreateCount = new AtomicLong();
	private AtomicLong collectionFetchCount = new AtomicLong();

	private AtomicLong secondLevelCacheHitCount = new AtomicLong();
	private AtomicLong secondLevelCacheMissCount = new AtomicLong();
	private AtomicLong secondLevelCachePutCount = new AtomicLong();
	
	private AtomicLong naturalIdCacheHitCount = new AtomicLong();
	private AtomicLong naturalIdCacheMissCount = new AtomicLong();
	private AtomicLong naturalIdCachePutCount = new AtomicLong();
	private AtomicLong naturalIdQueryExecutionCount = new AtomicLong();
	private AtomicLong naturalIdQueryExecutionMaxTime = new AtomicLong();
	private volatile String naturalIdQueryExecutionMaxTimeRegion;
	
	private AtomicLong queryExecutionCount = new AtomicLong();
	private AtomicLong queryExecutionMaxTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;
	private AtomicLong queryCacheHitCount = new AtomicLong();
	private AtomicLong queryCacheMissCount = new AtomicLong();
	private AtomicLong queryCachePutCount = new AtomicLong();

	private AtomicLong updateTimestampsCacheHitCount = new AtomicLong();
	private AtomicLong updateTimestampsCacheMissCount = new AtomicLong();
	private AtomicLong updateTimestampsCachePutCount = new AtomicLong();

	private AtomicLong committedTransactionCount = new AtomicLong();
	private AtomicLong transactionCount = new AtomicLong();

	private AtomicLong optimisticFailureCount = new AtomicLong();

	/**
	 * natural id cache statistics per region
	 */
	private final ConcurrentMap naturalIdCacheStatistics = new ConcurrentHashMap();
	/**
	 * second level cache statistics per region
	 */
	private final ConcurrentMap secondLevelCacheStatistics = new ConcurrentHashMap();
	/**
	 * entity statistics per name
	 */
	private final ConcurrentMap entityStatistics = new ConcurrentHashMap();
	/**
	 * collection statistics per name
	 */
	private final ConcurrentMap collectionStatistics = new ConcurrentHashMap();
	/**
	 * entity statistics per query string (HQL or SQL)
	 */
	private final ConcurrentMap queryStatistics = new ConcurrentHashMap();

	@SuppressWarnings({ "UnusedDeclaration" })
	public ConcurrentStatisticsImpl() {
		clear();
	}

	public ConcurrentStatisticsImpl(SessionFactoryImplementor sessionFactory) {
		clear();
		this.sessionFactory = sessionFactory;
	}

	/**
	 * reset all statistics
	 */
	public void clear() {
		secondLevelCacheHitCount.set( 0 );
		secondLevelCacheMissCount.set( 0 );
		secondLevelCachePutCount.set( 0 );
		
		naturalIdCacheHitCount.set( 0 );
		naturalIdCacheMissCount.set( 0 );
		naturalIdCachePutCount.set( 0 );
		naturalIdQueryExecutionCount.set( 0 );
		naturalIdQueryExecutionMaxTime.set( 0 );
		naturalIdQueryExecutionMaxTimeRegion = null;

		sessionCloseCount.set( 0 );
		sessionOpenCount.set( 0 );
		flushCount.set( 0 );
		connectCount.set( 0 );

		prepareStatementCount.set( 0 );
		closeStatementCount.set( 0 );

		entityDeleteCount.set( 0 );
		entityInsertCount.set( 0 );
		entityUpdateCount.set( 0 );
		entityLoadCount.set( 0 );
		entityFetchCount.set( 0 );

		collectionRemoveCount.set( 0 );
		collectionUpdateCount.set( 0 );
		collectionRecreateCount.set( 0 );
		collectionLoadCount.set( 0 );
		collectionFetchCount.set( 0 );

		queryExecutionCount.set( 0 );
		queryCacheHitCount.set( 0 );
		queryExecutionMaxTime.set( 0 );
		queryExecutionMaxTimeQueryString = null;
		queryCacheMissCount.set( 0 );
		queryCachePutCount.set( 0 );

		updateTimestampsCacheMissCount.set( 0 );
		updateTimestampsCacheHitCount.set( 0 );
		updateTimestampsCachePutCount.set( 0 );

		transactionCount.set( 0 );
		committedTransactionCount.set( 0 );

		optimisticFailureCount.set( 0 );

		secondLevelCacheStatistics.clear();
		entityStatistics.clear();
		collectionStatistics.clear();
		queryStatistics.clear();

		startTime = System.currentTimeMillis();
	}

	public void openSession() {
		sessionOpenCount.getAndIncrement();
	}

	public void closeSession() {
		sessionCloseCount.getAndIncrement();
	}

	public void flush() {
		flushCount.getAndIncrement();
	}

	public void connect() {
		connectCount.getAndIncrement();
	}


	/**
	 * @return entity deletion count
	 */
	public long getEntityDeleteCount() {
		return entityDeleteCount.get();
	}

	/**
	 * @return entity insertion count
	 */
	public long getEntityInsertCount() {
		return entityInsertCount.get();
	}

	/**
	 * @return entity load (from DB)
	 */
	public long getEntityLoadCount() {
		return entityLoadCount.get();
	}

	/**
	 * @return entity fetch (from DB)
	 */
	public long getEntityFetchCount() {
		return entityFetchCount.get();
	}

	/**
	 * @return entity update
	 */
	public long getEntityUpdateCount() {
		return entityUpdateCount.get();
	}

	public long getQueryExecutionCount() {
		return queryExecutionCount.get();
	}

	public long getQueryCacheHitCount() {
		return queryCacheHitCount.get();
	}

	public long getQueryCacheMissCount() {
		return queryCacheMissCount.get();
	}

	public long getQueryCachePutCount() {
		return queryCachePutCount.get();
	}

	public long getUpdateTimestampsCacheHitCount() {
		return updateTimestampsCacheHitCount.get();
	}

	public long getUpdateTimestampsCacheMissCount() {
		return updateTimestampsCacheMissCount.get();
	}

	public long getUpdateTimestampsCachePutCount() {
		return updateTimestampsCachePutCount.get();
	}

	/**
	 * @return flush
	 */
	public long getFlushCount() {
		return flushCount.get();
	}

	/**
	 * @return session connect
	 */
	public long getConnectCount() {
		return connectCount.get();
	}

	/**
	 * @return second level cache hit
	 */
	public long getSecondLevelCacheHitCount() {
		return secondLevelCacheHitCount.get();
	}

	/**
	 * @return second level cache miss
	 */
	public long getSecondLevelCacheMissCount() {
		return secondLevelCacheMissCount.get();
	}

	/**
	 * @return second level cache put
	 */
	public long getSecondLevelCachePutCount() {
		return secondLevelCachePutCount.get();
	}

	/**
	 * @return session closing
	 */
	public long getSessionCloseCount() {
		return sessionCloseCount.get();
	}

	/**
	 * @return session opening
	 */
	public long getSessionOpenCount() {
		return sessionOpenCount.get();
	}

	/**
	 * @return collection loading (from DB)
	 */
	public long getCollectionLoadCount() {
		return collectionLoadCount.get();
	}

	/**
	 * @return collection fetching (from DB)
	 */
	public long getCollectionFetchCount() {
		return collectionFetchCount.get();
	}

	/**
	 * @return collection update
	 */
	public long getCollectionUpdateCount() {
		return collectionUpdateCount.get();
	}

	/**
	 * @return collection removal
	 *         FIXME: even if isInverse="true"?
	 */
	public long getCollectionRemoveCount() {
		return collectionRemoveCount.get();
	}

	/**
	 * @return collection recreation
	 */
	public long getCollectionRecreateCount() {
		return collectionRecreateCount.get();
	}

	/**
	 * @return start time in ms (JVM standards {@link System#currentTimeMillis()})
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Are statistics logged
	 */
	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	public void setStatisticsEnabled(boolean b) {
		isStatisticsEnabled = b;
	}

	/**
	 * @return Returns the max query execution time,
	 *         for all queries
	 */
	public long getQueryExecutionMaxTime() {
		return queryExecutionMaxTime.get();
	}

	/**
	 * Get all executed query strings
	 */
	public String[] getQueries() {
		return ArrayHelper.toStringArray( queryStatistics.keySet() );
	}

	/**
	 * Get the names of all entities
	 */
	public String[] getEntityNames() {
		if ( sessionFactory == null ) {
			return ArrayHelper.toStringArray( entityStatistics.keySet() );
		}
		else {
			return ArrayHelper.toStringArray( sessionFactory.getAllClassMetadata().keySet() );
		}
	}


	public void endTransaction(boolean success) {
		transactionCount.getAndIncrement();
		if ( success ) {
			committedTransactionCount.getAndIncrement();
		}
	}

	public long getSuccessfulTransactionCount() {
		return committedTransactionCount.get();
	}

	public long getTransactionCount() {
		return transactionCount.get();
	}

	public void closeStatement() {
		closeStatementCount.getAndIncrement();
	}

	public void prepareStatement() {
		prepareStatementCount.getAndIncrement();
	}

	public long getCloseStatementCount() {
		return closeStatementCount.get();
	}

	public long getPrepareStatementCount() {
		return prepareStatementCount.get();
	}

	public long getOptimisticFailureCount() {
		return optimisticFailureCount.get();
	}

	@Override
    public String toString() {
		return new StringBuilder()
				.append( "Statistics[" )
				.append( "start time=" ).append( startTime )
				.append( ",sessions opened=" ).append( sessionOpenCount )
				.append( ",sessions closed=" ).append( sessionCloseCount )
				.append( ",transactions=" ).append( transactionCount )
				.append( ",successful transactions=" ).append( committedTransactionCount )
				.append( ",optimistic lock failures=" ).append( optimisticFailureCount )
				.append( ",flushes=" ).append( flushCount )
				.append( ",connections obtained=" ).append( connectCount )
				.append( ",statements prepared=" ).append( prepareStatementCount )
				.append( ",statements closed=" ).append( closeStatementCount )
				.append( ",second level cache puts=" ).append( secondLevelCachePutCount )
				.append( ",second level cache hits=" ).append( secondLevelCacheHitCount )
				.append( ",second level cache misses=" ).append( secondLevelCacheMissCount )
				.append( ",entities loaded=" ).append( entityLoadCount )
				.append( ",entities updated=" ).append( entityUpdateCount )
				.append( ",entities inserted=" ).append( entityInsertCount )
				.append( ",entities deleted=" ).append( entityDeleteCount )
				.append( ",entities fetched=" ).append( entityFetchCount )
				.append( ",collections loaded=" ).append( collectionLoadCount )
				.append( ",collections updated=" ).append( collectionUpdateCount )
				.append( ",collections removed=" ).append( collectionRemoveCount )
				.append( ",collections recreated=" ).append( collectionRecreateCount )
				.append( ",collections fetched=" ).append( collectionFetchCount )
				.append( ",naturalId queries executed to database=" ).append( naturalIdQueryExecutionCount )
				.append( ",naturalId cache puts=" ).append( naturalIdCachePutCount )
				.append( ",naturalId cache hits=" ).append( naturalIdCacheHitCount )
				.append( ",naturalId cache misses=" ).append( naturalIdCacheMissCount )
				.append( ",naturalId max query time=" ).append( naturalIdQueryExecutionMaxTime )
				.append( ",queries executed to database=" ).append( queryExecutionCount )
				.append( ",query cache puts=" ).append( queryCachePutCount )
				.append( ",query cache hits=" ).append( queryCacheHitCount )
				.append( ",query cache misses=" ).append( queryCacheMissCount )
				.append(",update timestamps cache puts=").append(updateTimestampsCachePutCount)
				.append(",update timestamps cache hits=").append(updateTimestampsCacheHitCount)
				.append(",update timestamps cache misses=").append(updateTimestampsCacheMissCount)
				.append( ",max query time=" ).append( queryExecutionMaxTime )
				.append( ']' )
				.toString();
	}

	public String getQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	@Override
	public void deleteEntity(String entityName) {
	}

	@Override
	public void optimisticFailure(String entityName) {
	}

}

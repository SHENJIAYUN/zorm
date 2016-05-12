package com.zorm.action;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.zorm.exception.ZormException;
import com.zorm.persister.entity.Queryable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public class BulkOperationCleanupAction implements Executable, Serializable{
	
	private final Serializable[] affectedTableSpaces;
	
//	private final Set<EntityCleanup> entityCleanups = new HashSet<EntityCleanup>();
//	private final Set<CollectionCleanup> collectionCleanups = new HashSet<CollectionCleanup>();
//	private final Set<NaturalIdCleanup> naturalIdCleanups = new HashSet<NaturalIdCleanup>();
	
	public BulkOperationCleanupAction(SessionImplementor session,
			Queryable... affectedQueryables) {
		SessionFactoryImplementor factory = session.getFactory();
		LinkedHashSet<String> spacesList = new LinkedHashSet<String>();
		for ( Queryable persister : affectedQueryables ) {
			spacesList.addAll( Arrays.asList( (String[]) persister.getQuerySpaces() ) );
			
		}
		this.affectedTableSpaces = spacesList.toArray( new String[ spacesList.size() ] );
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return null;
	}

	@Override
	public void beforeExecutions() throws ZormException {
		
	}

	@Override
	public void execute() throws ZormException {
		
	}

	@Override
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return new AfterTransactionCompletionProcess() {
			@Override
			public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
//				for ( EntityCleanup cleanup : entityCleanups ) {
//					cleanup.release();
//				}
//				entityCleanups.clear();
//
//				for ( NaturalIdCleanup cleanup : naturalIdCleanups ) {
//					cleanup.release();
//
//				}
//				entityCleanups.clear();
//
//				for ( CollectionCleanup cleanup : collectionCleanups ) {
//					cleanup.release();
//				}
//				collectionCleanups.clear();
			}
		};
	}
	
//	private static class EntityCleanup {
//		private final EntityRegionAccessStrategy cacheAccess;
//		private final SoftLock cacheLock;
//
//		private EntityCleanup(EntityRegionAccessStrategy cacheAccess) {
//			this.cacheAccess = cacheAccess;
//			this.cacheLock = cacheAccess.lockRegion();
//			cacheAccess.removeAll();
//		}
//
//		private void release() {
//			cacheAccess.unlockRegion( cacheLock );
//		}
//	}
	
//	private class NaturalIdCleanup {
//		private final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy;
//		private final SoftLock cacheLock;
//
//		public NaturalIdCleanup(NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy) {
//			this.naturalIdCacheAccessStrategy = naturalIdCacheAccessStrategy;
//			this.cacheLock = naturalIdCacheAccessStrategy.lockRegion();
//			naturalIdCacheAccessStrategy.removeAll();
//		}
//
//		private void release() {
//			naturalIdCacheAccessStrategy.unlockRegion( cacheLock );
//		}
//	}
	
//	private static class CollectionCleanup {
//		private final CollectionRegionAccessStrategy cacheAccess;
//		private final SoftLock cacheLock;
//
//		private CollectionCleanup(CollectionRegionAccessStrategy cacheAccess) {
//			this.cacheAccess = cacheAccess;
//			this.cacheLock = cacheAccess.lockRegion();
//			cacheAccess.removeAll();
//		}
//
//		private void release() {
//			cacheAccess.unlockRegion( cacheLock );
//		}
//	}

	@Override
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	
}

package com.zorm.event;

import com.zorm.LockMode;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;

public class DefaultLoadEventListener implements LoadEventListener {

	private static final long serialVersionUID = -7726524981373695191L;
	
	public static final Object REMOVED_ENTITY_MARKER = new Object();
	public static final Object INCONSISTENT_RTN_CLASS_MARKER = new Object();
	
	@Override
	public void onLoad(LoadEvent event, LoadType loadType) {
		final SessionImplementor source = event.getSession();

		EntityPersister persister;
		persister = source.getFactory().getEntityPersister( event.getEntityClassName() );
		if ( persister == null ) {
			throw new ZormException(
					"Unable to locate persister: " +
					event.getEntityClassName()
				);
		}
		
		final  EntityKey keyToLoad = source.generateEntityKey( event.getEntityId(), persister );
		try {
			if ( event.getLockMode() == LockMode.NONE ) {
				event.setResult( proxyOrLoad(event, persister, keyToLoad, loadType) );
			}
		}
		catch(ZormException e) {
			throw e;
		}
	}

	protected Object proxyOrLoad(final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad, 
			final LoadType options) {
		 return load(event, persister, keyToLoad, options);
	}

	protected Object load(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad, 
			final LoadType options) {
		Object entity = doLoad(event, persister, keyToLoad, options);
		return entity;
	}

	protected Object doLoad(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {
		Object entity = loadFromSessionCache( event, keyToLoad, options );
		if ( entity == REMOVED_ENTITY_MARKER ) {
			return null;
		}
		if ( entity == INCONSISTENT_RTN_CLASS_MARKER ) {
			return null;
		}
		if ( entity != null ) {
			return entity;
		}
		
		entity = loadFromSecondLevelCache(event, persister, options);
		if(entity == null){
		    entity = loadFromDatasource(event, persister, keyToLoad, options);
		}
		return entity;
	}

	protected Object loadFromSecondLevelCache(
			LoadEvent event,
			EntityPersister persister,
			LoadType options) {
		return null;
	}

	protected Object loadFromSessionCache(
			LoadEvent event, 
			EntityKey keyToLoad,
			LoadType options) {
		SessionImplementor session = event.getSession();
		Object old = session.getEntityUsingInterceptor(keyToLoad);
		if(old != null){
			EntityEntry oldEntry = session.getPersistenceContext().getEntry( old );
			if ( options.isCheckDeleted() ) {
				Status status = oldEntry.getStatus();
				if ( status == Status.DELETED || status == Status.GONE ) {
					return REMOVED_ENTITY_MARKER;
				}
			}
			if ( options.isAllowNulls() ) {
				final EntityPersister persister = event.getSession().getFactory().getEntityPersister( keyToLoad.getEntityName() );
				if ( ! persister.isInstance( old ) ) {
					return INCONSISTENT_RTN_CLASS_MARKER;
				}
			}
		}
		return old;
	}

	protected Object loadFromDatasource(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {
		final SessionImplementor source = event.getSession();
		Object entity = persister.load(
				event.getEntityId(),
				event.getInstanceToLoad(),
				event.getLockOptions(),
				source
		);
		return entity;
	}

}

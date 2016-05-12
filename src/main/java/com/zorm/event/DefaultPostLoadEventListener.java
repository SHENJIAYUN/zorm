package com.zorm.event;

import com.zorm.LockMode;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.AssertionFailure;
import com.zorm.persister.entity.EntityPersister;

public class DefaultPostLoadEventListener implements PostLoadEventListener {

	public void onPostLoad(PostLoadEvent event) {
//		final Object entity = event.getEntity();
//		final EntityEntry entry = event.getSession().getPersistenceContext().getEntry( entity );
//		if ( entry == null ) {
//			throw new AssertionFailure( "possible non-threadsafe access to the session" );
//		}
//
//		final LockMode lockMode = entry.getLockMode();
//		if ( LockMode.PESSIMISTIC_FORCE_INCREMENT.equals( lockMode ) ) {
//			final EntityPersister persister = entry.getPersister();
//			Object nextVersion = persister.forceVersionIncrement(
//					entry.getId(), entry.getVersion(), event.getSession()
//			);
//			entry.forceLocked( entity, nextVersion );
//		}
//		else if ( LockMode.OPTIMISTIC_FORCE_INCREMENT.equals( lockMode ) ) {
//			EntityIncrementVersionProcess incrementVersion = new EntityIncrementVersionProcess( entity, entry );
//			event.getSession().getActionQueue().registerProcess( incrementVersion );
//		}
//		else if ( LockMode.OPTIMISTIC.equals( lockMode ) ) {
//			EntityVerifyVersionProcess verifyVersion = new EntityVerifyVersionProcess( entity, entry );
//			event.getSession().getActionQueue().registerProcess( verifyVersion );
//		}
//
//		if ( event.getPersister().implementsLifecycle() ) {
//			//log.debug( "calling onLoad()" );
//			( ( Lifecycle ) event.getEntity() ).onLoad( event.getSession(), event.getId() );
//		}

	}

}

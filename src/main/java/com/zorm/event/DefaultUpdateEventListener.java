package com.zorm.event;

import java.io.Serializable;

import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.ObjectDeletedException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;

public class DefaultUpdateEventListener extends DefaultSaveOrUpdateEventListener {

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		// this implementation is supposed to tolerate incorrect unsaved-value
		// mappings, for the purpose of backward-compatibility
		EntityEntry entry = event.getSession().getPersistenceContext().getEntry( event.getEntity() );
		if ( entry!=null ) {
			if ( entry.getStatus()== Status.DELETED ) {
				throw new ObjectDeletedException( "deleted instance passed to update()", null, event.getEntityName() );
			}
			else {
				return entityIsPersistent(event);
			}
		}
		else {
			//entityIsDetached(event);
			return null;
		}
	}
	
	/**
	 * If the user specified an id, assign it to the instance and use that, 
	 * otherwise use the id already assigned to the instance
	 */
	protected Serializable getUpdateId(
			Object entity,
			EntityPersister persister,
			Serializable requestedId,
			SessionImplementor session) throws ZormException {
		if ( requestedId == null ) {
			//return super.getUpdateId( entity, persister, requestedId, session );
		    return null;
		}
		else {
			persister.setIdentifier( entity, requestedId, session );
			return requestedId;
		}
	}

}
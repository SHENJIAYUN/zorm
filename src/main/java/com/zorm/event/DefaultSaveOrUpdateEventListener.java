package com.zorm.event;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.action.CascadingAction;
import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.PersistentObjectException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.MessageHelper;

public class DefaultSaveOrUpdateEventListener extends AbstractSaveEventListener implements SaveOrUpdateEventListener{

	private static final long serialVersionUID = 260424542312803940L;
	
	private static final Log log = LogFactory.getLog(DefaultSaveOrUpdateEventListener.class);

	@Override
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws ZormException {
		final SessionImplementor source = event.getSession();
		//获取持久化对象
		final Object object = event.getObject();	
		
		final Serializable requestedId = event.getRequestedId();
		if(requestedId==null){
		  final Object entity = source.getPersistenceContext().unproxyAndReassociate( object );
	      //设置对象属性值
		  event.setEntity(entity);
	      event.setEntry(source.getPersistenceContext().getEntry(entity));
	      event.setRequestedId(performSaveOrUpdate(event));
		}
	}

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		EntityState entityState = getEntityState(
				event.getEntity(),
				event.getEntityName(),
				event.getEntry(),
				event.getSession()
		);

		switch ( entityState ) {
			case DETACHED:
				entityIsDetached( event );
				return null;
			case PERSISTENT:
				return entityIsPersistent( event );
			default: 
				return entityIsTransient( event );
		}
	}
	
	protected void entityIsDetached(SaveOrUpdateEvent event) {
	}
	
	@Override
    protected CascadingAction getCascadeAction() {
		return CascadingAction.SAVE_UPDATE;
	}
	
	protected Serializable entityIsPersistent(SaveOrUpdateEvent event){
		EntityEntry entityEntry = event.getEntry();
		if ( entityEntry == null ) {
			throw new AssertionFailure( "entity was transient or detached" );
		}
		else {

			if ( entityEntry.getStatus() == Status.DELETED ) {
				throw new AssertionFailure( "entity was deleted" );
			}

			final SessionFactoryImplementor factory = event.getSession().getFactory();

			Serializable requestedId = event.getRequestedId();

			Serializable savedId;
			if ( requestedId == null ) {
				savedId = entityEntry.getId();
			}
			else {

				final boolean isEqual = !entityEntry.getPersister().getIdentifierType()
						.isEqual( requestedId, entityEntry.getId(), factory );

				if ( isEqual ) {
					throw new PersistentObjectException(
							"object passed to save() was already persistent: " +
									MessageHelper.infoString( entityEntry.getPersister(), requestedId, factory )
					);
				}

				savedId = requestedId;

			}

			return savedId;

		}
	}

	public Serializable saveWithGeneratedOrRequestedId(SaveOrUpdateEvent event) {
		return saveWithGeneratedId(
				event.getEntity(),
				event.getEntityName(),
				null,
				event.getSession(),
				true
		);
	}
	
	protected Serializable entityIsTransient(SaveOrUpdateEvent event) {
		log.debug("Saving transient instance");
		final EventSource source = event.getSession();
		EntityEntry entityEntry = event.getEntry();
		if ( entityEntry != null ) {
			if ( entityEntry.getStatus() == Status.DELETED ) {
				//source.forceFlush( entityEntry );
			}
			else {
				throw new AssertionFailure( "entity was persistent" );
			}
		}
		
		Serializable id = saveWithGeneratedOrRequestedId( event );
		source.getPersistenceContext().reassociateProxy( event.getObject(), id );

		return id;
	}

}

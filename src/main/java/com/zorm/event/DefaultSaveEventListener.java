package com.zorm.event;

import java.io.Serializable;

import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.AssertionFailure;

//默认的save事件监听器
public class DefaultSaveEventListener extends DefaultSaveOrUpdateEventListener{
	
	private static final long serialVersionUID = 4273600116416332332L;

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		EntityEntry entry = event.getSession().getPersistenceContext().getEntry( event.getEntity() );
		if ( entry!=null && entry.getStatus() != Status.DELETED ) {
			return entityIsPersistent(event);
		}
		else {
			return entityIsTransient(event);
		}
	}

	

	public Serializable saveWithGeneratedOrRequestedId(SaveOrUpdateEvent event) {
        if(event.getRequestedId() == null){
        	return super.saveWithGeneratedOrRequestedId(event);
        }
		return null;
	}

}

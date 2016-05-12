package com.zorm.event;

import java.io.Serializable;

import com.zorm.exception.ZormException;

public interface SaveOrUpdateEventListener extends Serializable{
	/** 
     * Handle the given update event.
     *
     * @param event The update event to be handled.
     * @throws HibernateException
     */
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws ZormException;
}

package com.zorm.event;

import java.io.Serializable;
import java.util.Set;

import com.zorm.exception.ZormException;

public interface DeleteEventListener extends Serializable {

    /** Handle the given delete event.
     *
     * @param event The delete event to be handled.
     * @throws HibernateException
     */
	public void onDelete(DeleteEvent event) throws ZormException;

	public void onDelete(DeleteEvent event, Set transientEntities);

}

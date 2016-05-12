package com.zorm.event;

import java.util.Set;

import com.zorm.engine.ActionQueue;
import com.zorm.session.Session;
import com.zorm.session.SessionImplementor;

public interface EventSource extends SessionImplementor,Session{

	public ActionQueue getActionQueue();

	/**
	 * Cascade delete an entity instance
	 */
	public void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, Set transientEntities);

}

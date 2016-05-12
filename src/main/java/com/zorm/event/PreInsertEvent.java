package com.zorm.event;

import java.io.Serializable;

import com.zorm.persister.entity.EntityPersister;

public class PreInsertEvent extends AbstractPreDatabaseOperationEvent {
	private Object[] state;

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param entity The entity to be inserted.
	 * @param id The id to use in the insertion.
	 * @param state The state to be inserted.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreInsertEvent(
			Object entity,
			Serializable id,
			Object[] state,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.state = state;
	}

	/**
	 * Getter for property 'state'.  These are the values to be inserted.
	 *
	 * @return Value for property 'state'.
	 */
	public Object[] getState() {
		return state;
	}
}

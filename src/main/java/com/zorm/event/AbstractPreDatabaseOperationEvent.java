package com.zorm.event;

import java.io.Serializable;

import com.zorm.persister.entity.EntityPersister;

public abstract class AbstractPreDatabaseOperationEvent extends AbstractEvent{
	private final Object entity;
	private final Serializable id;
	private final EntityPersister persister;

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param source The session from which the event originated.
	 * @param entity The entity to be invloved in the database operation.
	 * @param id The entity id to be invloved in the database operation.
	 * @param persister The entity's persister.
	 */
	public AbstractPreDatabaseOperationEvent(
			EventSource source,
			Object entity,
			Serializable id,
			EntityPersister persister) {
		super( source );
		this.entity = entity;
		this.id = id;
		this.persister = persister;
	}

	/**
	 * Retrieves the entity involved in the database operation.
	 *
	 * @return The entity.
	 */
	public Object getEntity() {
		return entity;
	}

	/**
	 * The id to be used in the database operation.
	 *
	 * @return The id.
	 */
	public Serializable getId() {
		return id;
	}

	/**
	 * The persister for the {@link #getEntity entity}.
	 *
	 * @return The entity persister.
	 */
	public EntityPersister getPersister() {
		return persister;
	}

	/**
	 * Getter for property 'source'.  This is the session from which the event
	 * originated.
	 * <p/>
	 * Some of the pre-* events had previous exposed the event source using
	 * getSource() because they had not originally extended from
	 * {@link AbstractEvent}.
	 *
	 * @return Value for property 'source'.
	 * @deprecated Use {@link #getSession} instead
	 */
	public EventSource getSource() {
		return getSession();
	}
}

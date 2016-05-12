package com.zorm.event;

import java.io.Serializable;

import com.zorm.persister.entity.EntityPersister;

public class PostLoadEvent extends AbstractEvent {
	private Object entity;
	private Serializable id;
	private EntityPersister persister;

	public PostLoadEvent(EventSource session) {
		super(session);
	}

	public Object getEntity() {
		return entity;
	}
	
	public EntityPersister getPersister() {
		return persister;
	}
	
	public Serializable getId() {
		return id;
	}

	public PostLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	
	public PostLoadEvent setId(Serializable id) {
		this.id = id;
		return this;
	}

	public PostLoadEvent setPersister(EntityPersister persister) {
		this.persister = persister;
		return this;
	}
	
}

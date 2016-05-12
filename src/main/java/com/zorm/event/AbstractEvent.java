package com.zorm.event;

import java.io.Serializable;

public abstract class AbstractEvent implements Serializable{
	private final EventSource session;
	
	public AbstractEvent(EventSource source) {
		this.session = source;
	}
	
	public final EventSource getSession() {
		return session;
	}
}

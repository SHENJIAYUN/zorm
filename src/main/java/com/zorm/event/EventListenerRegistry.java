package com.zorm.event;

import java.io.Serializable;

import com.zorm.service.Service;

public interface EventListenerRegistry extends Service,Serializable{
	public <T> EventListenerGroup<T> getEventListenerGroup(EventType<T> eventType);
}

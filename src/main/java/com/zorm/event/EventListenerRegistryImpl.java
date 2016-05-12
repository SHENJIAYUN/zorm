package com.zorm.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.zorm.exception.ZormException;
import static com.zorm.event.EventType.*;

public class EventListenerRegistryImpl implements EventListenerRegistry {

	private Map<EventType, EventListenerGroupImpl> registeredEventListenersMap = prepareListenerMap();

	@Override
	public <T> EventListenerGroupImpl<T> getEventListenerGroup(
			EventType<T> eventType) {
		EventListenerGroupImpl<T> listeners = registeredEventListenersMap
				.get(eventType);
		if (listeners == null) {
			throw new ZormException("Unable to find listeners for type ["
					+ eventType.eventName() + "]");
		}
		return listeners;
	}

	// 注册监听器
	private Map<EventType, EventListenerGroupImpl> prepareListenerMap() {
		final Map<EventType, EventListenerGroupImpl> workMap = new HashMap<EventType, EventListenerGroupImpl>();
		// save listeners
		prepareListeners(SAVE, new DefaultSaveEventListener(), workMap);

		// save-update listeners
		prepareListeners(SAVE_UPDATE, new DefaultSaveOrUpdateEventListener(),
				workMap);

		// auto-flush listeners
		prepareListeners(AUTO_FLUSH, new DefaultAutoFlushEventListener(),
				workMap);

		// delete listeners
		prepareListeners(DELETE, new DefaultDeleteEventListener(), workMap);

		// post-delete listeners
		prepareListeners(POST_DELETE, workMap);

		// post-commit-delete listeners
		prepareListeners(POST_COMMIT_DELETE, workMap);

		// flush listeners
		prepareListeners(FLUSH, new DefaultFlushEventListener(), workMap);

		// flush-entity listeners
		prepareListeners(FLUSH_ENTITY, new DefaultFlushEntityEventListener(),
				workMap);

		prepareListeners(PRE_LOAD, new DefaultPreLoadEventListener(), workMap);

		// load listeners
		prepareListeners(LOAD, new DefaultLoadEventListener(), workMap);

		prepareListeners(POST_LOAD, new DefaultPostLoadEventListener(), workMap);

		// pre-insert listeners
		prepareListeners(PRE_INSERT, workMap);

		// update listeners
		prepareListeners(UPDATE, new DefaultUpdateEventListener(), workMap);

		return Collections.unmodifiableMap(workMap);
	}

	private static <T> void prepareListeners(EventType<T> type,
			Map<EventType, EventListenerGroupImpl> map) {
		prepareListeners(type, null, map);
	}

	private static <T> void prepareListeners(EventType<T> type,
			T defaultListener, Map<EventType, EventListenerGroupImpl> map) {
		final EventListenerGroupImpl<T> listeners = new EventListenerGroupImpl<T>(
				type);
		if (defaultListener != null) {
			listeners.appendListener(defaultListener);
		}
		map.put(type, listeners);
	}

}

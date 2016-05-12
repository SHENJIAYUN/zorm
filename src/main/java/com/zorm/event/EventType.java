package com.zorm.event;

public class EventType<T> {
	
	private final String eventName;
	private final Class<? extends T> baseListenerInterface;
	
	private EventType(String eventName, Class<? extends T> baseListenerInterface) {
		this.eventName = eventName;
		this.baseListenerInterface = baseListenerInterface;
	}
	
	public static final EventType<SaveOrUpdateEventListener> UPDATE
	= new EventType<SaveOrUpdateEventListener>( "update", SaveOrUpdateEventListener.class );
	public static final EventType<PostDeleteEventListener> POST_COMMIT_DELETE
	= new EventType<PostDeleteEventListener>( "post-commit-delete", PostDeleteEventListener.class );
	public static final EventType<PreInsertEventListener> PRE_INSERT
	= new EventType<PreInsertEventListener>( "pre-insert", PreInsertEventListener.class );
	public static final EventType<PreLoadEventListener> PRE_LOAD
	= new EventType<PreLoadEventListener>( "pre-load", PreLoadEventListener.class );
	public static final EventType<LoadEventListener> LOAD
	= new EventType<LoadEventListener>( "load", LoadEventListener.class );
	public static final EventType<PostLoadEventListener> POST_LOAD
	= new EventType<PostLoadEventListener>( "post-load", PostLoadEventListener.class );
	public static final EventType<SaveOrUpdateEventListener> SAVE
	= new EventType<SaveOrUpdateEventListener>( "save", SaveOrUpdateEventListener.class );

	public static final EventType<PersistEventListener> PERSIST
	= new EventType<PersistEventListener>( "create", PersistEventListener.class );
	
	public static final EventType<FlushEventListener> FLUSH
	= new EventType<FlushEventListener>( "flush", FlushEventListener.class );
	
	public static final EventType<FlushEntityEventListener> FLUSH_ENTITY
	= new EventType<FlushEntityEventListener>( "flush-entity", FlushEntityEventListener.class );
	
	public static final EventType<DeleteEventListener> DELETE
	= new EventType<DeleteEventListener>( "delete", DeleteEventListener.class );
	
	public static final EventType<PostDeleteEventListener> POST_DELETE
	= new EventType<PostDeleteEventListener>( "post-delete", PostDeleteEventListener.class );
	
	public static final EventType<AutoFlushEventListener> AUTO_FLUSH
	= new EventType<AutoFlushEventListener>( "auto-flush", AutoFlushEventListener.class );
	
	public static final EventType<SaveOrUpdateEventListener> SAVE_UPDATE
	= new EventType<SaveOrUpdateEventListener>( "save-update", SaveOrUpdateEventListener.class );
	
	public String eventName() {
		return eventName;
	}

	public Class baseListenerInterface() {
		return baseListenerInterface;
	}
}

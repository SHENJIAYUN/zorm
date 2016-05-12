package com.zorm.event;

import com.zorm.LazyPropertyInitializer;
import com.zorm.exception.ZormException;
import com.zorm.type.CollectionType;
import com.zorm.type.EntityType;
import com.zorm.type.Type;

public abstract class AbstractVisitor {
	private final EventSource session;

	AbstractVisitor(EventSource session) {
		this.session = session;
	}
	
	public void processEntityPropertyValues(Object[] values, Type[] types)throws ZormException {
		for ( int i=0; i<types.length; i++ ) {
			if ( includeEntityProperty(values, i) ) {
				processValue( i, values, types );
			}
		}
	}
	
	void processValue(int i, Object[] values, Type[] types) {
		processValue( values[i], types[i] );
	}
	
	final Object processValue(Object value, Type type) throws ZormException {

		if ( type.isCollectionType() ) {
			return processCollection( value, (CollectionType) type );
		}
		else if ( type.isEntityType() ) {
			return processEntity( value, (EntityType) type );
		}
		else {
			return null;
		}
	}
	
	Object processCollection(Object collection, CollectionType type)
			throws ZormException {
		return null;
	}
	
	Object processEntity(Object value, EntityType entityType)
			throws ZormException {
		return null;
	}
	
	boolean includeEntityProperty(Object[] values, int i) {
		return includeProperty(values, i);
	}
	
	boolean includeProperty(Object[] values, int i) {
		return values[i]!= LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}
	
	final EventSource getSession() {
		return session;
	}
	
}

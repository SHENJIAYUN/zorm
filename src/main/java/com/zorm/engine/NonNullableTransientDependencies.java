package com.zorm.engine;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.zorm.session.SessionImplementor;

public class NonNullableTransientDependencies {

	// Multiple property paths can refer to the same transient entity, so use Set<String>
	// for the map value.
	private final Map<Object,Set<String>> propertyPathsByTransientEntity =
			new IdentityHashMap<Object,Set<String>>();

	/* package-protected */
	void add(String propertyName, Object transientEntity) {
		Set<String> propertyPaths = propertyPathsByTransientEntity.get( transientEntity );
		if ( propertyPaths == null ) {
			propertyPaths = new HashSet<String>();
			propertyPathsByTransientEntity.put( transientEntity, propertyPaths );
		}
		propertyPaths.add( propertyName );
	}

	public Iterable<Object> getNonNullableTransientEntities() {
		return propertyPathsByTransientEntity.keySet();
	}

	public Iterable<String> getNonNullableTransientPropertyPaths(Object entity) {
		return propertyPathsByTransientEntity.get( entity );
	}

	public boolean isEmpty() {
		return propertyPathsByTransientEntity.isEmpty();
	}

	public void resolveNonNullableTransientEntity(Object entity) {
		if ( propertyPathsByTransientEntity.remove( entity ) == null ) {
			throw new IllegalStateException( "Attempt to resolve a non-nullable, transient entity that is not a dependency." );
		}
	}

	public String toLoggableString(SessionImplementor session) {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( '[' );
		for ( Map.Entry<Object,Set<String>> entry : propertyPathsByTransientEntity.entrySet() ) {
			sb.append( "transientEntityName=" ).append( session.bestGuessEntityName( entry.getKey() ) );
			sb.append( " requiredBy=" ).append( entry.getValue() );
		}
		sb.append( ']' );
		return sb.toString();
	}
}


package com.zorm.engine;

import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.type.VersionType;

public final class Versioning {
	/**
	 * Apply no optimistic locking
	 */
	public static final int OPTIMISTIC_LOCK_NONE = -1;

	/**
	 * Apply optimistic locking based on the defined version or timestamp
	 * property.
	 */
	public static final int OPTIMISTIC_LOCK_VERSION = 0;

	/**
	 * Apply optimistic locking based on the a current vs. snapshot comparison
	 * of <b>all</b> properties.
	 */
	public static final int OPTIMISTIC_LOCK_ALL = 2;

	/**
	 * Apply optimistic locking based on the a current vs. snapshot comparison
	 * of <b>dirty</b> properties.
	 */
	public static final int OPTIMISTIC_LOCK_DIRTY = 1;

	/**
	 * Private constructor disallowing instantiation.
	 */
	private Versioning() {}

	/**
	 * Create an initial optimistic locking value according the {@link VersionType}
	 * contract for the version property.
	 *
	 * @param versionType The version type.
	 * @param session The originating session
	 * @return The initial optimistic locking value
	 */
	private static Object seed(VersionType versionType, SessionImplementor session) {
		Object seed = versionType.seed( session );
		return seed;
	}

	/**
	 * Create an initial optimistic locking value according the {@link VersionType}
	 * contract for the version property <b>if required</b> and inject it into
	 * the snapshot state.
	 *
	 * @param fields The current snapshot state
	 * @param versionProperty The index of the version property
	 * @param versionType The version type
	 * @param session The originating session
	 * @return True if we injected a new version value into the fields array; false
	 * otherwise.
	 */
	public static boolean seedVersion(
	        Object[] fields,
	        int versionProperty,
	        VersionType versionType,
	        SessionImplementor session) {
		Object initialVersion = fields[versionProperty];
		if (
			initialVersion==null ||
			// This next bit is to allow for both unsaved-value="negative"
			// and for "older" behavior where version number did not get
			// seeded if it was already set in the object
			// TODO: shift it into unsaved-value strategy
			( (initialVersion instanceof Number) && ( (Number) initialVersion ).longValue()<0 )
		) {
			fields[versionProperty] = seed( versionType, session );
			return true;
		}
		return false;
	}


	/**
	 * Generate the next increment in the optimistic locking value according
	 * the {@link VersionType} contract for the version property.
	 *
	 * @param version The current version
	 * @param versionType The version type
	 * @param session The originating session
	 * @return The incremented optimistic locking value.
	 */
	public static Object increment(Object version, VersionType versionType, SessionImplementor session) {
		Object next = versionType.next( version, session );
		return next;
	}

	/**
	 * Inject the optimistic locking value into the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param version The optimistic locking value
	 * @param persister The entity persister
	 */
	public static void setVersion(Object[] fields, Object version, EntityPersister persister) {
		if ( !persister.isVersioned() ) {
			return;
		}
		fields[ persister.getVersionProperty() ] = version;
	}

	/**
	 * Extract the optimistic locking value out of the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param persister The entity persister
	 * @return The extracted optimistic locking value
	 */
	public static Object getVersion(Object[] fields, EntityPersister persister) {
		if ( !persister.isVersioned() ) {
			return null;
		}
		return fields[ persister.getVersionProperty() ];
	}

	/**
	 * Do we need to increment the version number, given the dirty properties?
	 *
	 * @param dirtyProperties The array of property indexes which were deemed dirty
	 * @param hasDirtyCollections Were any collections found to be dirty (structurally changed)
	 * @param propertyVersionability An array indicating versionability of each property.
	 * @return True if a version increment is required; false otherwise.
	 */
	public static boolean isVersionIncrementRequired(
			final int[] dirtyProperties,
			final boolean hasDirtyCollections,
			final boolean[] propertyVersionability) {
		if ( hasDirtyCollections ) {
			return true;
		}
		for ( int i = 0; i < dirtyProperties.length; i++ ) {
			if ( propertyVersionability[ dirtyProperties[i] ] ) {
				return true;
			}
		}
		return false;
	}
}

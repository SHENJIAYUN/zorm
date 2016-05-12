package com.zorm.engine;

import java.io.Serializable;

import com.zorm.LazyPropertyInitializer;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityMode;
import com.zorm.exception.TransientObjectException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.type.EntityType;
import com.zorm.type.Type;

public class ForeignKeys {
	private ForeignKeys() {
	}

	public static class Nullifier {
		private final boolean isDelete;
		private final boolean isEarlyInsert;
		private final SessionImplementor session;
		private final Object self;

		public Nullifier(Object self, boolean isDelete, boolean isEarlyInsert,
				SessionImplementor session) {
			this.isDelete = isDelete;
			this.isEarlyInsert = isEarlyInsert;
			this.session = session;
			this.self = self;
		}

		public void nullifyTransientReferences(final Object[] values,
				final Type[] types) throws ZormException {
			for (int i = 0; i < types.length; i++) {
				values[i] = nullifyTransientReferences(values[i], types[i]);
			}

		}

		private boolean isNullifiable(final String entityName, Object object) {

			if (object == LazyPropertyInitializer.UNFETCHED_PROPERTY)
				return false; 

			if (object == self) {
				return isEarlyInsert
						|| (isDelete && session.getFactory().getDialect()
								.hasSelfReferentialForeignKeyBug());
			}

			EntityEntry entityEntry = session.getPersistenceContext().getEntry(
					object);
			if (entityEntry == null) {
				return isTransient(entityName, object, null, session);
			} else {
				return entityEntry.isNullifiable(isEarlyInsert, session);
			}

		}

		private Object nullifyTransientReferences(final Object value,
				final Type type) {
			if (value == null) {
				return null;
			} else if (type.isEntityType()) {
				EntityType entityType = (EntityType) type;
				if (entityType.isOneToOne()) {
					return value;
				} else {
					String entityName = entityType.getAssociatedEntityName();
					return isNullifiable(entityName, value) ? null : value;
				}
			} else if (type.isAnyType()) {
				return isNullifiable(null, value) ? null : value;
			} else {
				return value;
			}
		}

	}

	public static Serializable getEntityIdentifierIfNotUnsaved(
			final String entityName, final Object object,
			final SessionImplementor session) throws ZormException {
		if (object == null) {
			return null;
		} else {
			Serializable id = session.getContextEntityIdentifier(object);
			if (id == null) {
				// context-entity-identifier returns null explicitly if the
				// entity
				// is not associated with the persistence context; so make some
				// deeper checks...
				if (isTransient(entityName, object, Boolean.FALSE, session)) {
					throw new TransientObjectException(
							"object references an unsaved transient instance - save the transient instance before flushing: "
									+ (entityName == null ? session
											.guessEntityName(object)
											: entityName));
				}
				id = session.getEntityPersister(entityName, object)
						.getIdentifier(object, session);
			}
			return id;
		}
	}

	public static boolean isTransient(String entityName, Object entity,
			Boolean assumed, SessionImplementor session) throws ZormException {

		if (entity == LazyPropertyInitializer.UNFETCHED_PROPERTY) {
			return false;
		}

		Boolean isUnsaved = session.getInterceptor().isTransient(entity);
		if (isUnsaved != null)
			return isUnsaved.booleanValue();

		// let the persister inspect the instance to decide
		EntityPersister persister = session.getEntityPersister(entityName,
				entity);
		isUnsaved = persister.isTransient(entity, session);
		if (isUnsaved != null)
			return isUnsaved.booleanValue();

		// we use the assumed value, if there is one, to avoid hitting
		// the database
		if (assumed != null)
			return assumed.booleanValue();

		// hit the database, after checking the session cache for a snapshot
		Object[] snapshot = session.getPersistenceContext()
				.getDatabaseSnapshot(persister.getIdentifier(entity, session),
						persister);
		return snapshot == null;

	}

	public static boolean isNotTransient(String entityName, Object entity,
			Boolean assumed, SessionImplementor session) {
		if (session.getPersistenceContext().isEntryFor(entity))
			return true;
		return !isTransient(entityName, entity, assumed, session);
	}
}

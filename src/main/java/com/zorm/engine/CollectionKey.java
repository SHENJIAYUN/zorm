package com.zorm.engine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.zorm.entity.EntityMode;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.MessageHelper;

public final class CollectionKey implements Serializable {
	private final String role;
	private final Serializable key;
	private final Type keyType;
	private final SessionFactoryImplementor factory;
	private final int hashCode;
	private EntityMode entityMode;

	public CollectionKey(CollectionPersister persister, Serializable key) {
		this(
				persister.getRole(),
				key,
				persister.getKeyType(),
				persister.getOwnerEntityPersister().getEntityMetamodel().getEntityMode(),
				persister.getFactory()
		);
	}

	public CollectionKey(CollectionPersister persister, Serializable key, EntityMode em) {
		this( persister.getRole(), key, persister.getKeyType(), em, persister.getFactory() );
	}

	private CollectionKey(
			String role,
	        Serializable key,
	        Type keyType,
	        EntityMode entityMode,
	        SessionFactoryImplementor factory) {
		this.role = role;
		this.key = key;
		this.keyType = keyType;
		this.entityMode = entityMode;
		this.factory = factory;
		this.hashCode = generateHashCode(); //cache the hashcode
	}

	public boolean equals(Object other) {
		CollectionKey that = (CollectionKey) other;
		return that.role.equals(role) &&
		       keyType.isEqual(that.key, key, factory);
	}

	public int generateHashCode() {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + keyType.getHashCode(key, factory);
		return result;
	}

	public int hashCode() {
		return hashCode;
	}

	public String getRole() {
		return role;
	}

	public Serializable getKey() {
		return key;
	}

	public String toString() {
		return "CollectionKey" +
		       MessageHelper.collectionInfoString( factory.getCollectionPersister(role), key, factory );
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws java.io.IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( key );
		oos.writeObject( keyType );
		oos.writeObject( entityMode.toString() );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 * @return The deserialized CollectionKey
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static CollectionKey deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionKey(
				( String ) ois.readObject(),
		        ( Serializable ) ois.readObject(),
		        ( Type ) ois.readObject(),
		        EntityMode.parse( ( String ) ois.readObject() ),
		        ( session == null ? null : session.getFactory() )
		);
	}
}
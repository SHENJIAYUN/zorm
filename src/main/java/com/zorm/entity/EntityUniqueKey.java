package com.zorm.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class EntityUniqueKey {
	private final String uniqueKeyName;
	private final String entityName;
	private final Object key;
	private final Type keyType;
	private final EntityMode entityMode;
	private final int hashCode;

	public EntityUniqueKey(
			final String entityName,
	        final String uniqueKeyName,
	        final Object semiResolvedKey,
	        final Type keyType,
	        final EntityMode entityMode,
	        final SessionFactoryImplementor factory
	) {
		this.uniqueKeyName = uniqueKeyName;
		this.entityName = entityName;
		this.key = semiResolvedKey;
		this.keyType = keyType.getSemiResolvedType(factory);
		this.entityMode = entityMode;
		this.hashCode = generateHashCode(factory);
	}
	
	public int generateHashCode(SessionFactoryImplementor factory) {
		int result = 17;
		result = 37 * result + entityName.hashCode();
		result = 37 * result + uniqueKeyName.hashCode();
		result = 37 * result + keyType.getHashCode(key, factory);
		return result;
	}

	public static EntityUniqueKey deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new EntityUniqueKey(
				( String ) ois.readObject(),
		        ( String ) ois.readObject(),
		        ois.readObject(),
		        ( Type ) ois.readObject(),
		        ( EntityMode ) ois.readObject(),
		        session.getFactory()
		);
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		checkAbilityToSerialize();
		oos.writeObject( uniqueKeyName );
		oos.writeObject( entityName );
		oos.writeObject( key );
		oos.writeObject( keyType );
		oos.writeObject( entityMode );
	}
	
	private void checkAbilityToSerialize() {
		if ( key != null && ! Serializable.class.isAssignableFrom( key.getClass() ) ) {
			throw new IllegalStateException(
					"Cannot serialize an EntityUniqueKey which represents a non " +
					"serializable property value [" + entityName + "." + uniqueKeyName + "]"
			);
		}
	}
}

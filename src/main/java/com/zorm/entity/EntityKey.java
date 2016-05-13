package com.zorm.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.zorm.exception.AssertionFailure;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.EqualsHelper;

public final class EntityKey implements Serializable{
	private static final long serialVersionUID = 7159183300241960324L;
	private final Serializable identifier;
	private final String entityName;
	private final String rootEntityName;
	private final String tenantId;
	private final int hashCode;
	private final Type identifierType;
	private final boolean isBatchLoadable;
	private final SessionFactoryImplementor factory;
	
	public EntityKey(Serializable id, EntityPersister persister, String tenantId) {
		if ( id == null ) {
			throw new AssertionFailure( "null identifier" );
		}
		this.identifier = id; 
		this.rootEntityName = persister.getRootEntityName();
		this.entityName = persister.getEntityName();
		this.tenantId = tenantId;

		this.identifierType = persister.getIdentifierType();
		this.isBatchLoadable = persister.isBatchLoadable();
		this.factory = persister.getFactory();
		this.hashCode = generateHashCode();
	}
	
	private EntityKey(
			Serializable identifier,
	        String rootEntityName,
	        String entityName,
	        Type identifierType,
	        boolean batchLoadable,
	        SessionFactoryImplementor factory,
			String tenantId) {
		this.identifier = identifier;
		this.rootEntityName = rootEntityName;
		this.entityName = entityName;
		this.identifierType = identifierType;
		this.isBatchLoadable = batchLoadable;
		this.factory = factory;
		this.tenantId = tenantId;
		this.hashCode = generateHashCode();
	}

	public Serializable getIdentifier() {
		return identifier;
	}
	
	private int generateHashCode() {
		int result = 17;
		result = 37 * result + rootEntityName.hashCode();
		result = 37 * result + identifierType.getHashCode( identifier, factory );
		return result;
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public boolean equals(Object other) {
		EntityKey otherKey = (EntityKey) other;
		return otherKey.rootEntityName.equals(this.rootEntityName) &&
				identifierType.isEqual(otherKey.identifier, this.identifier, factory) &&
				EqualsHelper.equals( tenantId, otherKey.tenantId );
	}

	public String getEntityName() {
		return entityName;
	}

	public static EntityKey deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new EntityKey(
				( Serializable ) ois.readObject(),
		        (String) ois.readObject(),
				(String) ois.readObject(),
		        ( Type ) ois.readObject(),
		        ois.readBoolean(),
		        ( session == null ? null : session.getFactory() ),
				(String) ois.readObject()
		);
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( identifier );
		oos.writeObject( rootEntityName );
		oos.writeObject( entityName );
		oos.writeObject( identifierType );
		oos.writeBoolean( isBatchLoadable );
		oos.writeObject( tenantId );
	}

}

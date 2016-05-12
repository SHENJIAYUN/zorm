package com.zorm.type;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.exception.MappingException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class CustomCollectionType extends CollectionType{
	private final UserCollectionType userType;
	private final boolean customLogging;
	
	public CustomCollectionType(
			TypeFactory.TypeScope typeScope,
			Class userTypeClass,
			String role,
			String foreignKeyPropertyName) {
		super( typeScope, role, foreignKeyPropertyName );
		userType = createUserCollectionType( userTypeClass );
		customLogging = LoggableUserType.class.isAssignableFrom( userTypeClass );
	}
	
	private static UserCollectionType createUserCollectionType(Class userTypeClass) {
		if ( !UserCollectionType.class.isAssignableFrom( userTypeClass ) ) {
			throw new MappingException( "Custom type does not implement UserCollectionType: " + userTypeClass.getName() );
		}

		try {
			return ( UserCollectionType ) userTypeClass.newInstance();
		}
		catch ( InstantiationException ie ) {
			throw new MappingException( "Cannot instantiate custom type: " + userTypeClass.getName() );
		}
		catch ( IllegalAccessException iae ) {
			throw new MappingException( "IllegalAccessException trying to instantiate custom type: " + userTypeClass.getName() );
		}
	}

	public UserCollectionType getUserType() {
		return userType;
	}

	@Override
	public PersistentCollection wrap(SessionImplementor session,
			Object collection) {
		return null;
	}

	@Override
	public PersistentCollection instantiate(SessionImplementor session,
			CollectionPersister persister, Serializable key) {
		return null;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return null;
	}
}

package com.zorm.type;

import java.io.Serializable;
import java.util.Properties;

import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.ReflectHelper;

@SuppressWarnings("serial")
public final class TypeFactory implements Serializable{

	private final TypeScopeImpl typeScope = new TypeScopeImpl();
	
	public static interface TypeScope extends Serializable {
		public SessionFactoryImplementor resolveFactory();
	}
	
	
	private static class TypeScopeImpl implements TypeFactory.TypeScope {
		private SessionFactoryImplementor factory;

		public void injectSessionFactory(SessionFactoryImplementor factory) {
			if ( this.factory != null ) {
			}
			else {
			}
			this.factory = factory;
		}

		public SessionFactoryImplementor resolveFactory() {
			if ( factory == null ) {
				throw new ZormException( "SessionFactory for type scoping not yet known" );
			}
			return factory;
		}
	}

	public void injectSessionFactory(SessionFactoryImplementor factory) {
		typeScope.injectSessionFactory(factory);
	}

	public Type byClass(Class clazz, Properties parameters) {
//		if ( Type.class.isAssignableFrom( clazz ) ) {
//			return type( clazz, parameters );
//		}

//		if ( CompositeUserType.class.isAssignableFrom( clazz ) ) {
//			return customComponent( clazz, parameters );
//		}
//
//		if ( UserType.class.isAssignableFrom( clazz ) ) {
//			return custom( clazz, parameters );
//		}
//
//		if ( Lifecycle.class.isAssignableFrom( clazz ) ) {
//			// not really a many-to-one association *necessarily*
//			return manyToOne( clazz.getName() );
//		}

//		if ( Serializable.class.isAssignableFrom( clazz ) ) {
//			return serializable( clazz );
//		}

		return null;
	}

	public EntityType manyToOne(String persistentClass) {
		return new ManyToOneType(typeScope,persistentClass);
	}

	public Type manyToOne(String persistentClass, boolean lazy) {
		return new ManyToOneType( typeScope, persistentClass, lazy );
	}

	public EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				typeScope,
				persistentClass,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	public CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef) {
		Class typeClass;
		try {
			typeClass = ReflectHelper.classForName( typeName );
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new MappingException( "user collection type class not found: " + typeName, cnfe );
		}
		CustomCollectionType result = new CustomCollectionType( typeScope, typeClass, role, propertyRef );
		if ( typeParameters != null ) {
			injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

	public static void injectParameters(Object type, Properties parameters) {
		if ( ParameterizedType.class.isInstance( type ) ) {
			( (ParameterizedType) type ).setParameterValues(parameters);
		}
		else if ( parameters!=null && !parameters.isEmpty() ) {
			throw new MappingException( "type is not parameterized: " + type.getClass().getName() );
		}
	}

	public CollectionType bag(String role, String propertyRef) {
		return new BagType( typeScope, role, propertyRef );
	}

	public CollectionType list(String role, String propertyRef) {
		return new ListType( typeScope, role, propertyRef );
	}

	public CollectionType array(String role, String propertyRef, Class elementClass) {
		return new ArrayType( typeScope, role, propertyRef, elementClass );
	}
}

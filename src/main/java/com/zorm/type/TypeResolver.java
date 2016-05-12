package com.zorm.type;

import java.io.Serializable;
import java.util.Properties;

import com.zorm.exception.MappingException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.ReflectHelper;

public class TypeResolver implements Serializable{
	private static final long serialVersionUID = -5639977645085175410L;
	
	private final BasicTypeRegistry basicTypeRegistry;
	private final TypeFactory typeFactory;
	
	public TypeResolver() {
		this(new BasicTypeRegistry(), new TypeFactory());
	}
	
	public TypeResolver(BasicTypeRegistry basicTypeRegistry, TypeFactory typeFactory) {
		this.basicTypeRegistry = basicTypeRegistry;
		this.typeFactory = typeFactory;
	}
	
	public TypeResolver scope(SessionFactoryImplementor factory) {
		typeFactory.injectSessionFactory( factory );
		return new TypeResolver( basicTypeRegistry.shallowCopy(), typeFactory );
	}

	public Type heuristicType(String typeName, Properties parameters) throws MappingException {
		Type type = basic( typeName );
		if(type!=null){
			return type;
		}
		try {
			Class typeClass = ReflectHelper.classForName( typeName );
			if ( typeClass != null ) {
				return typeFactory.byClass( typeClass, parameters );
			}
		}
		catch ( ClassNotFoundException ignore ) {
		}
		return null;
	}

	public BasicType basic(String name) {
		return basicTypeRegistry.getRegisteredType( name );
	}

	public TypeFactory getTypeFactory() {
		return typeFactory;
	}
}

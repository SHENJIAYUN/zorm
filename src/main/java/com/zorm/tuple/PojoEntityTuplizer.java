package com.zorm.tuple;

import java.util.*;

import com.zorm.EntityNameResolver;
import com.zorm.config.Environment;
import com.zorm.entity.EntityMetamodel;
import com.zorm.entity.EntityMode;
import com.zorm.exception.ZormException;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.meta.EntityBinding;
import com.zorm.property.Getter;
import com.zorm.property.Setter;
import com.zorm.session.SessionFactoryImplementor;

public class PojoEntityTuplizer extends AbstractEntityTuplizer{
	private final Class mappedClass;
	private final Class proxyInterface;
	private final boolean lifecycleImplementor;
	private final Set lazyPropertyNames = new HashSet();
	private final ReflectionOptimizer optimizer;
	private final boolean isInstrumented;
	
	public PojoEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
		this.mappedClass = mappedEntity.getMappedClass();
		this.proxyInterface = mappedEntity.getProxyInterface();
		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedClass );
		this.isInstrumented = entityMetamodel.isInstrumented();

		Iterator iter = mappedEntity.getPropertyClosureIterator();
		while ( iter.hasNext() ) {
			Property property = (Property) iter.next();
			if ( property.isLazy() ) {
				lazyPropertyNames.add( property.getName() );
			}
		}

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[i].getMethodName();
			setterNames[i] = setters[i].getMethodName();
			propTypes[i] = getters[i].getReturnType();
		}

		if ( hasCustomAccessors || !Environment.useReflectionOptimizer() ) {
			optimizer = null;
		}
		else {
			optimizer = null;
			//optimizer = Environment.getBytecodeProvider().getReflectionOptimizer( mappedClass, getterNames, setterNames, propTypes );
		}

	}

	@Override
    public Object[] getPropertyValues(Object entity) throws ZormException {
		
	  return super.getPropertyValues( entity );
		
	}
	
	@Override
	public boolean hasUninitializedLazyProperties(Object entity) {
//		if ( getEntityMetamodel().hasLazyProperties() ) {
//			FieldInterceptor callback = FieldInterceptionHelper.extractFieldInterceptor( entity );
//			return callback != null && !callback.isInitialized();
//		}
//		else {
			return false;
//		}
	}
	
	@Override
	public EntityMode getEntityMode() {
		return null;
	}

	@Override
	public boolean isInstrumented() {
		return false;
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return null;
	}

	@Override
	public String determineConcreteSubclassEntityName(Object entityInstance,
			SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public Class getMappedClass() {
		return null;
	}

	@Override
	protected Getter buildPropertyGetter(Property mappedProperty,
			PersistentClass mappedEntity) {
		return  mappedProperty.getGetter( mappedEntity.getMappedClass() );
	}

	@Override
	protected Setter buildPropertySetter(Property mappedProperty,
			PersistentClass mappedEntity) {
		return mappedProperty.getSetter( mappedEntity.getMappedClass() );
	}

	@Override
	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		if ( optimizer == null ) {
			return new PojoInstantiator( persistentClass, null );
		}
		else {
			return new PojoInstantiator( persistentClass, optimizer.getInstantiationOptimizer() );
		}
	}

	@Override
	protected Instantiator buildInstantiator(EntityBinding mappingInfo) {
		return null;
	}

}

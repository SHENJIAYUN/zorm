package com.zorm.tuple;

import java.io.Serializable;
import java.util.*;

import com.zorm.LazyPropertyInitializer;
import com.zorm.entity.EntityMetamodel;
import com.zorm.entity.StandardProperty;
import com.zorm.event.EventListenerRegistry;
import com.zorm.event.EventType;
import com.zorm.event.PersistEventListener;
import com.zorm.exception.ZormException;
import com.zorm.id.Assigned;
import com.zorm.mapping.PersistentClass;
import com.zorm.meta.EntityBinding;
import com.zorm.property.Getter;
import com.zorm.property.Setter;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.mapping.Property;;

public abstract class AbstractEntityTuplizer implements EntityTuplizer{
	private final EntityMetamodel entityMetamodel;
	private final Getter idGetter;
	private final Setter idSetter;
	
	protected final Getter[] getters;
	protected final Setter[] setters;
	protected final int propertySpan;
	protected final boolean hasCustomAccessors;
	private final Instantiator instantiator;
	
	/*
	 * 构造给定属性的getter方法
	 */
	protected abstract Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity);
	/*
	 * 构造给定属性的setter方法
	 */
	protected abstract Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity);
	/*
	 * 构造给定实体的实例
	 */
	protected abstract Instantiator buildInstantiator(PersistentClass mappingInfo);
	
	protected abstract Instantiator buildInstantiator(EntityBinding mappingInfo);
	
	public AbstractEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappingInfo) {
		this.entityMetamodel = entityMetamodel;
		
		if(!entityMetamodel.getIdentifierProperty().isVirtual()){
			idGetter = buildPropertyGetter(mappingInfo.getIdentifierProperty(), mappingInfo);
		    idSetter = buildPropertySetter(mappingInfo.getIdentifierProperty(), mappingInfo);
		}
		else{
			idGetter = null;
			idSetter = null;
		}
		
		propertySpan = entityMetamodel.getPropertySpan();
		getters = new Getter[propertySpan];
		setters = new Setter[propertySpan];
		//
		Iterator itr = mappingInfo.getPropertyClosureIterator();
		boolean foundCustomAccessor = false;
		int i=0;
		while(itr.hasNext()){
			Property property = (Property)itr.next();
			getters[i] = buildPropertyGetter(property, mappingInfo);
			setters[i] = buildPropertySetter(property, mappingInfo);
			if(!property.isBasicPropertyAccessor()){
				foundCustomAccessor = true;
			}
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;
		instantiator = buildInstantiator(mappingInfo);
	}
	
	protected String getEntityName() {
		return entityMetamodel.getName();
	}
	
	protected Set getSubclassEntityNames() {
		return entityMetamodel.getSubclassEntityNames();
	}
	
	public Serializable getIdentifier(Object entity) {
		return getIdentifier(entity,null);
	}
	
	public Serializable getIdentifier(Object entity, SessionImplementor session) {
		final Object id;
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			id = entity;
		}
		else {
            id = idGetter.get( entity );
        }

		try {
			return (Serializable) id;
		}
		catch ( ClassCastException cce ) {
			StringBuilder msg = new StringBuilder( "Identifier classes must be serializable. " );
			if ( id != null ) {
				msg.append( id.getClass().getName() ).append( " is not serializable. " );
			}
			if ( cce.getMessage() != null ) {
				msg.append( cce.getMessage() );
			}
			throw new ClassCastException( msg.toString() );
		}
	}
	
	public void setIdentifier(Object entity,Serializable id) throws ZormException{
		setIdentifier(entity,id,null);
	}
	
	public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
		}
		else if(idSetter != null ) {
			idSetter.set( entity, id, getFactory() );
		}
	}
	
	protected final SessionFactoryImplementor getFactory() {
		return entityMetamodel.getSessionFactory();
	}
	
	private static Iterable<PersistEventListener> persistEventListeners(SessionImplementor session) {
		return session
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.PERSIST )
				.listeners();
	}
	
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
		resetIdentifier( entity, currentId, currentVersion, null );
	}
	
	public void resetIdentifier(
			Object entity,
			Serializable currentId,
			Object currentVersion,
			SessionImplementor session) {
		if ( entityMetamodel.getIdentifierProperty().getIdentifierGenerator() instanceof Assigned ) {
		}
		else {
			//reset the id
			Serializable result = entityMetamodel.getIdentifierProperty()
					.getUnsavedValue()
					.getDefaultValue( currentId );
			setIdentifier( entity, result, session );
			//reset the version
			//VersionProperty versionProperty = entityMetamodel.getVersionProperty();
//			if ( entityMetamodel.isVersioned() ) {
//				setPropertyValue(
//				        entity,
//				        entityMetamodel.getVersionPropertyIndex(),
//						versionProperty.getUnsavedValue().getDefaultValue( currentVersion )
//				);
//			}
		}
	}
	
	public Object getVersion(Object entity) throws ZormException {
		if ( !entityMetamodel.isVersioned() ) return null;
		return getters[ entityMetamodel.getVersionPropertyIndex() ].get( entity );
	}
	
	protected boolean shouldGetAllProperties(Object entity) {
		return !hasUninitializedLazyProperties( entity );
	}
	
	public Object[] getPropertyValues(Object entity) throws ZormException {
		boolean getAll = shouldGetAllProperties( entity );
		final int span = entityMetamodel.getPropertySpan();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			StandardProperty property = entityMetamodel.getProperties()[j];
			if ( getAll || !property.isLazy() ) {
				result[j] = getters[j].get( entity );
			}
			else {
				result[j] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
		}
		return result;
	}
	
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session)
			throws ZormException {
				final int span = entityMetamodel.getPropertySpan();
				final Object[] result = new Object[span];

				for ( int j = 0; j < span; j++ ) {
					result[j] = getters[j].getForInsert( entity, mergeMap, session );
				}
				return result;
			}

	public Object getPropertyValue(Object entity, int i) throws ZormException {
		return getters[i].get( entity );
	}
	
	public Object getPropertyValue(Object entity, String propertyPath) throws ZormException {
		int loc = propertyPath.indexOf('.');
		String basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		Integer index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		if (index == null) {
			propertyPath = "_identifierMapper." + propertyPath;
			loc = propertyPath.indexOf('.');
			basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		}
		index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		final Object baseValue = getPropertyValue( entity, index.intValue() );
		if ( loc > 0 ) {
			if ( baseValue == null ) {
				return null;
			}
			return null;
		}
		else {
			return baseValue;
		}
	}
	
	public void setPropertyValues(Object entity, Object[] values) throws ZormException {
		boolean setAll = !entityMetamodel.hasLazyProperties();

		for ( int j = 0; j < entityMetamodel.getPropertySpan(); j++ ) {
			if ( setAll || values[j] != LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				setters[j].set( entity, values[j], getFactory() );
			}
		}
	}
	
	public void setPropertyValue(Object entity, int i, Object value) throws ZormException {
		setters[i].set( entity, value, getFactory() );
	}
	
	public void setPropertyValue(Object entity, String propertyName, Object value) throws ZormException {
		setters[ entityMetamodel.getPropertyIndex( propertyName ) ].set( entity, value, getFactory() );
	}
	
	public final Object instantiate(Serializable id) throws ZormException {
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		return instantiate( id, null );
	}

	public final Object instantiate(Serializable id, SessionImplementor session) {
		Object result = getInstantiator().instantiate( id );
		if ( id != null ) {
			setIdentifier( result, id, session );
		}
		return result;
	}
	
	protected final Instantiator getInstantiator() {
		return instantiator;
	}

	public final Object instantiate() throws ZormException {
		return instantiate( null, null );
	}
	
	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {}

	public boolean hasUninitializedLazyProperties(Object entity) {
		return false;
	}

	public final boolean isInstance(Object object) {
        return getInstantiator().isInstance( object );
	}

	public boolean isLifecycleImplementor() {
		return false;
	}

	protected final EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}
	
	@Override
    public String toString() {
		return getClass().getName() + '(' + getEntityMetamodel().getName() + ')';
	}

	public Getter getIdentifierGetter() {
		return idGetter;
	}

	public Getter getVersionGetter() {
		if ( getEntityMetamodel().isVersioned() ) {
			return getGetter( getEntityMetamodel().getVersionPropertyIndex() );
		}
		return null;
	}

	public Getter getGetter(int i) {
		return getters[i];
	}

	
}

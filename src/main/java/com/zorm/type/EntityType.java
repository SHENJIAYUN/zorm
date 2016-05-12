package com.zorm.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.zorm.engine.ForeignKeys;
import com.zorm.engine.Mapping;
import com.zorm.engine.PersistenceContext;
import com.zorm.entity.EntityUniqueKey;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Joinable;
import com.zorm.persister.entity.UniqueKeyLoadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.ReflectHelper;

public abstract class EntityType extends AbstractType implements AssociationType{

	private final TypeFactory.TypeScope scope;
	private final String associatedEntityName;
	protected final String uniqueKeyPropertyName;
	protected final boolean isEmbeddedInXML;
	private final boolean eager;
	private final boolean unwrapProxy;

	private transient Class returnedClass;
	
	protected EntityType(
			TypeFactory.TypeScope scope,
			String entityName,
			String uniqueKeyPropertyName,
			boolean eager,
			boolean isEmbeddedInXML,
			boolean unwrapProxy) {
		this.scope = scope;
		this.associatedEntityName = entityName;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;
		this.isEmbeddedInXML = isEmbeddedInXML;
		this.eager = eager;
		this.unwrapProxy = unwrapProxy;
	}
	
	protected EntityType(
			TypeFactory.TypeScope scope,
			String entityName,
			String uniqueKeyPropertyName,
			boolean eager,
			boolean unwrapProxy) {
		this.scope = scope;
		this.associatedEntityName = entityName;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;
		this.isEmbeddedInXML = true;
		this.eager = eager;
		this.unwrapProxy = unwrapProxy;
	}
	
	public String getAssociatedEntityName(SessionFactoryImplementor factory) {
		return getAssociatedEntityName();
	}
	
	Type getIdentifierType(SessionImplementor session) {
		return getIdentifierType( session.getFactory() );
	}
	
	protected boolean isNull(Object owner, SessionImplementor session) {
		return false;
	}
	
	public Object resolve(Object value, SessionImplementor session, Object owner) throws ZormException {
		if ( isNotEmbedded( session ) ) {
			return value;
		}

		if ( value == null ) {
			return null;
		}
		else {
			if ( isNull( owner, session ) ) {
				return null; //EARLY EXIT!
			}

			if ( isReferenceToPrimaryKey() ) {
				return resolveIdentifier( (Serializable) value, session );
			}
			else {
				return loadByUniqueKey( getAssociatedEntityName(), uniqueKeyPropertyName, value, session );
			}
		}
	}
	
	public Object loadByUniqueKey(
			String entityName, 
			String uniqueKeyPropertyName, 
			Object key, 
			SessionImplementor session) throws ZormException {
		final SessionFactoryImplementor factory = session.getFactory();
		UniqueKeyLoadable persister = ( UniqueKeyLoadable ) factory.getEntityPersister( entityName );

		//TODO: implement caching?! proxies?!

		EntityUniqueKey euk = new EntityUniqueKey(
				entityName, 
				uniqueKeyPropertyName, 
				key, 
				getIdentifierOrUniqueKeyType( factory ),
				persister.getEntityMode(),
				session.getFactory()
		);

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		Object result = persistenceContext.getEntity( euk );
		if ( result == null ) {
			result = persister.loadByUniqueKey( uniqueKeyPropertyName, key, session );
		}
		return result == null ? null : persistenceContext.proxyFor( result );
	}
	
	protected abstract boolean isNullable();
	
	protected final Object resolveIdentifier(Serializable id, SessionImplementor session) throws ZormException {
		boolean isProxyUnwrapEnabled = unwrapProxy &&
				session.getFactory()
						.getEntityPersister( getAssociatedEntityName() )
						.isInstrumented();

		Object proxyOrEntity = session.internalLoad(
				getAssociatedEntityName(),
				id,
				eager,
				isNullable() && !isProxyUnwrapEnabled
		);

		return proxyOrEntity;
	}
	
	public final boolean isSame(Object x, Object y) {
		return x == y;
	}
	
	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws ZormException, SQLException {
		return resolve( hydrate(rs, names, session, owner), session, owner );
	}
	
	protected final Object getIdentifier(Object value, SessionImplementor session) throws ZormException {
		if ( isNotEmbedded(session) ) {
			return value;
		}

		if ( isReferenceToPrimaryKey() ) {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved( getAssociatedEntityName(), value, session ); //tolerates nulls
		}
		else if ( value == null ) {
			return null;
		}
		else {
			EntityPersister entityPersister = session.getFactory().getEntityPersister( getAssociatedEntityName() );
			Object propertyValue = entityPersister.getPropertyValue( value, uniqueKeyPropertyName );
			Type type = entityPersister.getPropertyType( uniqueKeyPropertyName );
			if ( type.isEntityType() ) {
				propertyValue = ( ( EntityType ) type ).getIdentifier( propertyValue, session );
			}

			return propertyValue;
		}
	}
	
	private boolean isNotEmbedded(SessionImplementor session) {
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		// associations (many-to-one and one-to-one) can be null...
		if ( x == null || y == null ) {
			return x == y;
		}

		EntityPersister persister = factory.getEntityPersister(associatedEntityName);
		if ( !persister.canExtractIdOutOfEntity() ) {
			return super.isEqual(x, y );
		}

		final Class mappedClass = persister.getMappedClass();
		Serializable xid;
			if ( mappedClass.isAssignableFrom( x.getClass() ) ) {
				xid = persister.getIdentifier( x);
			}
			else {
				//JPA 2 case where @IdClass contains the id and not the associated entity
				xid = (Serializable) x;
			}

		Serializable yid;
			if ( mappedClass.isAssignableFrom( y.getClass() ) ) {
				yid = persister.getIdentifier( y);
			}
			else {
				yid = (Serializable) y;
			}

		return persister.getIdentifierType()
				.isEqual(xid, yid, factory);
	}
	
	public String getLHSPropertyName() {
		return null;
	}
	
	public final Class getReturnedClass() {
		if ( returnedClass == null ) {
			returnedClass = determineAssociatedEntityClass();
		}
		return returnedClass;
	}
	
	private Class determineAssociatedEntityClass() {
		try {
			return ReflectHelper.classForName( getAssociatedEntityName() );
		}
		catch ( ClassNotFoundException cnfe ) {
			return java.util.Map.class;
		}
	}
	
	public String getRHSUniqueKeyPropertyName() {
		return uniqueKeyPropertyName;
	}
	
	public final boolean isEntityType() {
		return true;
	}
	
	public boolean isReferenceToPrimaryKey() {
		return uniqueKeyPropertyName==null;
	}
	
	Type getIdentifierType(Mapping factory) {
		return factory.getIdentifierType( getAssociatedEntityName() );
	}
	
	public boolean isAssociationType() {
		return true;
	}
	
	public final Type getIdentifierOrUniqueKeyType(Mapping factory) throws MappingException {
		if ( isReferenceToPrimaryKey() ) {
			return getIdentifierType(factory);
		}
		else {
			Type type = factory.getReferencedPropertyType( getAssociatedEntityName(), uniqueKeyPropertyName );
			if ( type.isEntityType() ) {
				type = ( ( EntityType ) type).getIdentifierOrUniqueKeyType( factory );
			}
			return type;
		}
	}
	
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters)
			throws MappingException {
				if ( isReferenceToPrimaryKey() ) { //TODO: this is a bit arbitrary, expose a switch to the user?
					return "";
				}
				else {
					return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters );
				}
			}

	public abstract boolean isOneToOne() ;
	
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) throws MappingException {
		return ( Joinable ) factory.getEntityPersister( associatedEntityName );
	}

	public String getAssociatedEntityName() {
		return associatedEntityName;
	}

	public boolean isLogicalOneToOne() {
		return isOneToOne();
	}
	
	@Override
	public int compare(Object x, Object y) {
		return 0;
	}
	
	public String getName() {
		return associatedEntityName;
	}
	
	public boolean isMutable() {
		return false;
	}
}

package com.zorm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zorm.collection.PersistentCollection;
import com.zorm.engine.CollectionKey;
import com.zorm.engine.Mapping;
import com.zorm.engine.PersistenceContext;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityMode;
import com.zorm.event.EventSource;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.meta.Size;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Joinable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.ArrayHelper;
import com.zorm.util.MarkerObject;

@SuppressWarnings("rawtypes")
public abstract class CollectionType  extends AbstractType implements AssociationType {
	private static final long serialVersionUID = -4334687538425566259L;
	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );
	public static final Object UNFETCHED_COLLECTION = new MarkerObject( "UNFETCHED COLLECTION" );

	private final TypeFactory.TypeScope typeScope;
	private final String role;
	private final String foreignKeyPropertyName;
	private final boolean isEmbeddedInXML;
	
	public abstract PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key);
	
	public CollectionType(TypeFactory.TypeScope typeScope, String role, String foreignKeyPropertyName) {
		this.typeScope = typeScope;
		this.role = role;
		this.foreignKeyPropertyName = foreignKeyPropertyName;
		this.isEmbeddedInXML = true;
	}
	
	public boolean isAssociationType() {
		return true;
	}
	
	public boolean isArrayType() {
		return false;
	}
	
	public boolean isCollectionType() {
		return true;
	}
	
	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 0;
	}

	@Override
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return null;
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return null;
	}

	@Override
	public Class getReturnedClass() {
		return null;
	}

	@Override
	public boolean isSame(Object x, Object y) throws ZormException {
		return false;
	}

	@Override
	public boolean isEqual(Object x, Object y) throws ZormException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory)
			throws ZormException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getHashCode(Object x) throws ZormException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory)
			throws ZormException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int compare(Object x, Object y) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isDirty(Object old, Object current,
			SessionImplementor session) throws ZormException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDirty(Object oldState, Object currentState,
			boolean[] checkable, SessionImplementor session)
			throws ZormException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isModified(Object dbState, Object currentState,
			boolean[] checkable, SessionImplementor session)
			throws ZormException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names,
			SessionImplementor session, Object owner) throws ZormException,
			SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String name,
			SessionImplementor session, Object owner) throws ZormException,
			SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			boolean[] settable, SessionImplementor session)
			throws ZormException, SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws ZormException, SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toLoggableString(Object value,
			SessionFactoryImplementor factory) throws ZormException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws ZormException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value, SessionImplementor session,
			Object owner) throws ZormException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object assemble(Serializable cached, SessionImplementor session,
			Object owner) throws ZormException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void beforeAssemble(Serializable cached, SessionImplementor session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object resolve(Object value, SessionImplementor session, Object owner)
			throws ZormException {
		return resolveKey( getKeyOfOwner( owner, session ), session, owner );
	}
	
	private Object resolveKey(Serializable key, SessionImplementor session, Object owner) {
		return key == null ? null : getCollection( key, session, owner );
	}
	
	public Object getCollection(Serializable key, SessionImplementor session, Object owner) {

		CollectionPersister persister = getPersister( session );
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityMode entityMode = persister.getOwnerEntityPersister().getEntityMode();

		PersistentCollection collection = persistenceContext.getLoadContexts().locateLoadingCollection( persister, key );
		
		if ( collection == null ) {
			
			collection = persistenceContext.useUnownedCollection( new CollectionKey(persister, key, entityMode) );
			
			if ( collection == null ) {
				collection = instantiate( session, persister, key );
				
				collection.setOwner(owner);
	
				persistenceContext.addUninitializedCollection( persister, collection, key );
	
				if ( initializeImmediately() ) {
					session.initializeCollection( collection, false );
				}
				else if ( !persister.isLazy() ) {
					persistenceContext.addNonLazyCollection( collection );
				}
	
				if ( hasHolder() ) {
					session.getPersistenceContext().addCollectionHolder( collection );
				}
				
			}
		}
		
		collection.setOwner(owner);

		return collection.getValue();
	}
	
	protected boolean initializeImmediately() {
		return false;
	}

	@Override
	public Object semiResolve(Object value, SessionImplementor session,
			Object owner) throws ZormException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object replace(Object original, Object target,
			SessionImplementor session, Object owner, Map copyCache)
			throws ZormException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object replace(Object original, Object target,
			SessionImplementor session, Object owner, Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws ZormException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FOREIGN_KEY_TO_PARENT;
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return foreignKeyPropertyName == null;
	}

	@Override
	public String getLHSPropertyName() {
		return foreignKeyPropertyName;
	}

	@Override
	public String getRHSUniqueKeyPropertyName() {
		return null;
	}

	@Override
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory)
			throws MappingException {
		return (Joinable) factory.getCollectionPersister( role );
	}

	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory)
			throws MappingException {
		return null;
	}

	@Override
	public String getOnCondition(String alias,
			SessionFactoryImplementor factory, Map enabledFilters)
			throws MappingException {
		return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters );
	}

	@Override
	public boolean isAlwaysDirtyChecked() {
		return true;
	}

	@Override
	public Object hydrate(ResultSet rs, String[] name, SessionImplementor session, Object owner) {
		return NOT_NULL_COLLECTION;
	}
	
	public Iterator getElementsIterator(Object collection, EventSource session) {
		return getElementsIterator(collection);
	}
	
	protected Iterator getElementsIterator(Object collection) {
		return ( (Collection) collection ).iterator();
	}

	public Iterator getElementsIterator(Object collection, SessionImplementor session) {
		return getElementsIterator(collection);
	}

	public String getRole() {
		return role;
	}

	private CollectionPersister getPersister(SessionImplementor session) {
		return session.getFactory().getCollectionPersister( role );
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Serializable getIdOfOwnerOrNull(Serializable key, SessionImplementor session) {
		Serializable ownerId = null;
		if ( foreignKeyPropertyName == null ) {
			ownerId = key;
		}
		else {
			Type keyType = getPersister( session ).getKeyType();
			EntityPersister ownerPersister = getPersister( session ).getOwnerEntityPersister();
			Class ownerMappedClass = ownerPersister.getMappedClass();
			if ( ownerMappedClass.isAssignableFrom( keyType.getReturnedClass() ) &&
					keyType.getReturnedClass().isInstance( key ) ) {
				ownerId = ownerPersister.getIdentifier( key, session );
			}
			else {
			}
		}
		return ownerId;
	}

	public boolean hasHolder() {
		return false;
	}

public Serializable getKeyOfOwner(Object owner, SessionImplementor session) {
		
		EntityEntry entityEntry = session.getPersistenceContext().getEntry( owner );
		if ( entityEntry == null ) return null; 
									 
		
		if ( foreignKeyPropertyName == null ) {
			return entityEntry.getId();
		}
		else {
			Object id;
			if ( entityEntry.getLoadedState() != null ) {
				id = entityEntry.getLoadedValue( foreignKeyPropertyName );
			}
			else {
				id = entityEntry.getPersister().getPropertyValue( owner, foreignKeyPropertyName );
			}

			Type keyType = getPersister( session ).getKeyType();
			if ( !keyType.getReturnedClass().isInstance( id ) ) {
				id = (Serializable) keyType.semiResolve(
						entityEntry.getLoadedValue( foreignKeyPropertyName ),
						session,
						owner 
					);
			}

			return (Serializable) id;
		}
	}

   public abstract PersistentCollection wrap(SessionImplementor session, Object collection);

   public abstract Object instantiate(int anticipatedSize);

}

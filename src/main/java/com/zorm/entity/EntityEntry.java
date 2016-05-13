package com.zorm.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.zorm.LockMode;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.Status;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.UniqueKeyLoadable;
import com.zorm.session.Session;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public final class EntityEntry implements Serializable{
	private static final long serialVersionUID = 5415219042819723381L;
	private LockMode lockMode;
	private Status status;
	private Status previousStatus;
	private final Serializable id;
	private Object[] loadedState;
	private Object[] deletedState;
	private boolean existsInDatabase;
	private Object version;
	private transient EntityPersister persister; // for convenience to save some lookups
	private final EntityMode entityMode;
	private final String tenantId;
	private final String entityName;
	private transient EntityKey cachedEntityKey; // cached EntityKey (lazy-initialized)
	private boolean isBeingReplicated;
	private boolean loadedWithLazyPropertiesUnfetched; //NOTE: this is not updated when properties are fetched lazily!
	private final transient Object rowId;
	private final transient PersistenceContext persistenceContext;

	public EntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final EntityMode entityMode,
			final String tenantId,
			final boolean disableVersionIncrement,
			final boolean lazyPropertiesAreUnfetched,
			final PersistenceContext persistenceContext) {
		this.status = status;
		this.previousStatus = null;
		// only retain loaded state if the status is not Status.READ_ONLY
		if ( status != Status.READ_ONLY ) {
			this.loadedState = loadedState;
		}
		this.id=id;
		this.rowId=rowId;
		this.existsInDatabase=existsInDatabase;
		this.version=version;
		this.lockMode=lockMode;
		this.isBeingReplicated=disableVersionIncrement;
		this.loadedWithLazyPropertiesUnfetched = lazyPropertiesAreUnfetched;
		this.persister=persister;
		this.entityMode = entityMode;
		this.tenantId = tenantId;
		this.entityName = persister == null ? null : persister.getEntityName();
		this.persistenceContext = persistenceContext;
	}

	private EntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Serializable id,
			final EntityMode entityMode,
			final String tenantId,
			final Status status,
			final Status previousStatus,
			final Object[] loadedState,
	        final Object[] deletedState,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final boolean isBeingReplicated,
			final boolean loadedWithLazyPropertiesUnfetched,
			final PersistenceContext persistenceContext) {
		this.entityName = entityName;
		this.persister = ( factory == null ? null : factory.getEntityPersister( entityName ) );
		this.id = id;
		this.entityMode = entityMode;
		this.tenantId = tenantId;
		this.status = status;
		this.previousStatus = previousStatus;
		this.loadedState = loadedState;
		this.deletedState = deletedState;
		this.version = version;
		this.lockMode = lockMode;
		this.existsInDatabase = existsInDatabase;
		this.isBeingReplicated = isBeingReplicated;
		this.loadedWithLazyPropertiesUnfetched = loadedWithLazyPropertiesUnfetched;
		this.rowId = null; // this is equivalent to the old behavior...
		this.persistenceContext = persistenceContext;
	}

	public Serializable getId() {
		return id;
	}

	public Status getStatus() {
		return status;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	public boolean requiresDirtyCheck(Object entity) {
		return isModifiableEntity()
				&& ( ! isUnequivocallyNonDirty( entity ) );
	}

	private boolean isUnequivocallyNonDirty(Object entity) {
		return false;
	}

	public boolean isModifiableEntity() {
		return getPersister().isMutable()
				&& status != Status.READ_ONLY
				&& !(status==Status.DELETED && previousStatus == Status.READ_ONLY);
	}

	public Object[] getLoadedState() {
		return loadedState;
	}

	public void postInsert(Object[] state) {
		existsInDatabase = true;
	}

	public void postUpdate(Object entity, Object[] updatedState, Object nextVersion) {
		this.loadedState = updatedState;
		setLockMode( LockMode.WRITE );
		
		persistenceContext.getSession()
		.getFactory()
		.getCustomEntityDirtinessStrategy()
		.resetDirty( entity, getPersister(), (Session) persistenceContext.getSession() );
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	public Object[] getDeletedState() {
		return deletedState;
	}

	public boolean isBeingReplicated() {
		return isBeingReplicated;
	}

	public Object getVersion() {
		return version;
	}

	public void setDeletedState(Object[] deletedState) {
		this.deletedState = deletedState;
	}

	public void setStatus(Status status) {
		if(status == Status.READ_ONLY){
			loadedState = null;
		}
		if(this.status != status){
			this.previousStatus = this.status;
			this.status = status;
		}
	}

	public void postDelete() {
		previousStatus = status;
		status = Status.GONE;
		existsInDatabase = false;
	}

	public EntityKey getEntityKey() {
		if ( cachedEntityKey == null ) {
			if ( getId() == null ) {
				throw new IllegalStateException( "cannot generate an EntityKey when id is null.");
			}
			cachedEntityKey = new EntityKey( getId(), getPersister(), tenantId );
		}
		return cachedEntityKey;
	}

	public boolean isLoadedWithLazyPropertiesUnfetched() {
		return loadedWithLazyPropertiesUnfetched;
	}

	public Object getRowId() {
		return rowId;
	}

	public Object getLoadedValue(String propertyName) {
		if ( loadedState == null ) {
			return null;
		}
		else {
			int propertyIndex = ( (UniqueKeyLoadable) persister )
					.getPropertyIndex( propertyName );
			return loadedState[propertyIndex];
		}
	}
	
	public boolean isExistsInDatabase() {
		return existsInDatabase;
	}

	public boolean isNullifiable(boolean earlyInsert, SessionImplementor session) {
		return getStatus() == Status.SAVING || (
				earlyInsert ?
						!isExistsInDatabase() :
						session.getPersistenceContext().getNullifiableEntityKeys()
							.contains( getEntityKey() )
				);
	}

	public static EntityEntry deserialize(
			ObjectInputStream ois,
	        PersistenceContext persistenceContext) throws IOException, ClassNotFoundException {
		String previousStatusString;
		return new EntityEntry(
				// this complexity comes from non-flushed changes, should really look at how that reattaches entries
				( persistenceContext.getSession() == null ? null : persistenceContext.getSession().getFactory() ),
		        (String) ois.readObject(),
				( Serializable ) ois.readObject(),
	            EntityMode.parse( (String) ois.readObject() ),
				(String) ois.readObject(),
				Status.valueOf( (String) ois.readObject() ),
				( ( previousStatusString = ( String ) ois.readObject() ).length() == 0 ?
							null :
							Status.valueOf( previousStatusString )
				),
	            ( Object[] ) ois.readObject(),
	            ( Object[] ) ois.readObject(),
	            ois.readObject(),
	            LockMode.valueOf( (String) ois.readObject() ),
	            ois.readBoolean(),
	            ois.readBoolean(),
	            ois.readBoolean(),
				persistenceContext
		);
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( entityName );
		oos.writeObject( id );
		oos.writeObject( entityMode.toString() );
		oos.writeObject( tenantId );
		oos.writeObject( status.name() );
		oos.writeObject( (previousStatus == null ? "" : previousStatus.name()) );
		oos.writeObject( loadedState );
		oos.writeObject( deletedState );
		oos.writeObject( version );
		oos.writeObject( lockMode.toString() );
		oos.writeBoolean( existsInDatabase );
		oos.writeBoolean( isBeingReplicated );
		oos.writeBoolean( loadedWithLazyPropertiesUnfetched );
	}

}

package com.zorm.session;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.ConnectionReleaseMode;
import com.zorm.EmptyInterceptor;
import com.zorm.EntityNameResolver;
import com.zorm.FlushMode;
import com.zorm.Interceptor;
import com.zorm.LockOptions;
import com.zorm.collection.PersistentCollection;
import com.zorm.engine.ActionQueue;
import com.zorm.engine.ConnectionObserverStatsBridge;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.engine.NonFlushedChanges;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.StatefulPersistenceContext;
import com.zorm.engine.transaction.spi.TransactionImplementor;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.event.AutoFlushEvent;
import com.zorm.event.AutoFlushEventListener;
import com.zorm.event.DeleteEvent;
import com.zorm.event.DeleteEventListener;
import com.zorm.event.EventListenerGroup;
import com.zorm.event.EventListenerRegistry;
import com.zorm.event.EventSource;
import com.zorm.event.EventType;
import com.zorm.event.FlushEvent;
import com.zorm.event.FlushEventListener;
import com.zorm.event.LoadEvent;
import com.zorm.event.LoadEventListener;
import com.zorm.event.LoadEventListener.LoadType;
import com.zorm.event.SaveOrUpdateEvent;
import com.zorm.event.SaveOrUpdateEventListener;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.SessionException;
import com.zorm.exception.TransientObjectException;
import com.zorm.exception.UnresolvableObjectException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.query.CustomQuery;
import com.zorm.query.NativeSQLQuerySpecification;
import com.zorm.query.Query;
import com.zorm.query.QueryParameters;
import com.zorm.query.QueryPlan;
import com.zorm.query.ScrollableResults;
import com.zorm.transaction.Transaction;
import com.zorm.transaction.TransactionCoordinator;
import com.zorm.transaction.TransactionCoordinatorImpl;
import com.zorm.transaction.TransactionObserver;
import com.zorm.type.Type;

@SuppressWarnings("rawtypes")
public final class SessionImpl extends AbstractSessionImpl  implements EventSource{

	private static final long serialVersionUID = -6439962938412032677L;

	private final static Log log = LogFactory.getLog(SessionImpl.class);
	
	private transient long timestamp;
	private transient SessionOwner sessionOwner;
	private transient Interceptor interceptor;
	private transient ConnectionReleaseMode connectionReleaseMode;
	private transient boolean autoClear; //for EJB3
	private transient boolean autoJoinTransactions = true;
	private transient boolean flushBeforeCompletionEnabled;
	private transient boolean autoCloseSessionEnabled;
	private transient int dontFlushFromFind = 0;
	private final transient boolean isTransactionCoordinatorShared ;
	private transient TransactionObserver transactionObserver;
	private transient EntityNameResolver entityNameResolver = new CoordinatingEntityNameResolver();
	private transient FlushMode flushMode = FlushMode.AUTO;
	private transient ActionQueue actionQueue;
	private transient StatefulPersistenceContext persistenceContext;
	private transient TransactionCoordinatorImpl transactionCoordinator;
	private transient LoadQueryInfluencers loadQueryInfluencers;
	
	SessionImpl(
			final Connection connection,
			final SessionFactoryImpl factory,
			final SessionOwner sessionOwner,
			final TransactionCoordinatorImpl transactionCoordinator,
			final boolean autoJoinTransactions,
			final long timestamp,
			final Interceptor interceptor,
			final boolean flushBeforeCompletionEnabled,
			final boolean autoCloseSessionEnabled,
			final ConnectionReleaseMode connectionReleaseMode,
			final String tenantIdentifier) {
	super( factory, tenantIdentifier );
	this.timestamp = timestamp;
	this.sessionOwner = sessionOwner;
	//EmptyInterceptor
	this.interceptor = interceptor == null ? EmptyInterceptor.INSTANCE : interceptor;
	this.actionQueue = new ActionQueue( this );
	this.persistenceContext = new StatefulPersistenceContext( this );

	this.autoCloseSessionEnabled = autoCloseSessionEnabled;
	this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;

	if ( transactionCoordinator == null ) {
		this.isTransactionCoordinatorShared = false;
		this.connectionReleaseMode = connectionReleaseMode;
		this.autoJoinTransactions = autoJoinTransactions;

		this.transactionCoordinator = new TransactionCoordinatorImpl( connection, this );
		this.transactionCoordinator.getJdbcCoordinator().getLogicalConnection().addObserver(
				new ConnectionObserverStatsBridge( factory )
		);
	}
	else {
		this.isTransactionCoordinatorShared = true;
	}

	loadQueryInfluencers = new LoadQueryInfluencers( factory );
  }
	
	@Override
	public Query createQuery(String queryString) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return super.createQuery(queryString);
	}
	
	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}
	
	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}
	
	@Override
	public boolean shouldAutoJoinTransaction() {
		return autoJoinTransactions;
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}
	
	@Override
	public List listCustomQuery(CustomQuery customQuery,
			QueryParameters queryParameters) throws ZormException {
		return null;
	}
	
	@Override
	public Transaction beginTransaction() throws ZormException{
		errorIfClosed();
		Transaction result = getTransaction();
		result.begin();
		return result;
	}
	
	@Override
	public void afterTransactionBegin(TransactionImplementor jdbcTransaction) {
       errorIfClosed();
       interceptor.afterTransactionBegin(jdbcTransaction);
	}
	
	public Transaction getTransaction() throws ZormException {
		errorIfClosed();
		return transactionCoordinator.getTransaction();
	}
	
	private class CoordinatingEntityNameResolver implements EntityNameResolver {
		//获取实体名
		public String resolveEntityName(Object entity) {
			String entityName = interceptor.getEntityName( entity );
			if ( entityName != null ) {
				return entityName;
			}

			for ( EntityNameResolver resolver : factory.iterateEntityNameResolvers() ) {
				entityName = resolver.resolveEntityName( entity );
				if ( entityName != null ) {
					break;
				}
			}

			if ( entityName != null ) {
				return entityName;
			}

			return entity.getClass().getName();
		}
	}

	@Override
	public Interceptor getInterceptor() {
		checkTransactionSynchStatus();
		return interceptor;
	}

	@Override
	public void setAutoClear(boolean enabled) {
		
	}

	@Override
	public void disableTransactionAutoJoin() {
		
	}

	@Override
	public boolean isTransactionInProgress() {
		checkTransactionSynchStatus();
		return !isClosed() && transactionCoordinator.isTransactionInProgress();
	}

	@Override
	public Object internalLoad(String entityName, Serializable id,
			boolean eager, boolean nullable) throws ZormException {
		LoadEventListener.LoadType type = nullable
				? LoadEventListener.INTERNAL_LOAD_NULLABLE
				: eager
						? LoadEventListener.INTERNAL_LOAD_EAGER
						: LoadEventListener.INTERNAL_LOAD_LAZY;
		LoadEvent event = new LoadEvent(id, entityName, true, this);
		fireLoad(event, type);
		if ( !nullable ) {
			UnresolvableObjectException.throwIfNull( event.getResult(), id, entityName );
		}
		return event.getResult();
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id)
			throws ZormException {
		return null;
	}

	@Override
	public long getTimestamp() {
		return 0;
	}

	protected boolean autoFlushIfRequired(Set querySpaces) throws ZormException {
		errorIfClosed();
		if ( ! isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return false;
		}
		AutoFlushEvent event = new AutoFlushEvent( querySpaces, this );
		for ( AutoFlushEventListener listener : listeners( EventType.AUTO_FLUSH ) ) {
			listener.onAutoFlush( event );
		}
		return event.isFlushRequired();
	}
	
	@Override
	public List list(String query, QueryParameters queryParameters)
			throws ZormException {
		errorIfClosed();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		QueryPlan plan = getQueryPlan( query, false );
		autoFlushIfRequired( plan.getQuerySpaces() );

		List results = Collections.EMPTY_LIST;
		boolean success = false;

		dontFlushFromFind++;   //stops flush being called multiple times if this method is recursively called
		try {
			results = plan.performList( queryParameters, this );
			success = true;
		}
		finally {
			dontFlushFromFind--;
			afterOperation(success);
		}
		return results;
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters)
			throws ZormException {
		return null;
	}

	@Override
	public ScrollableResults scroll(String query,
			QueryParameters queryParameters) throws ZormException {
		return null;
	}

	@Override
	public List listFilter(Object collection, String filter,
			QueryParameters queryParameters) throws ZormException {
		return null;
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter,
			QueryParameters queryParameters) throws ZormException {
		return null;
	}

	@Override
	public EntityPersister getEntityPersister(final String entityName,final Object object)
			throws ZormException {
		errorIfClosed();
		if(entityName==null){
			return factory.getEntityPersister(guessEntityName(object));
		}
		else{
			try {
				return factory.getEntityPersister( entityName ).getSubclassEntityPersister( object, getFactory() );
			}
			catch( ZormException e ) {
				try {
					return getEntityPersister( null, object );
				}
				catch( ZormException e2 ) {
					throw e;
				}
			}
		}	
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws ZormException {
		errorIfClosed();
		final Object result = persistenceContext.getEntity(key);
		if ( result == null ) {
			final Object newObject = interceptor.getEntity( key.getEntityName(), key.getIdentifier() );
			if ( newObject != null ) {
				//lock( newObject, LockMode.NONE );
			}
			return newObject;
		}
		else {
			return result;
		}
	}

	@Override
	public Serializable getContextEntityIdentifier(Object object) {
		errorIfClosed();
		EntityEntry entry = persistenceContext.getEntry(object);
		return entry != null ? entry.getId() : null;
		
	}

	@Override
	public String bestGuessEntityName(Object object) {
		return null;
	}

	@Override
	public String guessEntityName(Object entity) throws ZormException {
		errorIfClosed();
		return entityNameResolver.resolveEntityName(entity);
	}

	@Override
	public Object instantiate(String entityName, Serializable id)
			throws ZormException {
		return instantiate( factory.getEntityPersister( entityName ), id );
	}
	
	public Object instantiate(EntityPersister persister, Serializable id) throws ZormException {
		errorIfClosed();
		checkTransactionSynchStatus();
		Object result = interceptor.instantiate( persister.getEntityName(), persister.getEntityMetamodel().getEntityMode(), id );
		if ( result == null ) {
			result = persister.instantiate( id, this );
		}
		return result;
	}

	@Override
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery,
			QueryParameters queryParameters) throws ZormException {
		return null;
	}

	@Override
	public Object getFilterParameterValue(String filterParameterName) {
		return null;
	}

	@Override
	public Type getFilterParameterType(String filterParameterName) {
		return null;
	}

	@Override
	public Map getEnabledFilters() {
		errorIfClosed();
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getEnabledFilters();
	}

	@Override
	public int getDontFlushFromFind() {
		return 0;
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		errorIfClosed();
		checkTransactionSynchStatus();
		return persistenceContext;
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters)
			throws ZormException {
		errorIfClosed();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		QueryPlan plan = getQueryPlan( query, false );
		autoFlushIfRequired( plan.getQuerySpaces() );
		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation(success);
		}
		return result;
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification specification,
			QueryParameters queryParameters) throws ZormException {
		return 0;
	}

	@Override
	public NonFlushedChanges getNonFlushedChanges() throws ZormException {
		return null;
	}

	@Override
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges)
			throws ZormException {
		
	}

	@Override
	public boolean isOpen() {
		checkTransactionSynchStatus();
		return !isClosed();
	}

	@Override
	public boolean isConnected() {
		checkTransactionSynchStatus();
		return !isClosed();
	}

	@Override
	public FlushMode getFlushMode() {
		checkTransactionSynchStatus();
		return flushMode;
	}

	@Override
	public void setFlushMode(FlushMode fm) {
	}

	@Override
	public Connection connection() {
		return null;
	}

	@Override
	public void flush() {
		errorIfClosed();
		checkTransactionSynchStatus();
		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new ZormException("Flush during cascade is dangerous");
		}
		FlushEvent flushEvent = new FlushEvent(this);
		//调用监听器的监听方法
		for ( FlushEventListener listener : listeners( EventType.FLUSH ) ) {
			listener.onFlush( flushEvent );
		}
	}

	@Override
	public boolean isEventSource() {
		checkTransactionSynchStatus();
		return true;
	}

	@Override
	public void afterScrollOperation() {
		
	}

	@Override
	public String getFetchProfile() {
		return null;
	}

	@Override
	public void setFetchProfile(String name) {
		
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		errorIfClosed();
		return transactionCoordinator;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}
	
	//
	@Override
	public Object find(Class clazz, Serializable id) {
		return byId(clazz).load(id);
	}

	
	private IdentifierLoadAccessImpl byId(Class clazz) {
		return new IdentifierLoadAccessImpl(clazz);
	}
	
	private EntityPersister locateEntityPersister(String entityName) {
		final EntityPersister entityPersister = factory.getEntityPersister( entityName );
		if ( entityPersister == null ) {
			throw new ZormException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}
	
	private class IdentifierLoadAccessImpl implements IdentifierLoadAccess{
		private final EntityPersister entityPersister;
		private LockOptions lockOptions;
		
		private IdentifierLoadAccessImpl(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;
		}
		
		private IdentifierLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}
		
		private IdentifierLoadAccessImpl(Class entityClass) {
			this( entityClass.getName() );
		}
		
		@Override
		public final Object load(Serializable id) {
			if ( this.lockOptions != null ) {
				LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, SessionImpl.this );
				fireLoad( event, LoadEventListener.GET );
				return event.getResult();
			}

			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, SessionImpl.this );
			boolean success = false;
			try {
				fireLoad( event, LoadEventListener.GET );
				success = true;
				return event.getResult();
			}
			finally {
				afterOperation( success );
			}
		}
		
	}
	
	public void afterOperation(boolean success) {
		if ( ! transactionCoordinator.isTransactionInProgress() ) {
			//transactionCoordinator.afterNonTransactionalQuery( success );
		}
	}

	public void fireLoad(LoadEvent event, LoadType loadType) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for(LoadEventListener listener : listeners(EventType.LOAD)){
			listener.onLoad(event, loadType);
		}
	}
	
	//save() operation: 持久化对象
	public Serializable save(Object object) throws ZormException{
		return save(null,object);
	}


	public Serializable save(String entityName, Object object) {
		return fireSave(new SaveOrUpdateEvent(entityName,object,this));
	}

	private Serializable fireSave(SaveOrUpdateEvent event) {
        errorIfClosed();
        //检查事务状态
        checkTransactionSynchStatus();
        checkNoUnresolvedActionsBeforeOperation();
        for(SaveOrUpdateEventListener listener : listeners(EventType.SAVE)){
        	listener.onSaveOrUpdate(event);
        }
		return event.getRequestedId();
	}
	
	//删除操作
	@Override
	public void delete(Object object) throws ZormException{
		fireDelete(new DeleteEvent(object,this));
	}
	
	public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled, Set transientEntities) throws ZormException {
		fireDelete( new DeleteEvent( entityName, object, isCascadeDeleteEnabled, this ), transientEntities );
	}

	private void fireDelete(DeleteEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for(DeleteEventListener listener : listeners(EventType.DELETE)){
			listener.onDelete(event);
		}
	}
	
	private void fireDelete(DeleteEvent event, Set transientEntities) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( DeleteEventListener listener : listeners( EventType.DELETE ) ) {
			listener.onDelete( event, transientEntities );
		}
	}
	
	//更新操作
	@Override
	public void update(Object object) {
		update(null,object);
	}
	
	@Override
	public void update(String entityName, Object object) {
		fireUpdate(new SaveOrUpdateEvent(entityName, object,this));
	}

	private void fireUpdate(SaveOrUpdateEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.UPDATE ) ) {
			listener.onSaveOrUpdate( event );
		}
	}
	
	public void saveOrUpdate(String entityName, Object obj) throws ZormException {
		fireSaveOrUpdate( new SaveOrUpdateEvent( entityName, obj, this ) );
	}
	
	private void fireSaveOrUpdate(SaveOrUpdateEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.SAVE_UPDATE ) ) {
			listener.onSaveOrUpdate( event );
		}
	}

	private <T> Iterable<T> listeners(EventType<T> type) {
		return eventListenerGroup(type).listeners();
	}
	
	private <T> EventListenerGroup<T> eventListenerGroup(EventType<T> type) {
		return factory.getServiceRegistry().getService( EventListenerRegistry.class ).getEventListenerGroup( type );
	}

	private void checkNoUnresolvedActionsBeforeOperation() {
		if(persistenceContext.getCascadeLevel() == 0 && actionQueue.hasUnresolvedEntityInsertActions()){
			throw new IllegalStateException( "There are delayed insert actions before operation as cascade level 0." );
		}
	}

	private void checkTransactionSynchStatus() {
		if(!isClosed()){
			transactionCoordinator.pulse();
		}
	}

	@Override
	public ActionQueue getActionQueue() {
		errorIfClosed();
		checkTransactionSynchStatus();
		return actionQueue;
	}

	@Override
	public boolean isFlushModeNever() {
		return FlushMode.isManualFlushMode( getFlushMode() );
	}

	@Override
	public void managedFlush() {
		if(isClosed()){
			log.info("Skipping auto-flush due to session closed");
			return;
		}
		log.info("Automatically flushing session");
		flush();
	}

	@Override
	public String onPrepareStatement(String sql) {
		errorIfClosed();
		sql = interceptor.onPrepareStatement( sql );
		if ( sql == null || sql.length() == 0 ) {
			throw new AssertionFailure( "Interceptor.onPrepareStatement() returned null or empty string." );
		}
		return sql;
	}

	@Override
	public void beforeTransactionCompletion(
			TransactionImplementor jdbcTransaction) {
		actionQueue.beforeTransactionCompletion();
		try{
			interceptor.beforeTransactionCompletion( jdbcTransaction );
		}
		catch(Throwable t){}
	}

	@Override
	public void afterTransactionCompletion(
			TransactionImplementor jdbcTransaction, boolean successful) {
		persistenceContext.afterTransactionCompletion();
		actionQueue.afterTransactionCompletion( successful );
		if ( jdbcTransaction != null ) {
			try {
				interceptor.afterTransactionCompletion( jdbcTransaction );
			}
			catch (Throwable t) {
			}
		}
		if ( autoClear ) {
			//internalClear();
		}
	}

	@Override
	public boolean shouldAutoClose() {
		return false;
	}

	@Override
	public void managedClose() {
	}

	@Override
	public Serializable getIdentifier(Object entity) {
		EntityEntry entry = persistenceContext.getEntry(entity);
		if(entry==null){
			throw new TransientObjectException( "The instance was not associated with this session" );
		}
		return entry.getId();
	}

	@Override
	public void initializeCollection(PersistentCollection collection,boolean writing) {
		
	}
	
	public Connection close() {
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed" );
		}

		try {
			if ( !isTransactionCoordinatorShared ) {
				return transactionCoordinator.close();
			}
			else {
				if ( getActionQueue().hasAfterTransactionActions() ){
				}
				else {
					transactionCoordinator.removeObserver( transactionObserver );
				}
				return null;
			}
		}
		finally {
			setClosed();
			cleanup();
		}
	}
	
	private void cleanup() {
		persistenceContext.clear();
	}
}

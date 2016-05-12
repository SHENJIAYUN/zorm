package com.zorm.session;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zorm.FlushMode;
import com.zorm.Interceptor;
import com.zorm.collection.PersistentCollection;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.engine.NonFlushedChanges;
import com.zorm.engine.PersistenceContext;
import com.zorm.entity.EntityKey;
import com.zorm.exception.ZormException;
import com.zorm.jdbc.JdbcConnectionAccess;
import com.zorm.jdbc.LobCreationContext;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.query.CustomQuery;
import com.zorm.query.NativeSQLQuerySpecification;
import com.zorm.query.Query;
import com.zorm.query.QueryParameters;
import com.zorm.query.ScrollableResults;
import com.zorm.transaction.TransactionCoordinator;
import com.zorm.type.Type;

public interface SessionImplementor extends Serializable, LobCreationContext {
	/**
	 * Match te method on {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession}
	 *
	 * @return The tenant identifier of this session
	 */
	public String getTenantIdentifier();

	/**
	 * Provides access to JDBC connections
	 *
	 * @return The contract for accessing JDBC connections.
	 */
	public JdbcConnectionAccess getJdbcConnectionAccess();

	/**
	 * Hide the changing requirements of entity key creation
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 *
	 * @return The entity key
	 */
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister);

	/**
	 * Retrieves the interceptor currently in use by this event source.
	 *
	 * @return The interceptor.
	 */
	public Interceptor getInterceptor();

	/**
	 * Enable/disable automatic cache clearing from after transaction
	 * completion (for EJB3)
	 */
	public void setAutoClear(boolean enabled);

	/**
	 * Disable automatic transaction joining.  The really only has any effect for CMT transactions.  The default
	 * Hibernate behavior is to auto join any active JTA transaction (register {@link javax.transaction.Synchronization}).
	 * JPA however defines an explicit join transaction operation.
	 *
	 * See javax.persistence.EntityManager#joinTransaction
	 */
	public void disableTransactionAutoJoin();

	/**
	 * Does this <tt>Session</tt> have an active Hibernate transaction
	 * or is there a JTA transaction in progress?
	 */
	public boolean isTransactionInProgress();

	/**
	 * Load an instance without checking if it was deleted.
	 *
	 * When <tt>nullable</tt> is disabled this method may create a new proxy or
	 * return an existing proxy; if it does not exist, throw an exception.
	 *
	 * When <tt>nullable</tt> is enabled, the method does not create new proxies
	 * (but might return an existing proxy); if it does not exist, return
	 * <tt>null</tt>.
	 *
	 * When <tt>eager</tt> is enabled, the object is eagerly fetched
	 */
	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
	throws ZormException;

	/**
	 * Load an instance immediately. This method is only called when lazily initializing a proxy.
	 * Do not return the proxy.
	 */
	public Object immediateLoad(String entityName, Serializable id) throws ZormException;

	/**
	 * System time before the start of the transaction
	 */
	public long getTimestamp();
	/**
	 * Get the creating <tt>SessionFactoryImplementor</tt>
	 */
	public SessionFactoryImplementor getFactory();

	/**
	 * Execute a <tt>find()</tt> query
	 */
	public List list(String query, QueryParameters queryParameters) throws ZormException;
	/**
	 * Execute an <tt>iterate()</tt> query
	 */
	public Iterator iterate(String query, QueryParameters queryParameters) throws ZormException;
	/**
	 * Execute a <tt>scroll()</tt> query
	 */
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws ZormException;

	/**
	 * Execute a filter
	 */
	public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws ZormException;
	/**
	 * Iterate a filter
	 */
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws ZormException;

	/**
	 * Get the <tt>EntityPersister</tt> for any instance
	 * @param entityName optional entity name
	 * @param object the entity instance
	 */
	public EntityPersister getEntityPersister(String entityName, Object object) throws ZormException;

	/**
	 * Get the entity instance associated with the given <tt>Key</tt>,
	 * calling the Interceptor if necessary
	 */
	public Object getEntityUsingInterceptor(EntityKey key) throws ZormException;

	/**
	 * Return the identifier of the persistent object, or null if
	 * not associated with the session
	 */
	public Serializable getContextEntityIdentifier(Object object);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	public String bestGuessEntityName(Object object);

	/**
	 * The guessed entity name for an entity not in an association
	 */
	public String guessEntityName(Object entity) throws ZormException;

	/**
	 * Instantiate the entity class, initializing with the given identifier
	 */
	public Object instantiate(String entityName, Serializable id) throws ZormException;

	/**
	 * Execute an SQL Query
	 */
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
	throws ZormException;

	/**
	 * Execute an SQL Query
	 */
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
	throws ZormException;

	/**
	 * Execute a native SQL query, and return the results as a fully built list.
	 *
	 * @param spec The specification of the native SQL query to execute.
	 * @param queryParameters The parameters by which to perform the execution.
	 * @return The result list.
	 * @throws HibernateException
	 */
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
	throws ZormException;

	/**
	 * Execute a native SQL query, and return the results as a scrollable result.
	 *
	 * @param spec The specification of the native SQL query to execute.
	 * @param queryParameters The parameters by which to perform the execution.
	 * @return The resulting scrollable result.
	 * @throws HibernateException
	 */
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
	throws ZormException;

	/**
	 * Retreive the currently set value for a filter parameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 * @return The filter parameter value.
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public Object getFilterParameterValue(String filterParameterName);

	/**
	 * Retreive the type for a given filter parrameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 * @return The filter param type
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public Type getFilterParameterType(String filterParameterName);

	/**
	 * Return the currently enabled filters.  The filter map is keyed by filter
	 * name, with values corresponding to the {@link org.hibernate.internal.FilterImpl}
	 * instance.
	 * @return The currently enabled filters.
	 */
    public Map getEnabledFilters();

	public int getDontFlushFromFind();


	/**
	 * Get the persistence context for this session
	 */
	public PersistenceContext getPersistenceContext();

	/**
	 * Execute a HQL update or delete query
	 */
	int executeUpdate(String query, QueryParameters queryParameters) throws ZormException;

	/**
	 * Execute a native SQL update or delete query
	 */
	int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters) throws ZormException;


	/**
	 * Return changes to this session that have not been flushed yet.
	 *
	 * @return The non-flushed changes.
	 */
	public NonFlushedChanges getNonFlushedChanges() throws ZormException;

	/**
	 * Apply non-flushed changes from a different session to this session. It is assumed
	 * that this SessionImpl is "clean" (e.g., has no non-flushed changes, no cached entities,
	 * no cached collections, no queued actions). The specified NonFlushedChanges object cannot
	 * be bound to any session.
	 * <p/>
	 * @param nonFlushedChanges the non-flushed changes
	 */
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws ZormException;
	public boolean isOpen();
	public boolean isConnected();
	public FlushMode getFlushMode();
	public void setFlushMode(FlushMode fm);
	public Connection connection();
	public void flush();

	/**
	 * Get a Query instance for a named query or named native SQL query
	 */
	public Query getNamedQuery(String name);
	/**
	 * Get a Query instance for a named native SQL query
	 */
	public Query getNamedSQLQuery(String name);

	public boolean isEventSource();

	public void afterScrollOperation();

	/**
	 * Get the <i>internal</i> fetch profile currently associated with this session.
	 *
	 * @return The current internal fetch profile, or null if none currently associated.
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public String getFetchProfile();

	/**
	 * Set the current <i>internal</i> fetch profile for this session.
	 *
	 * @param name The internal fetch profile name to use
	 * @deprecated use {@link #getLoadQueryInfluencers} instead
	 */
	@Deprecated
    public void setFetchProfile(String name);

	public TransactionCoordinator getTransactionCoordinator();

	public boolean isClosed();

	public LoadQueryInfluencers getLoadQueryInfluencers();

	public void initializeCollection(PersistentCollection collection, boolean writing);
}


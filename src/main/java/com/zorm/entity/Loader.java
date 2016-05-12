package com.zorm.entity;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.collection.PersistentCollection;
import com.zorm.dialect.Dialect;
import com.zorm.dialect.LimitHandler;
import com.zorm.dialect.NoopLimitHandler;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.RowSelection;
import com.zorm.engine.TwoPhaseLoad;
import com.zorm.engine.TypedValue;
import com.zorm.event.EventSource;
import com.zorm.event.PostLoadEvent;
import com.zorm.event.PreLoadEvent;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.QueryException;
import com.zorm.exception.WrongClassException;
import com.zorm.exception.ZormException;
import com.zorm.jdbc.ScrollMode;
import com.zorm.loader.CollectionAliases;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Loadable;
import com.zorm.query.QueryParameters;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.AssociationType;
import com.zorm.type.EntityType;
import com.zorm.type.Type;
import com.zorm.util.LimitHelper;

public abstract class Loader {
	private final SessionFactoryImplementor factory;

	public Loader(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	@SuppressWarnings("rawtypes")
	protected final List loadEntity(final SessionImplementor session,
			final Object id, final Type identifierType,
			final Object optionalObject, final String optionalEntityName,
			final Serializable optionalIdentifier,
			final EntityPersister persister, LockOptions lockOptions) {
		List result;
		try {
			QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes(new Type[] { identifierType });
			qp.setPositionalParameterValues(new Object[] { id });
			qp.setOptionalObject(optionalObject);
			qp.setOptionalEntityName(optionalEntityName);
			qp.setOptionalId(optionalIdentifier);
			qp.setLockOptions(lockOptions);
			result = doQueryAndInitializeNonLazyCollections(session, qp, false);
		} catch (SQLException sqle) {
			throw new ZormException();
		}
		return result;
	}

	private List doQueryAndInitializeNonLazyCollections(
			final SessionImplementor session,
			final QueryParameters queryParameters, final boolean returnProxies)
			throws SQLException {
		return doQueryAndInitializeNonLazyCollections(session, queryParameters,
				returnProxies, null);
	}

	@SuppressWarnings("rawtypes")
	private List doQueryAndInitializeNonLazyCollections(
			final SessionImplementor session,
			final QueryParameters queryParameters, final boolean returnProxies,
			final ResultTransformer forcedResultTransformer)
			throws SQLException {
		final PersistenceContext persistenceContext = session
				.getPersistenceContext();
		boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		if (queryParameters.isReadOnlyInitialized()) {
			persistenceContext.setDefaultReadOnly(queryParameters.isReadOnly());
		} else {
			queryParameters.setReadOnly(persistenceContext.isDefaultReadOnly());
		}
		persistenceContext.beforeLoad();
		List result;
		try {
			try {
				result = doQuery(session, queryParameters, returnProxies,
						forcedResultTransformer);
			} finally {
				persistenceContext.afterLoad();
			}
		} finally {
			persistenceContext.setDefaultReadOnly(defaultReadOnlyOrig);
		}
		return result;
	}

	protected static interface AfterLoadAction {
		public void afterLoad(SessionImplementor session, Object entity,
				Loadable persister);
	}

	protected abstract String getSQLString();

	protected ResultSet executeQueryStatement(
			final QueryParameters queryParameters, final boolean scroll,
			List<AfterLoadAction> afterLoadActions,
			final SessionImplementor session) throws SQLException {
		return executeQueryStatement(getSQLString(), queryParameters, scroll,
				afterLoadActions, session);
	}

	protected ResultSet executeQueryStatement(final String sqlStatement,
			final QueryParameters queryParameters, final boolean scroll,
			List<AfterLoadAction> afterLoadActions,
			final SessionImplementor session) throws SQLException {

		// Processing query filters.
		queryParameters.processFilters(getSQLString(), session);

		// Applying LIMIT clause.
		final LimitHandler limitHandler = getLimitHandler(
				queryParameters.getFilteredSQL(),
				queryParameters.getRowSelection());
		String sql = limitHandler.getProcessedSql();

		// Adding locks and comments.
		sql = preprocessSQL(sql, queryParameters, getFactory().getDialect(),
				afterLoadActions);

		final PreparedStatement st = prepareQueryStatement(sql,
				queryParameters, limitHandler, scroll, session);
		return getResultSet(st, queryParameters.getRowSelection(),
				limitHandler, queryParameters.hasAutoDiscoverScalarTypes(),
				session);
	}

	private synchronized ResultSet wrapResultSetIfEnabled(final ResultSet rs,
			final SessionImplementor session) {
		if (session.getFactory().getSettings().isWrapResultSetsEnabled()) {
			// try {
			// return session.getFactory()
			// .getJdbcServices()
			// .getResultSetWrapper().wrap( rs, retreiveColumnNameToIndexCache(
			// rs ) );
			// }
			// catch(SQLException e) {
			// return rs;
			// }
			return rs;
		} else {
			return rs;
		}
	}

	protected final ResultSet getResultSet(final PreparedStatement st,
			final RowSelection selection, final LimitHandler limitHandler,
			final boolean autodiscovertypes, final SessionImplementor session)
			throws SQLException {

		try {
			ResultSet rs = st.executeQuery();
			rs = wrapResultSetIfEnabled(rs, session);

			if (!limitHandler.supportsLimitOffset()
					|| !LimitHelper.useLimit(limitHandler, selection)) {
				advance(rs, selection);
			}

			if (autodiscovertypes) {
				autoDiscoverTypes(rs);
			}
			return rs;
		} catch (SQLException sqle) {
			st.close();
			throw sqle;
		}
	}

	protected void autoDiscoverTypes(ResultSet rs) {
		throw new AssertionFailure(
				"Auto discover types not supported in this loader");

	}

	private void advance(final ResultSet rs, final RowSelection selection)
			throws SQLException {

		final int firstRow = LimitHelper.getFirstRow(selection);
		if (firstRow != 0) {
			if (getFactory().getSettings().isScrollableResultSetsEnabled()) {
				// we can go straight to the first required row
				rs.absolute(firstRow);
			} else {
				// we need to step through the rows one row at a time (slow)
				for (int m = 0; m < firstRow; m++)
					rs.next();
			}
		}
	}

	protected final PreparedStatement prepareQueryStatement(final String sql,
			final QueryParameters queryParameters,
			final LimitHandler limitHandler, final boolean scroll,
			final SessionImplementor session) throws SQLException {
		final Dialect dialect = getFactory().getDialect();
		final RowSelection selection = queryParameters.getRowSelection();
		boolean useLimit = LimitHelper.useLimit(limitHandler, selection);
		boolean hasFirstRow = LimitHelper.hasFirstRow(selection);
		boolean useLimitOffset = hasFirstRow && useLimit
				&& limitHandler.supportsLimitOffset();
		boolean callable = queryParameters.isCallable();
		final ScrollMode scrollMode = getScrollMode(scroll, hasFirstRow,
				useLimitOffset, queryParameters);
		PreparedStatement st = session.getTransactionCoordinator()
				.getJdbcCoordinator().getStatementPreparer()
				.prepareQueryStatement(sql, callable, scrollMode);
		try {
			int col = 1;
			col += limitHandler.bindLimitParametersAtStartOfQuery(st, col);

			if (callable) {
				col = dialect.registerResultSetOutParameter(
						(CallableStatement) st, col);
			}
			col += bindParameterValues(st, queryParameters, col, session);
			col += limitHandler.bindLimitParametersAtEndOfQuery(st, col);
			limitHandler.setMaxRows(st);
			if (selection != null) {
				if (selection.getTimeout() != null) {
					st.setQueryTimeout(selection.getTimeout());
				}
				if (selection.getFetchSize() != null) {
					st.setFetchSize(selection.getFetchSize());
				}
			}
			LockOptions lockOptions = queryParameters.getLockOptions();
			if (lockOptions != null) {
				if (lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER) {
					if (!dialect.supportsLockTimeouts()) {
					} else if (dialect.isLockTimeoutParameterized()) {
						st.setInt(col++, lockOptions.getTimeOut());
					}
				}
			}
		} catch (SQLException sqle) {
			st.close();
			throw sqle;
		} catch (ZormException he) {
			st.close();
			throw he;
		}

		return st;

	}

	protected int bindParameterValues(PreparedStatement statement,
			QueryParameters queryParameters, int startIndex,
			SessionImplementor session) throws SQLException {
		int span = 0;
		// 设置位置参数
		span += bindPositionalParameters(statement, queryParameters,
				startIndex, session);
		span += bindNamedParameters(statement,
				queryParameters.getNamedParameters(), startIndex + span,
				session);
		return span;
	}

	protected int bindNamedParameters(final PreparedStatement statement,
			final Map namedParams, final int startIndex,
			final SessionImplementor session) throws SQLException {
		if (namedParams != null) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while (iter.hasNext()) {
				Map.Entry e = (Map.Entry) iter.next();
				String name = (String) e.getKey();
				TypedValue typedval = (TypedValue) e.getValue();
				int[] locs = getNamedParameterLocs(name);
				for (int i = 0; i < locs.length; i++) {
					typedval.getType().nullSafeSet(statement,
							typedval.getValue(), locs[i] + startIndex, session);
				}
				result += locs.length;
			}
			return result;
		} else {
			return 0;
		}
	}

	public int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure("no named parameters");
	}

	protected int bindPositionalParameters(final PreparedStatement statement,
			final QueryParameters queryParameters, final int startIndex,
			final SessionImplementor session) throws SQLException {
		final Object[] values = queryParameters
				.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters
				.getFilteredPositionalParameterTypes();
		int span = 0;
		for (int i = 0; i < values.length; i++) {
			types[i].nullSafeSet(statement, values[i], startIndex + span,
					session);
			span += types[i].getColumnSpan(getFactory());
		}
		return span;
	}

	private ScrollMode getScrollMode(boolean scroll, boolean hasFirstRow,
			boolean useLimitOffSet, QueryParameters queryParameters) {
		final boolean canScroll = getFactory().getSettings()
				.isScrollableResultSetsEnabled();
		if (canScroll) {
			if (scroll) {
				return queryParameters.getScrollMode();
			}
			if (hasFirstRow && !useLimitOffSet) {
				return ScrollMode.SCROLL_INSENSITIVE;
			}
		}
		return null;
	}

	protected String preprocessSQL(String sql, QueryParameters parameters,
			Dialect dialect, List<AfterLoadAction> afterLoadActions) {
		sql = applyLocks(sql, parameters, dialect, afterLoadActions);
		return getFactory().getSettings().isCommentsEnabled() ? prependComment(
				sql, parameters) : sql;
	}

	private String prependComment(String sql, QueryParameters parameters) {
		String comment = parameters.getComment();
		if (comment == null) {
			return sql;
		} else {
			return new StringBuilder(comment.length() + sql.length() + 5)
					.append("/* ").append(comment).append(" */ ").append(sql)
					.toString();
		}
	}

	protected String applyLocks(String sql, QueryParameters parameters,
			Dialect dialect, List<AfterLoadAction> afterLoadActions) {
		return sql;
	}

	private LimitHandler getLimitHandler(String sql, RowSelection rowSelection) {
		final LimitHandler limitHandler = getFactory().getDialect()
				.buildLimitHandler(sql, rowSelection);
		return LimitHelper.useLimit(limitHandler, rowSelection) ? limitHandler
				: new NoopLimitHandler(sql, rowSelection);
	}

	private SessionFactoryImplementor getFactory() {
		return factory;
	}

	private List doQuery(final SessionImplementor session,
			final QueryParameters queryParameters, final boolean returnProxies,
			final ResultTransformer forcedResultTransformer)
			throws SQLException {
		final RowSelection selection = queryParameters.getRowSelection();
		final int maxRows = LimitHelper.hasMaxRows(selection) ? selection
				.getMaxRows() : Integer.MAX_VALUE;
		final List<AfterLoadAction> afterLoadActions = new ArrayList<AfterLoadAction>();
		final ResultSet rs = executeQueryStatement(queryParameters, false,
				afterLoadActions, session);
		final Statement st = rs.getStatement();
		try {
			return processResultSet(rs, queryParameters, session,
					returnProxies, forcedResultTransformer, maxRows,
					afterLoadActions);
		} finally {
			st.close();
		}
	}

	protected abstract LockMode[] getLockModes(LockOptions lockOptions);

	protected boolean isSubselectLoadingEnabled() {
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List processResultSet(ResultSet rs,
			QueryParameters queryParameters, SessionImplementor session,
			boolean returnProxies, ResultTransformer forcedResultTransformer,
			int maxRows, List<AfterLoadAction> afterLoadActions)
			throws SQLException {
		final int entitySpan = getEntityPersisters().length;
		final EntityKey optionalObjectKey = getOptionalObjectKey(
				queryParameters, session);
		final LockMode[] lockModesArray = getLockModes(queryParameters
				.getLockOptions());
		final boolean createSubselects = isSubselectLoadingEnabled();
		final List subselectResultKeys = createSubselects ? new ArrayList()
				: null;
		final ArrayList hydratedObjects = entitySpan == 0 ? null
				: new ArrayList(entitySpan * 10);
		final List results = new ArrayList();

		EntityKey[] keys = new EntityKey[entitySpan]; // we can reuse it for
														// each row
		int count;
		for (count = 0; count < maxRows && rs.next(); count++) {
			Object result = getRowFromResultSet(rs, session, queryParameters,
					lockModesArray, optionalObjectKey, hydratedObjects, keys,
					returnProxies, forcedResultTransformer);
			results.add(result);
			if (createSubselects) {
				subselectResultKeys.add(keys);
				keys = new EntityKey[entitySpan]; // can't reuse in this case
			}
		}

		initializeEntitiesAndCollections(hydratedObjects, rs, session,
				queryParameters.isReadOnly(session), afterLoadActions);
		return results;

	}

	private void endCollectionLoad(final Object resultSetId,
			final SessionImplementor session,
			final CollectionPersister collectionPersister) {
		session.getPersistenceContext().getLoadContexts()
				.getCollectionLoadContext((ResultSet) resultSetId)
				.endLoadingCollections(collectionPersister);
	}

	private void initializeEntitiesAndCollections(final List hydratedObjects,
			final Object resultSetId, final SessionImplementor session,
			final boolean readOnly, List<AfterLoadAction> afterLoadActions)
			throws ZormException {

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if (collectionPersisters != null) {
			for (int i = 0; i < collectionPersisters.length; i++) {
				if (collectionPersisters[i].isArray()) {
					endCollectionLoad(resultSetId, session,
							collectionPersisters[i]);
				}
			}
		}

		final PreLoadEvent pre;
		final PostLoadEvent post;
		if (session.isEventSource()) {
			pre = new PreLoadEvent((EventSource) session);
			post = new PostLoadEvent((EventSource) session);
		} else {
			pre = null;
			post = null;
		}

		if (hydratedObjects != null) {
			int hydratedObjectsSize = hydratedObjects.size();
			for (int i = 0; i < hydratedObjectsSize; i++) {
				TwoPhaseLoad.initializeEntity(hydratedObjects.get(i), readOnly,
						session, pre, post);
			}
		}

		if (collectionPersisters != null) {
			for (int i = 0; i < collectionPersisters.length; i++) {
				if (!collectionPersisters[i].isArray()) {
					endCollectionLoad(resultSetId, session,
							collectionPersisters[i]);
				}
			}
		}

		if (hydratedObjects != null) {
			for (Object hydratedObject : hydratedObjects) {
				TwoPhaseLoad.postLoad(hydratedObject, session, post);
				if (afterLoadActions != null) {
					for (AfterLoadAction afterLoadAction : afterLoadActions) {
						final EntityEntry entityEntry = session
								.getPersistenceContext().getEntry(
										hydratedObject);
						if (entityEntry == null) {
							// big problem
							throw new ZormException(
									"Could not locate EntityEntry immediately after two-phase load");
						}
						afterLoadAction.afterLoad(session, hydratedObject,
								(Loadable) entityEntry.getPersister());
					}
				}
			}
		}
	}

	protected boolean isSingleRowLoader() {
		return false;
	}

	protected abstract CollectionAliases[] getCollectionAliases();

	protected void extractKeysFromResultSet(Loadable[] persisters,
			QueryParameters queryParameters, ResultSet resultSet,
			SessionImplementor session, EntityKey[] keys, LockMode[] lockModes,
			List hydratedObjects) throws SQLException {
		final int entitySpan = persisters.length;

		final int numberOfPersistersToProcess;
		final Serializable optionalId = queryParameters.getOptionalId();
		if (isSingleRowLoader() && optionalId != null) {
			keys[entitySpan - 1] = session.generateEntityKey(optionalId,
					persisters[entitySpan - 1]);
			// skip the last persister below...
			numberOfPersistersToProcess = entitySpan - 1;
		} else {
			numberOfPersistersToProcess = entitySpan;
		}

		final Object[] hydratedKeyState = new Object[numberOfPersistersToProcess];

		for (int i = 0; i < numberOfPersistersToProcess; i++) {
			final Type idType = persisters[i].getIdentifierType();
			hydratedKeyState[i] = idType.hydrate(resultSet,
					getEntityAliases()[i].getSuffixedKeyAliases(), session,
					null);
		}

		for (int i = 0; i < numberOfPersistersToProcess; i++) {
			final Type idType = persisters[i].getIdentifierType();
			// if ( idType.isComponentType() &&
			// getCompositeKeyManyToOneTargetIndices() != null ) {
			// // we may need to force resolve any key-many-to-one(s)
			// int[] keyManyToOneTargetIndices =
			// getCompositeKeyManyToOneTargetIndices()[i];
			// // todo : better solution is to order the index processing based
			// on target indices
			// // that would account for multiple levels whereas this scheme
			// does not
			// if ( keyManyToOneTargetIndices != null ) {
			// for ( int targetIndex : keyManyToOneTargetIndices ) {
			// if ( targetIndex < numberOfPersistersToProcess ) {
			// final Type targetIdType =
			// persisters[targetIndex].getIdentifierType();
			// final Serializable targetId = (Serializable)
			// targetIdType.resolve(
			// hydratedKeyState[targetIndex],
			// session,
			// null
			// );
			// // todo : need a way to signal that this key is resolved and its
			// data resolved
			// keys[targetIndex] = session.generateEntityKey( targetId,
			// persisters[targetIndex] );
			// }
			//
			// // this part copied from #getRow, this section could be
			// refactored out
			// Object object = session.getEntityUsingInterceptor(
			// keys[targetIndex] );
			// if ( object != null ) {
			// //its already loaded so don't need to hydrate it
			// instanceAlreadyLoaded(
			// resultSet,
			// targetIndex,
			// persisters[targetIndex],
			// keys[targetIndex],
			// object,
			// lockModes[targetIndex],
			// session
			// );
			// }
			// else {
			// instanceNotYetLoaded(
			// resultSet,
			// targetIndex,
			// persisters[targetIndex],
			// getEntityAliases()[targetIndex].getRowIdAlias(),
			// keys[targetIndex],
			// lockModes[targetIndex],
			// getOptionalObjectKey( queryParameters, session ),
			// queryParameters.getOptionalObject(),
			// hydratedObjects,
			// session
			// );
			// }
			// }
			// }
			// }
			final Serializable resolvedId = (Serializable) idType.resolve(
					hydratedKeyState[i], session, null);
			keys[i] = resolvedId == null ? null : session.generateEntityKey(
					resolvedId, persisters[i]);
		}
	}

	@SuppressWarnings("rawtypes")
	private Object getRowFromResultSet(final ResultSet resultSet,
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final LockMode[] lockModesArray, final EntityKey optionalObjectKey,
			final List hydratedObjects, final EntityKey[] keys,
			boolean returnProxies, ResultTransformer forcedResultTransformer)
			throws SQLException {
		final Loadable[] persisters = getEntityPersisters();
		extractKeysFromResultSet(persisters, queryParameters, resultSet,
				session, keys, lockModesArray, hydratedObjects);

		Object[] row = getRow(resultSet, persisters, keys,
				queryParameters.getOptionalObject(), optionalObjectKey,
				lockModesArray, hydratedObjects, session);

		readCollectionElements(row, resultSet, session);

		return forcedResultTransformer == null ? getResultColumnOrRow(row,
				queryParameters.getResultTransformer(), resultSet, session)
				: forcedResultTransformer.transformTuple(
						getResultRow(row, resultSet, session),
						getResultRowAliases());
	}

	protected int[] getCollectionOwners() {
		return null;
	}

	private void readCollectionElements(Object[] row, ResultSet resultSet,
			SessionImplementor session) throws SQLException, ZormException {

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if (collectionPersisters != null) {

			final CollectionAliases[] descriptors = getCollectionAliases();
			final int[] collectionOwners = getCollectionOwners();

			for (int i = 0; i < collectionPersisters.length; i++) {

				final boolean hasCollectionOwners = collectionOwners != null
						&& collectionOwners[i] > -1;

				final Object owner = hasCollectionOwners ? row[collectionOwners[i]]
						: null; // if null, owner will be retrieved from session

				final CollectionPersister collectionPersister = collectionPersisters[i];
				final Serializable key;
				if (owner == null) {
					key = null;
				} else {
					key = collectionPersister.getCollectionType()
							.getKeyOfOwner(owner, session);
				}

				readCollectionElement(owner, key, collectionPersister,
						descriptors[i], resultSet, session);

			}

		}
	}

	private void readCollectionElement(final Object optionalOwner,
			final Serializable optionalKey,
			final CollectionPersister persister,
			final CollectionAliases descriptor, final ResultSet rs,
			final SessionImplementor session) throws ZormException,
			SQLException {

		final PersistenceContext persistenceContext = session
				.getPersistenceContext();

		final Serializable collectionRowKey = (Serializable) persister.readKey(
				rs, descriptor.getSuffixedKeyAliases(), session);

		if (collectionRowKey != null) {

			Object owner = optionalOwner;
			if (owner == null) {
				owner = persistenceContext.getCollectionOwner(collectionRowKey,
						persister);
				if (owner == null) {
				}
			}

			PersistentCollection rowCollection = persistenceContext
					.getLoadContexts().getCollectionLoadContext(rs)
					.getLoadingCollection(persister, collectionRowKey);

			if (rowCollection != null) {
				rowCollection.readFrom(rs, persister, descriptor, owner);
			}

		} else if (optionalKey != null) {

			persistenceContext.getLoadContexts().getCollectionLoadContext(rs)
					.getLoadingCollection(persister, optionalKey); // handle
																	// empty
																	// collection

		}

	}

	protected Object getResultColumnOrRow(Object[] row,
			ResultTransformer transformer, ResultSet rs,
			SessionImplementor session) throws SQLException {
		return row;
	}

	protected Object[] getResultRow(Object[] row, ResultSet rs,
			SessionImplementor session) throws SQLException {
		return row;
	}

	protected String[] getResultRowAliases() {
		return null;
	}

	protected abstract EntityAliases[] getEntityAliases();

	private Object[] getRow(final ResultSet rs, final Loadable[] persisters,
			final EntityKey[] keys, final Object optionalObject,
			final EntityKey optionalObjectKey, final LockMode[] lockModes,
			final List hydratedObjects, final SessionImplementor session)
			throws SQLException {

		final int cols = persisters.length;
		final EntityAliases[] descriptors = getEntityAliases();

		final Object[] rowResults = new Object[cols];

		for (int i = 0; i < cols; i++) {

			Object object = null;
			EntityKey key = keys[i];

			if (keys[i] == null) {
				// do nothing
			} else {

				// If the object is already loaded, return the loaded one
				object = session.getEntityUsingInterceptor(key);
				if (object == null) {
					object = instanceNotYetLoaded(rs, i, persisters[i],
							descriptors[i].getRowIdAlias(), key, lockModes[i],
							optionalObjectKey, optionalObject, hydratedObjects,
							session);
				}

			}

			rowResults[i] = object;

		}

		return rowResults;
	}

	private Object instanceNotYetLoaded(final ResultSet rs, final int i,
			final Loadable persister, final String rowIdAlias,
			final EntityKey key, final LockMode lockMode,
			final EntityKey optionalObjectKey, final Object optionalObject,
			final List hydratedObjects, final SessionImplementor session)
			throws SQLException {
		final String instanceClass = getInstanceClass(rs, i, persister,
				key.getIdentifier(), session);

		final Object object;
		if (optionalObjectKey != null && key.equals(optionalObjectKey)) {
			// its the given optional object
			object = optionalObject;
		} else {
			// instantiate a new instance
			object = session.instantiate(instanceClass, key.getIdentifier());
		}

		LockMode acquiredLockMode = lockMode == LockMode.NONE ? LockMode.READ
				: lockMode;
		loadFromResultSet(rs, i, object, instanceClass, key, rowIdAlias,
				acquiredLockMode, persister, session);

		// materialize associations (and initialize the object) later
		hydratedObjects.add(object);

		return object;
	}

	protected boolean[] getEntityEagerPropertyFetches() {
		return null;
	}

	private boolean isEagerPropertyFetchEnabled(int i) {
		boolean[] array = getEntityEagerPropertyFetches();
		return array != null && array[i];
	}

	protected EntityType[] getOwnerAssociationTypes() {
		return null;
	}

	protected CollectionPersister[] getCollectionPersisters() {
		return null;
	}

	private void loadFromResultSet(final ResultSet rs, final int i,
			final Object object, final String instanceEntityName,
			final EntityKey key, final String rowIdAlias,
			final LockMode lockMode, final Loadable rootPersister,
			final SessionImplementor session) throws SQLException,
			ZormException {

		final Serializable id = key.getIdentifier();

		// Get the persister for the _subclass_
		final Loadable persister = (Loadable) getFactory().getEntityPersister(
				instanceEntityName);

		boolean eagerPropertyFetch = isEagerPropertyFetchEnabled(i);

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity(key, object, persister, lockMode,
				!eagerPropertyFetch, session);

		final String[][] cols = persister == rootPersister ? getEntityAliases()[i]
				.getSuffixedPropertyAliases() : getEntityAliases()[i]
				.getSuffixedPropertyAliases(persister);

		final Object[] values = persister.hydrate(rs, id, object,
				rootPersister, cols, eagerPropertyFetch, session);

		final Object rowId = persister.hasRowId() ? rs.getObject(rowIdAlias)
				: null;

		final AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
		if (ownerAssociationTypes != null && ownerAssociationTypes[i] != null) {
			String ukName = ownerAssociationTypes[i]
					.getRHSUniqueKeyPropertyName();
			if (ukName != null) {
				// final int index = ( (UniqueKeyLoadable) persister
				// ).getPropertyIndex(ukName);
				// final Type type = persister.getPropertyTypes()[index];
				//
				// // polymorphism not really handled completely correctly,
				// // perhaps...well, actually its ok, assuming that the
				// // entity name used in the lookup is the same as the
				// // the one used here, which it will be
				//
				// EntityUniqueKey euk = new EntityUniqueKey(
				// rootPersister.getEntityName(), //polymorphism comment above
				// ukName,
				// type.semiResolve( values[index], session, object ),
				// type,
				// persister.getEntityMode(),
				// session.getFactory()
				// );
				// session.getPersistenceContext().addEntity( euk, object );
			}
		}

		TwoPhaseLoad.postHydrate(persister, id, values, rowId, object,
				lockMode, !eagerPropertyFetch, session);

	}

	private String getInstanceClass(final ResultSet rs, final int i,
			final Loadable persister, final Serializable id,
			final SessionImplementor session) throws SQLException {
		if (persister.hasSubclasses()) {

			// Code to handle subclasses of topClass
			Object discriminatorValue = persister.getDiscriminatorType()
					.nullSafeGet(
							rs,
							getEntityAliases()[i]
									.getSuffixedDiscriminatorAlias(), session,
							null);

			final String result = persister
					.getSubclassForDiscriminatorValue(discriminatorValue);

			if (result == null) {
				// woops we got an instance of another class hierarchy branch
				throw new WrongClassException("Discriminator: "
						+ discriminatorValue, id, persister.getEntityName());
			}

			return result;
		} else {
			return persister.getEntityName();
		}

	}

	private static EntityKey getOptionalObjectKey(
			QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters
				.getOptionalEntityName();

		if (optionalObject != null && optionalEntityName != null) {
			return session.generateEntityKey(optionalId, session
					.getEntityPersister(optionalEntityName, optionalObject));
		} else {
			return null;
		}

	}

	protected abstract Loadable[] getEntityPersisters();

	protected List list(final SessionImplementor session,
			final QueryParameters queryParameters, final Set querySpaces,
			final Type[] resultTypes) throws ZormException {

		final boolean cacheable = factory.getSettings().isQueryCacheEnabled()
				&& queryParameters.isCacheable();

		if (cacheable) {
			// return listUsingQueryCache( session, queryParameters,
			// querySpaces, resultTypes );
			return null;
		} else {
			return listIgnoreQueryCache(session, queryParameters);
		}
	}

	private List listIgnoreQueryCache(SessionImplementor session,
			QueryParameters queryParameters) {
		return getResultList(doList(session, queryParameters),
				queryParameters.getResultTransformer());
	}

	protected List getResultList(List results,
			ResultTransformer resultTransformer) throws QueryException {
		return results;
	}

	protected List doList(final SessionImplementor session,
			final QueryParameters queryParameters) throws ZormException {
		return doList(session, queryParameters, null);
	}

	private List doList(final SessionImplementor session,
			final QueryParameters queryParameters,
			final ResultTransformer forcedResultTransformer)
			throws ZormException {

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections(session,
					queryParameters, true, forcedResultTransformer);
		} catch (SQLException sqle) {
			throw factory.getSQLExceptionHelper().convert(sqle,
					"could not execute query", getSQLString());
		}

		return result;
	}

}

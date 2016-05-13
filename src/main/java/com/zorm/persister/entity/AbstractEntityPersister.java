package com.zorm.persister.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.zorm.FetchMode;
import com.zorm.LazyPropertyInitializer;
import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.engine.BasicBatchKey;
import com.zorm.engine.CascadeStyle;
//import com.zorm.engine.CascadeStyle;
import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.engine.Mapping;
import com.zorm.engine.OptimisticLockStyle;
import com.zorm.engine.ValueInclusion;
import com.zorm.engine.Versioning;
import com.zorm.entity.BatchingEntityLoader;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityInstrumentationMetadata;
import com.zorm.entity.EntityKey;
import com.zorm.entity.EntityMetamodel;
import com.zorm.entity.EntityMode;
import com.zorm.event.EventSource;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.exception.StaleObjectStateException;
import com.zorm.exception.StaleStateException;
import com.zorm.exception.TooManyRowsAffectedException;
import com.zorm.exception.ZormException;
import com.zorm.id.Binder;
import com.zorm.id.IdentifierGenerator;
import com.zorm.id.InsertGeneratedIdentifierDelegate;
import com.zorm.id.PostInsertIdentifierGenerator;
import com.zorm.jdbc.Expectation;
import com.zorm.jdbc.Expectations;
import com.zorm.mapping.Column;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.Selectable;
import com.zorm.meta.ClassMetadata;
import com.zorm.property.BackrefPropertyAccessor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.sql.Alias;
import com.zorm.sql.Delete;
import com.zorm.sql.Insert;
import com.zorm.sql.JoinFragment;
import com.zorm.sql.JoinType;
import com.zorm.sql.Select;
import com.zorm.sql.SelectFragment;
import com.zorm.sql.SimpleSelect;
import com.zorm.sql.Update;
import com.zorm.tuple.EntityTuplizer;
import com.zorm.type.EntityType;
import com.zorm.type.Type;
import com.zorm.type.VersionType;
import com.zorm.util.ArrayHelper;
import com.zorm.util.FilterHelper;
import com.zorm.util.MessageHelper;
import com.zorm.util.StringHelper;
import com.zorm.util.TypeHelper;

@SuppressWarnings("unused")
public abstract class AbstractEntityPersister implements OuterJoinLoadable,
		Queryable, ClassMetadata, PostInsertIdentityPersister {

	public static final String ENTITY_CLASS = "class";

	private final SessionFactoryImplementor factory;
	private final EntityMetamodel entityMetamodel;
	private final EntityTuplizer entityTuplizer;
	private InsertGeneratedIdentifierDelegate identityDelegate;

	private final String[] lazyPropertyNames;
	private final int[] lazyPropertyNumbers;
	private final Type[] lazyPropertyTypes;
	private final String[][] lazyPropertyColumnAliases;

	private final String[] rootTableKeyColumnNames;
	private final String[] rootTableKeyColumnReaders;
	private final String[] rootTableKeyColumnReaderTemplates;
	private final String[] identifierAliases;
	private final int identifierColumnSpan;
	private final boolean hasFormulaProperties;
	private final int batchSize;
	protected final String rowIdName;
	private final String versionColumnName;
	private final Set lazyProperties;

	private final String sqlWhereString;
	private final String sqlWhereStringTemplate;

	private final int[] propertyColumnSpans;
	private final String[] propertySubclassNames;
	private final String[][] propertyColumnAliases;
	private final String[][] propertyColumnNames;
	private final String[][] propertyColumnFormulaTemplates;
	private final String[][] propertyColumnReaderTemplates;
	private final String[][] propertyColumnWriters;
	private final boolean[][] propertyColumnUpdateable;
	private final boolean[][] propertyColumnInsertable;
	private final boolean[] propertyUniqueness;
	private final boolean[] propertySelectable;
	private final FilterHelper filterHelper;

	private final List<Integer> lobProperties = new ArrayList<Integer>();

	// information about all properties in class hierarchy
	private final String[] subclassPropertyNameClosure;
	private final String[] subclassPropertySubclassNameClosure;
	private final Type[] subclassPropertyTypeClosure;
	private final String[][] subclassPropertyFormulaTemplateClosure;
	private final String[][] subclassPropertyColumnNameClosure;
	private final String[][] subclassPropertyColumnReaderClosure;
	private final String[][] subclassPropertyColumnReaderTemplateClosure;
	private final FetchMode[] subclassPropertyFetchModeClosure;
	private final boolean[] subclassPropertyNullabilityClosure;
	private final boolean[] propertyDefinedOnSubclass;
	private final int[][] subclassPropertyColumnNumberClosure;
	private final int[][] subclassPropertyFormulaNumberClosure;
	// private final CascadeStyle[] subclassPropertyCascadeStyleClosure;

	// information about all columns/formulas in class hierarchy
	private final String[] subclassColumnClosure;
	private final boolean[] subclassColumnLazyClosure;
	private final String[] subclassColumnAliasClosure;
	private final boolean[] subclassColumnSelectableClosure;
	private final String[] subclassColumnReaderTemplateClosure;
	private final String[] subclassFormulaClosure;
	private final String[] subclassFormulaTemplateClosure;
	private final String[] subclassFormulaAliasClosure;
	private final boolean[] subclassFormulaLazyClosure;
	private final CascadeStyle[] subclassPropertyCascadeStyleClosure;
	// private final FilterHelper filterHelper;

	private final Set affectingFetchProfileNames = new HashSet();
	private final Map uniqueKeyLoaders = new HashMap();
	private final Map lockers = new HashMap();
	private final Map loaders = new HashMap();

	// SQL strings
	private String sqlVersionSelectString;
	private String sqlSnapshotSelectString;
	private String sqlLazySelectString;

	private String sqlIdentityInsertString;
	private String sqlUpdateByRowIdString;
	private String sqlLazyUpdateByRowIdString;

	private String[] sqlDeleteStrings;
	private String[] sqlInsertStrings;
	private String[] sqlUpdateStrings;
	private String[] sqlLazyUpdateStrings;

	private String sqlInsertGeneratedValuesSelectString;
	private String sqlUpdateGeneratedValuesSelectString;

	// Custom SQL (would be better if these were private)
	protected boolean[] insertCallable;
	protected boolean[] updateCallable;
	protected boolean[] deleteCallable;
	protected String[] customSQLInsert;
	protected String[] customSQLUpdate;
	protected String[] customSQLDelete;
	protected ExecuteUpdateResultCheckStyle[] insertResultCheckStyles;
	protected ExecuteUpdateResultCheckStyle[] updateResultCheckStyles;
	protected ExecuteUpdateResultCheckStyle[] deleteResultCheckStyles;

	// private InsertGeneratedIdentifierDelegate identityDelegate;

	private boolean[] tableHasColumns;

	private final String loaderName;

	private UniqueEntityLoader queryLoader;

	private final String temporaryIdTableName;
	private final String temporaryIdTableDDL;

	private final Map subclassPropertyAliases = new HashMap();
	private final Map subclassPropertyColumnNames = new HashMap();

	protected final BasicEntityPropertyMapping propertyMapping;

	public boolean consumesEntityAlias() {
		return true;
	}

	@Override
	public int countSubclassProperties() {
		return subclassPropertyTypeClosure.length;
	}

	@Override
	public Type getSubclassPropertyType(int i) {
		return subclassPropertyTypeClosure[i];
	}

	@Override
	public boolean isSubclassPropertyNullable(int i) {
		return subclassPropertyNullabilityClosure[i];
	}

	@Override
	public String getSubclassPropertyName(int i) {
		return subclassPropertyNameClosure[i];
	}

	@Override
	public FetchMode getFetchMode(int i) {
		return subclassPropertyFetchModeClosure[i];
	}

	@Override
	public CascadeStyle getCascadeStyle(int i) {
		return subclassPropertyCascadeStyleClosure[i];
	}

	public Serializable[] getQuerySpaces() {
		return getPropertySpaces();
	}

	public String[] toColumns(String alias, String propertyName)
			throws QueryException {
		return propertyMapping.toColumns(alias, propertyName);
	}

	public String whereJoinFragment(String alias, boolean innerJoin,
			boolean includeSubclasses) {
		return getSubclassTableSpan() == 1 ? "" : 
				createJoin(alias, innerJoin, includeSubclasses).toWhereFragmentString();
	}

	@Override
	public Type getType() {
		return entityMetamodel.getEntityType();
	}

	public Type getIdentifierType() {
		return entityMetamodel.getIdentifierProperty().getType();
	}

	// protected void addDiscriminatorToInsert(Insert insert) {}

	// protected void addDiscriminatorToSelect(SelectFragment select, String
	// name, String suffix) {}

	protected abstract int[] getSubclassColumnTableNumberClosure();

	protected abstract int[] getSubclassFormulaTableNumberClosure();

	public abstract String getSubclassTableName(int j);

	protected abstract String[] getSubclassTableKeyColumns(int j);

	protected abstract boolean isClassOrSuperclassTable(int j);

	protected abstract int getSubclassTableSpan();

	protected abstract boolean isTableCascadeDeleteEnabled(int j);

	protected abstract String getTableName(int j);

	protected abstract String[] getKeyColumns(int j);

	protected abstract boolean isPropertyOfTable(int property, int j);

	protected abstract int[] getPropertyTableNumbersInSelect();

	protected abstract int[] getPropertyTableNumbers();

	protected abstract String filterFragment(String alias)
			throws MappingException;

	protected abstract int getSubclassPropertyTableNumber(int i);

	protected void addDiscriminatorToInsert(Insert insert) {
	}

	private static final String DISCRIMINATOR_ALIAS = "clazz_";

	public String filterFragment(String alias, Map enabledFilters)
			throws MappingException {
		final StringBuilder sessionFilterFragment = new StringBuilder();
		filterHelper.render(sessionFilterFragment,
				getFilterAliasGenerator(alias), enabledFilters);
		return sessionFilterFragment.append(filterFragment(alias)).toString();
	}

	public void postInstantiate() throws MappingException {
		createLoaders();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return entityMetamodel.isExplicitPolymorphism();
	}

	protected UniqueEntityLoader createEntityLoader(LockMode lockMode)
			throws MappingException {
		return createEntityLoader(lockMode, LoadQueryInfluencers.NONE);
	}

	public EntityType getEntityType() {
		return entityMetamodel.getEntityType();
	}

	protected String getSQLWhereString(String alias) {
		return StringHelper.replace(sqlWhereStringTemplate, Template.TEMPLATE,
				alias);
	}

	protected UniqueEntityLoader createEntityLoader(LockMode lockMode,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		return BatchingEntityLoader.createBatchingEntityLoader(this, batchSize,
				lockMode, getFactory(), loadQueryInfluencers);
	}

	protected void createLoaders() {
		final Map loaders = getLoaders();
		loaders.put(LockMode.NONE, createEntityLoader(LockMode.NONE));
		// UniqueEntityLoader readLoader = createEntityLoader( LockMode.READ );
		// loaders.put( LockMode.READ, readLoader );
	}

	@Override
	public Object instantiate(Serializable id, SessionImplementor session) {
		return getEntityTuplizer().instantiate(id, session);
	}

	@Override
	public Object getPropertyValue(Object object, int i) {
		return getEntityTuplizer().getPropertyValue(object, i);
	}

	@Override
	public Object getPropertyValue(Object object, String propertyName) {
		return getEntityTuplizer().getPropertyValue(object, propertyName);
	}

	@Override
	public boolean isInstance(Object object) {
		return getEntityTuplizer().isInstance(object);
	}

	public String oneToManyFilterFragment(String alias) throws MappingException {
		return "";
	}

	public Comparator getVersionComparator() {
		return isVersioned() ? getVersionType().getComparator() : null;
	}

	@Override
	public void resetIdentifier(Object entity, Serializable currentId,
			Object currentVersion, SessionImplementor session) {
		getEntityTuplizer().resetIdentifier(entity, currentId, currentVersion,
				session);
	}

	public boolean consumesCollectionAlias() {
		return false;
	}

	public String getName() {
		return getEntityName();
	}

	public boolean isCollection() {
		return false;
	}

	public final String getEntityName() {
		return entityMetamodel.getName();
	}

	public EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	public boolean hasSequentialSelect() {
		return false;
	}

	protected String[] getSubclassPropertySubclassNameClosure() {
		return subclassPropertySubclassNameClosure;
	}

	private void internalInitSubclassPropertyAliasesMap(String path,
			Iterator propertyIterator) {
		while (propertyIterator.hasNext()) {

			Property prop = (Property) propertyIterator.next();
			String propname = path == null ? prop.getName() : path + "." + prop.getName();
			if (prop.isComposite()) {
			}
			else {
				String[] aliases = new String[prop.getColumnSpan()];
				String[] cols = new String[prop.getColumnSpan()];
				Iterator colIter = prop.getColumnIterator();
				int l = 0;
				while (colIter.hasNext()) {
					Selectable thing = (Selectable) colIter.next();
					aliases[l] = thing.getAlias(getFactory().getDialect(), prop.getValue().getTable());
					cols[l] = thing.getText(getFactory().getDialect()); 
					l++;
				}

				subclassPropertyAliases.put(propname, aliases);
				subclassPropertyColumnNames.put(propname, cols);
			}
		}

	}

	@SuppressWarnings("unchecked")
	protected void initSubclassPropertyAliasesMap(PersistentClass model) throws MappingException {

		// ALIASES
		internalInitSubclassPropertyAliasesMap(null, model.getSubclassPropertyClosureIterator());

		if (!entityMetamodel.hasNonIdentifierPropertyNamedId()) {
			subclassPropertyAliases.put(ENTITY_ID, getIdentifierAliases());
			subclassPropertyColumnNames.put(ENTITY_ID, getIdentifierColumnNames());
		}

		if (hasIdentifierProperty()) {
			subclassPropertyAliases.put(getIdentifierPropertyName(), getIdentifierAliases());
			subclassPropertyColumnNames.put(getIdentifierPropertyName(), getIdentifierColumnNames());
		}

		if (entityMetamodel.isPolymorphic()) {
			subclassPropertyAliases.put(ENTITY_CLASS, new String[] { getDiscriminatorAlias() });
			subclassPropertyColumnNames.put(ENTITY_CLASS, new String[] { getDiscriminatorColumnName() });
		}
	}

	public Object[] hydrate(final ResultSet rs, final Serializable id,
			final Object object, final Loadable rootLoadable,
			final String[][] suffixedPropertyColumns,
			final boolean allProperties, final SessionImplementor session)
			throws SQLException {

		final AbstractEntityPersister rootPersister = (AbstractEntityPersister) rootLoadable;

		final boolean hasDeferred = rootPersister.hasSequentialSelect();
		PreparedStatement sequentialSelect = null;
		ResultSet sequentialResultSet = null;
		boolean sequentialSelectEmpty = false;
		try {

			if (hasDeferred) {
			}

			final String[] propNames = getPropertyNames();
			final Type[] types = getPropertyTypes();
			final Object[] values = new Object[types.length];
			final boolean[] laziness = getPropertyLaziness();
			final String[] propSubclassNames = getSubclassPropertySubclassNameClosure();

			for (int i = 0; i < types.length; i++) {
				if (!propertySelectable[i]) {
					values[i] = BackrefPropertyAccessor.UNKNOWN;
				} else if (allProperties || !laziness[i]) {
					// decide which ResultSet to get the property value from:
					final boolean propertyIsDeferred = hasDeferred
							&& rootPersister.isSubclassPropertyDeferred(
									propNames[i], propSubclassNames[i]);
					if (propertyIsDeferred && sequentialSelectEmpty) {
						values[i] = null;
					} else {
						final ResultSet propertyResultSet = propertyIsDeferred ? sequentialResultSet
								: rs;
						final String[] cols = propertyIsDeferred ? propertyColumnAliases[i]
								: suffixedPropertyColumns[i];
						values[i] = types[i].hydrate(propertyResultSet, cols, session, object);
					}
				} else {
					values[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
			}

			if (sequentialResultSet != null) {
				sequentialResultSet.close();
			}

			return values;

		} finally {
			if (sequentialSelect != null) {
				sequentialSelect.close();
			}
		}
	}

	private boolean[] getTableHasColumns() {
		return tableHasColumns;
	}

	protected int getPropertyColumnSpan(int i) {
		return propertyColumnSpans[i];
	}

	protected boolean[] getTableUpdateNeeded(final int[] dirtyProperties,
			boolean hasDirtyCollection) {

		if (dirtyProperties == null) {
			return getTableHasColumns(); // for objects that came in via
											// update()
		} else {
			boolean[] updateability = getPropertyUpdateability();
			int[] propertyTableNumbers = getPropertyTableNumbers();
			boolean[] tableUpdateNeeded = new boolean[getTableSpan()];
			for (int i = 0; i < dirtyProperties.length; i++) {
				int property = dirtyProperties[i];
				int table = propertyTableNumbers[property];
				tableUpdateNeeded[table] = tableUpdateNeeded[table]
						|| (getPropertyColumnSpan(property) > 0 && updateability[property]);
			}
			if (isVersioned()) {
				tableUpdateNeeded[0] = tableUpdateNeeded[0]
						|| Versioning.isVersionIncrementRequired(
								dirtyProperties, hasDirtyCollection,
								getPropertyVersionability());
			}
			return tableUpdateNeeded;
		}
	}

	private boolean isModifiableEntity(EntityEntry entry) {

		return (entry == null ? isMutable() : entry.isModifiableEntity());
	}

	public void update(final Serializable id, final Object[] fields,
			final int[] dirtyFields, final boolean hasDirtyCollection,
			final Object[] oldFields, final Object oldVersion,
			final Object object, final Object rowId,
			final SessionImplementor session) {
		final boolean[] tableUpdateNeeded = getTableUpdateNeeded(dirtyFields,
				hasDirtyCollection);
		final int span = getTableSpan();

		final boolean[] propsToUpdate;
		final String[] updateStrings;
		EntityEntry entry = session.getPersistenceContext().getEntry(object);

		// Ensure that an immutable or non-modifiable entity is not being
		// updated unless it is
		// in the process of being deleted.
		if (entry == null && !isMutable()) {
			throw new IllegalStateException(
					"Updating immutable entity that is not in session yet!");
		}
		if ((entityMetamodel.isDynamicUpdate() && dirtyFields != null)) {
			propsToUpdate = getPropertiesToUpdate(dirtyFields,
					hasDirtyCollection);
			updateStrings = new String[span];
			for (int j = 0; j < span; j++) {
				updateStrings[j] = tableUpdateNeeded[j] ? generateUpdateString(
						propsToUpdate, j, oldFields, j == 0 && rowId != null)
						: null;
			}
		} else if (!isModifiableEntity(entry)) {
			propsToUpdate = getPropertiesToUpdate(
					(dirtyFields == null ? ArrayHelper.EMPTY_INT_ARRAY
							: dirtyFields), hasDirtyCollection);
			// don't need to check laziness (dirty checking algorithm handles
			// that)
			updateStrings = new String[span];
			for (int j = 0; j < span; j++) {
				updateStrings[j] = tableUpdateNeeded[j] ? generateUpdateString(
						propsToUpdate, j, oldFields, j == 0 && rowId != null)
						: null;
			}
		} else {
			// For the case of dynamic-update="false", or no snapshot, we use
			// the static SQL
			updateStrings = getUpdateStrings(rowId != null,
					hasUninitializedLazyProperties(object));
			propsToUpdate = getPropertyUpdateability(object);
		}

		for (int j = 0; j < span; j++) {
			// Now update only the tables with dirty properties (and the table
			// with the version number)
			if (tableUpdateNeeded[j]) {
				updateOrInsert(id, fields, oldFields, j == 0 ? rowId : null,
						propsToUpdate, j, oldVersion, object, updateStrings[j],
						session);
			}
		}
	}

	protected String generateUpdateString(final boolean[] includeProperty,
			final int j, final Object[] oldFields, final boolean useRowId) {

		Update update = new Update(getFactory().getDialect())
				.setTableName(getTableName(j));

		if (useRowId) {
			update.addPrimaryKeyColumns(new String[] { rowIdName });
		} else {
			update.addPrimaryKeyColumns(getKeyColumns(j));
		}

		boolean hasColumns = false;
		for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
			if (includeProperty[i] && isPropertyOfTable(i, j)
					&& !lobProperties.contains(i)) {
				update.addColumns(getPropertyColumnNames(i),
						propertyColumnUpdateable[i], propertyColumnWriters[i]);
				hasColumns = hasColumns || getPropertyColumnSpan(i) > 0;
			}
		}

		for (int i : lobProperties) {
			if (includeProperty[i] && isPropertyOfTable(i, j)) {
				update.addColumns(getPropertyColumnNames(i),
						propertyColumnUpdateable[i], propertyColumnWriters[i]);
				hasColumns = true;
			}
		}

		if (j == 0
				&& isVersioned()
				&& entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.VERSION) {
			if (checkVersion(includeProperty)) {
				update.setVersionColumnName(getVersionColumnName());
				hasColumns = true;
			}
		} else if (isAllOrDirtyOptLocking() && oldFields != null) {

			boolean[] includeInWhere = entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.ALL ? getPropertyUpdateability() // optimistic-lock="all",
																																		// include
																																		// all
																																		// updatable
																																		// properties
					: includeProperty; // optimistic-lock="dirty", include all
										// properties we are updating this time

			boolean[] versionability = getPropertyVersionability();
			Type[] types = getPropertyTypes();
			for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
				boolean include = includeInWhere[i] && isPropertyOfTable(i, j)
						&& versionability[i];
				if (include) {
					String[] propertyColumnNames = getPropertyColumnNames(i);
					String[] propertyColumnWriters = getPropertyColumnWriters(i);
					boolean[] propertyNullness = types[i].toColumnNullness(
							oldFields[i], getFactory());
					for (int k = 0; k < propertyNullness.length; k++) {
						if (propertyNullness[k]) {
							update.addWhereColumn(propertyColumnNames[k], "="
									+ propertyColumnWriters[k]);
						} else {
							update.addWhereColumn(propertyColumnNames[k],
									" is null");
						}
					}
				}
			}

		}

		if (getFactory().getSettings().isCommentsEnabled()) {
			update.setComment("update " + getEntityName());
		}

		return hasColumns ? update.toStatementString() : null;
	}

	public String[] getPropertyColumnWriters(int i) {
		return propertyColumnWriters[i];
	}

	@Override
	public int[] findDirty(Object[] currentState, Object[] previousState,
			Object entity, SessionImplementor session) {
		int[] props = TypeHelper.findDirty(
				entityMetamodel.getProperties(),
				currentState, 
				previousState, 
				propertyColumnUpdateable,
				hasUninitializedLazyProperties(entity), 
				session);
		if (props == null) {
			return null;
		} else {
			return props;
		}
	}

	private boolean[] getPropertiesToUpdate(int[] dirtyFields,
			boolean hasDirtyCollection) {
		return null;
	}

	private boolean isAllNull(Object[] array, int tableNumber) {
		for (int i = 0; i < array.length; i++) {
			if (isPropertyOfTable(i, tableNumber) && array[i] != null) {
				return false;
			}
		}
		return true;
	}

	protected void updateOrInsert(final Serializable id, final Object[] fields,
			final Object[] oldFields, final Object rowId,
			final boolean[] includeProperty, final int j,
			final Object oldVersion, final Object object, final String sql,
			final SessionImplementor session) throws ZormException {

		if (!isInverseTable(j)) {

			final boolean isRowToUpdate;
			if (isNullableTable(j) && oldFields != null
					&& isAllNull(oldFields, j)) {
				isRowToUpdate = false;
			} else if (isNullableTable(j) && isAllNull(fields, j)) {
				isRowToUpdate = true;
				delete(id, oldVersion, j, object, getSQLDeleteStrings()[j],
						session, null);
			} else {
				isRowToUpdate = update(id, fields, oldFields, rowId,
						includeProperty, j, oldVersion, object, sql, session);
			}

			if (!isRowToUpdate && !isAllNull(fields, j)) {
				insert(id, fields, getPropertyInsertability(), j,
						getSQLInsertStrings()[j], object, session);
			}

		}

	}

	private BasicBatchKey updateBatchKey;

	protected boolean isUpdateCallable(int j) {
		return updateCallable[j];
	}

	public boolean[] getPropertyCheckability() {
		return entityMetamodel.getPropertyCheckability();
	}

	@Override
	public boolean hasProxy() {
		return entityMetamodel.isLazy();
	}

	public boolean hasCollections() {
		return entityMetamodel.hasCollections();
	}

	private int getSubclassPropertyIndex(String propertyName) {
		return ArrayHelper.indexOf(subclassPropertyNameClosure, propertyName);
	}

	protected String generateIdByUniqueKeySelectString(String uniquePropertyName) {
		Select select = new Select(getFactory().getDialect());

		if (getFactory().getSettings().isCommentsEnabled()) {
			select.setComment("resolve id by unique property ["
					+ getEntityName() + "." + uniquePropertyName + "]");
		}

		final String rooAlias = getRootAlias();

		select.setFromClause(fromTableFragment(rooAlias)
				+ fromJoinFragment(rooAlias, true, false));

		SelectFragment selectFragment = new SelectFragment();
		selectFragment.addColumns(rooAlias, getIdentifierColumnNames(),
				getIdentifierAliases());
		select.setSelectClause(selectFragment);

		StringBuilder whereClauseBuffer = new StringBuilder();
		final int uniquePropertyIndex = getSubclassPropertyIndex(uniquePropertyName);
		final String uniquePropertyTableAlias = generateTableAlias(rooAlias,
				getSubclassPropertyTableNumber(uniquePropertyIndex));
		String sep = "";
		for (String columnTemplate : getSubclassPropertyColumnReaderTemplateClosure()[uniquePropertyIndex]) {
			if (columnTemplate == null) {
				continue;
			}
			final String columnReference = StringHelper.replace(columnTemplate,
					Template.TEMPLATE, uniquePropertyTableAlias);
			whereClauseBuffer.append(sep).append(columnReference).append("=?");
			sep = " and ";
		}
		for (String formulaTemplate : getSubclassPropertyFormulaTemplateClosure()[uniquePropertyIndex]) {
			if (formulaTemplate == null) {
				continue;
			}
			final String formulaReference = StringHelper.replace(
					formulaTemplate, Template.TEMPLATE,
					uniquePropertyTableAlias);
			whereClauseBuffer.append(sep).append(formulaReference).append("=?");
			sep = " and ";
		}
		whereClauseBuffer.append(whereJoinFragment(rooAlias, true, false));

		select.setWhereClause(whereClauseBuffer.toString());

		return select.setOuterJoins("", "").toStatementString();
	}

	private String getRootAlias() {
		return StringHelper.generateAlias(getEntityName());
	}

	@Override
	public Serializable getIdByUniqueKey(Serializable key,
			String uniquePropertyName, SessionImplementor session)
			throws ZormException {

		int propertyIndex = getSubclassPropertyIndex(uniquePropertyName);
		if (propertyIndex < 0) {
			throw new ZormException("Could not determine Type for property ["
					+ uniquePropertyName + "] on entity [" + getEntityName()
					+ "]");
		}
		Type propertyType = getSubclassPropertyType(propertyIndex);

		try {
			PreparedStatement ps = session
					.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement(
							generateIdByUniqueKeySelectString(uniquePropertyName));
			try {
				propertyType.nullSafeSet(ps, key, 1, session);
				ResultSet rs = ps.executeQuery();
				try {
					// if there is no resulting row, return null
					if (!rs.next()) {
						return null;
					}
					return (Serializable) getIdentifierType().nullSafeGet(rs,
							getIdentifierAliases(), session, null);
				} finally {
					rs.close();
				}
			} finally {
				ps.close();
			}
		} catch (SQLException e) {
			throw getFactory()
					.getSQLExceptionHelper()
					.convert(
							e,
							String.format(
									"could not resolve unique property [%s] to identifier for entity [%s]",
									uniquePropertyName, getEntityName()),
							getSQLSnapshotSelectString());
		}

	}

	protected String getSQLSnapshotSelectString() {
		return sqlSnapshotSelectString;
	}

	protected boolean update(final Serializable id, final Object[] fields,
			final Object[] oldFields, final Object rowId,
			final boolean[] includeProperty, final int j,
			final Object oldVersion, final Object object, final String sql,
			final SessionImplementor session) throws ZormException {

		final Expectation expectation = Expectations
				.appropriateExpectation(updateResultCheckStyles[j]);
		final boolean useBatch = j == 0 && expectation.canBeBatched()
				&& isBatchable(); // note: updates to joined tables can't be
									// batched...
		if (useBatch && updateBatchKey == null) {
			updateBatchKey = new BasicBatchKey(getEntityName() + "#UPDATE",
					expectation);
		}
		final boolean callable = isUpdateCallable(j);
		final boolean useVersion = j == 0 && isVersioned();

		try {
			int index = 1; // starting index
			final PreparedStatement update;
			if (useBatch) {
				update = session.getTransactionCoordinator()
						.getJdbcCoordinator().getBatch(updateBatchKey)
						.getBatchStatement(sql, callable);
			} else {
				update = session.getTransactionCoordinator()
						.getJdbcCoordinator().getStatementPreparer()
						.prepareStatement(sql, callable);
			}

			try {
				index += expectation.prepare(update);

				// Now write the values of fields onto the prepared statement
				index = dehydrate(id, fields, rowId, includeProperty,
						propertyColumnUpdateable, j, update, session, index,
						true);

				// Write any appropriate versioning conditional parameters
				if (useVersion
						&& entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.VERSION) {
					if (checkVersion(includeProperty)) {
						getVersionType().nullSafeSet(update, oldVersion, index,
								session);
					}
				} else if (isAllOrDirtyOptLocking() && oldFields != null) {
					boolean[] versionability = getPropertyVersionability();
					boolean[] includeOldField = entityMetamodel
							.getOptimisticLockStyle() == OptimisticLockStyle.ALL ? getPropertyUpdateability()
							: includeProperty;
					Type[] types = getPropertyTypes();
					for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
						boolean include = includeOldField[i]
								&& isPropertyOfTable(i, j) && versionability[i];
						if (include) {
							boolean[] settable = types[i].toColumnNullness(
									oldFields[i], getFactory());
							types[i].nullSafeSet(update, oldFields[i], index,
									settable, session);
							index += ArrayHelper.countTrue(settable);
						}
					}
				}

				if (useBatch) {
					session.getTransactionCoordinator().getJdbcCoordinator()
							.getBatch(updateBatchKey).addToBatch();
					return true;
				} else {
					return check(update.executeUpdate(), id, j, expectation,
							update);
				}

			} catch (SQLException e) {
				if (useBatch) {
					session.getTransactionCoordinator().getJdbcCoordinator()
							.abortBatch();
				}
				throw e;
			} finally {
				if (!useBatch) {
					update.close();
				}
			}

		} catch (SQLException e) {
			throw getFactory().getSQLExceptionHelper().convert(
					e,
					"could not update: "
							+ MessageHelper.infoString(this, id, getFactory()),
					sql);
		}
	}

	protected boolean check(int rows, Serializable id, int tableNumber,
			Expectation expectation, PreparedStatement statement)
			throws ZormException {
		try {
			expectation.verifyOutcome(rows, statement, -1);
		} catch (StaleStateException e) {
			if (!isNullableTable(tableNumber)) {
				if (getFactory().getStatistics().isStatisticsEnabled()) {
					getFactory().getStatisticsImplementor().optimisticFailure(
							getEntityName());
				}
				throw new StaleObjectStateException(getEntityName(), id);
			}
			return false;
		} catch (TooManyRowsAffectedException e) {
			throw new ZormException("Duplicate identifier in table for: "
					+ MessageHelper.infoString(this, id, getFactory()));
		} catch (Throwable t) {
			return false;
		}
		return true;
	}

	private boolean checkVersion(final boolean[] includeProperty) {
		return includeProperty[getVersionProperty()]
				|| entityMetamodel.getPropertyUpdateGenerationInclusions()[getVersionProperty()] != ValueInclusion.NONE;
	}

	protected boolean[] getPropertyUpdateability(Object entity) {
		return hasUninitializedLazyProperties(entity) ? getNonLazyPropertyUpdateability()
				: getPropertyUpdateability();
	}

	public boolean[] getNonLazyPropertyUpdateability() {
		return entityMetamodel.getNonlazyPropertyUpdateability();
	}

	private String[] getUpdateStrings(boolean byRowId, boolean lazy) {
		if (byRowId) {
			return lazy ? getSQLLazyUpdateByRowIdStrings()
					: getSQLUpdateByRowIdStrings();
		} else {
			return lazy ? getSQLLazyUpdateStrings() : getSQLUpdateStrings();
		}
	}

	protected String[] getSQLUpdateStrings() {
		return sqlUpdateStrings;
	}

	private String[] getSQLLazyUpdateStrings() {
		return null;
	}

	private String[] getSQLUpdateByRowIdStrings() {
		return null;
	}

	private String[] getSQLLazyUpdateByRowIdStrings() {
		return null;
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		return getEntityTuplizer().hasUninitializedLazyProperties(object);
	}

	public void setPropertyValues(Object object, Object[] values) {
		getEntityTuplizer().setPropertyValues(object, values);
	}

	public void setPropertyValue(Object object, int i, Object value) {
		getEntityTuplizer().setPropertyValue(object, i, value);
	}

	public void setPropertyValue(Object object, String propertyName,
			Object value) {
		getEntityTuplizer().setPropertyValue(object, propertyName, value);
	}

	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		return entityMetamodel.getPropertyUpdateGenerationInclusions();
	}

	public boolean isVersionPropertyGenerated() {
		return isVersioned()
				&& (getPropertyUpdateGenerationInclusions()[getVersionProperty()] != ValueInclusion.NONE);
	}

	public boolean hasRowId() {
		return rowIdName != null;
	}

	public final Class getMappedClass() {
		return getEntityTuplizer().getMappedClass();
	}

	public final String selectFragment(Joinable rhs, String rhsAlias,
			String lhsAlias, String entitySuffix, String collectionSuffix,
			boolean includeCollectionColumns) {
		return selectFragment(lhsAlias, entitySuffix);
	}

	private boolean isSubclassPropertyDeferred(String string, String string2) {
		return false;
	}

	public boolean hasSubclasses() {
		return entityMetamodel.hasSubclasses();
	}

	public String getIdentifierPropertyName() {
		return entityMetamodel.getIdentifierProperty().getName();
	}

	public String[] getIdentifierAliases(String suffix) {
		return new Alias(suffix).toAliasStrings(getIdentifierAliases());
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	public String[] getPropertyNames() {
		return entityMetamodel.getPropertyNames();
	}

	protected String getDiscriminatorAlias() {
		return DISCRIMINATOR_ALIAS;
	}

	public String getDiscriminatorAlias(String suffix) {
		return entityMetamodel.hasSubclasses() ? new Alias(suffix)
				.toAliasString(getDiscriminatorAlias()) : null;
	}

	public String[] getPropertyAliases(String suffix, int i) {
		return new Alias(suffix)
				.toUnquotedAliasStrings(propertyColumnAliases[i]);
	}

	@Override
	public void setIdentifier(Object entity, Serializable id,
			SessionImplementor session) {
		getEntityTuplizer().setIdentifier(entity, id, session);
	}

	public AbstractEntityPersister(final PersistentClass persistentClass, final SessionFactoryImplementor factory) throws ZormException {

		this.factory = factory;
		this.entityMetamodel = new EntityMetamodel(persistentClass, factory);
		this.entityTuplizer = this.entityMetamodel.getTuplizer();

		int batch = persistentClass.getBatchSize();
		if (batch == -1) {
			batch = factory.getSettings().getDefaultBatchFetchSize();
		}
		batchSize = batch;
		propertyMapping = new BasicEntityPropertyMapping(this);

		// IDENTIFIER

		identifierColumnSpan = persistentClass.getIdentifier().getColumnSpan();
		rootTableKeyColumnNames = new String[identifierColumnSpan];
		rootTableKeyColumnReaders = new String[identifierColumnSpan];
		rootTableKeyColumnReaderTemplates = new String[identifierColumnSpan];
		identifierAliases = new String[identifierColumnSpan];
		rowIdName = persistentClass.getRootTable().getRowId();
		loaderName = persistentClass.getLoaderName();

		Iterator iter = persistentClass.getIdentifier().getColumnIterator();
		int i = 0;
		while (iter.hasNext()) {
			Column col = (Column) iter.next();
			rootTableKeyColumnNames[i] = col.getQuotedName(factory.getDialect());
			rootTableKeyColumnReaders[i] = col.getReadExpr(factory.getDialect());
			rootTableKeyColumnReaderTemplates[i] = col.getTemplate(factory.getDialect(), factory.getSqlFunctionRegistry());
			identifierAliases[i] = col.getAlias(factory.getDialect(),persistentClass.getRootTable());
			i++;
		}

		// VERSION

		if (persistentClass.isVersioned()) {
			versionColumnName = ((Column) persistentClass.getVersion()
					.getColumnIterator().next()).getQuotedName(factory
					.getDialect());
		} else {
			versionColumnName = null;
		}

		// WHERE STRING

		sqlWhereString = StringHelper.isNotEmpty(persistentClass.getWhere()) ? "( "+ persistentClass.getWhere() + ") "
				: null;
		sqlWhereStringTemplate = sqlWhereString == null ? null
				: Template.renderWhereStringTemplate(sqlWhereString,factory.getDialect(), factory.getSqlFunctionRegistry());

		// PROPERTIES

		final boolean lazyAvailable = isInstrumented();

		int hydrateSpan = entityMetamodel.getPropertySpan();
		propertyColumnSpans = new int[hydrateSpan];
		propertySubclassNames = new String[hydrateSpan];
		propertyColumnAliases = new String[hydrateSpan][];
		propertyColumnNames = new String[hydrateSpan][];
		propertyColumnFormulaTemplates = new String[hydrateSpan][];
		propertyColumnReaderTemplates = new String[hydrateSpan][];
		propertyColumnWriters = new String[hydrateSpan][];
		propertyUniqueness = new boolean[hydrateSpan];
		propertySelectable = new boolean[hydrateSpan];
		propertyColumnUpdateable = new boolean[hydrateSpan][];
		propertyColumnInsertable = new boolean[hydrateSpan][];
		HashSet thisClassProperties = new HashSet();

		lazyProperties = new HashSet();
		ArrayList lazyNames = new ArrayList();
		ArrayList lazyNumbers = new ArrayList();
		ArrayList lazyTypes = new ArrayList();
		ArrayList lazyColAliases = new ArrayList();

		iter = persistentClass.getPropertyClosureIterator();
		i = 0;
		boolean foundFormula = false;
		while (iter.hasNext()) {
			Property prop = (Property) iter.next();
			thisClassProperties.add(prop);

			int span = prop.getColumnSpan();
			propertyColumnSpans[i] = span;
			propertySubclassNames[i] = prop.getPersistentClass().getEntityName();
			String[] colNames = new String[span];
			String[] colAliases = new String[span];
			String[] colReaderTemplates = new String[span];
			String[] colWriters = new String[span];
			String[] formulaTemplates = new String[span];
			Iterator colIter = prop.getColumnIterator();
			int k = 0;
			while (colIter.hasNext()) {
				Selectable thing = (Selectable) colIter.next();
				colAliases[k] = thing.getAlias(factory.getDialect(), prop.getValue().getTable());
				if (thing.isFormula()) {
					foundFormula = true;
					formulaTemplates[k] = thing.getTemplate(factory.getDialect(),factory.getSqlFunctionRegistry());
				} else {
					Column col = (Column) thing;
					colNames[k] = col.getQuotedName(factory.getDialect());
					colReaderTemplates[k] = col.getTemplate(factory.getDialect(),factory.getSqlFunctionRegistry());
					colWriters[k] = col.getWriteExpr();
				}
				k++;
			}
			propertyColumnNames[i] = colNames;
			propertyColumnFormulaTemplates[i] = formulaTemplates;
			propertyColumnReaderTemplates[i] = colReaderTemplates;
			propertyColumnWriters[i] = colWriters;
			propertyColumnAliases[i] = colAliases;

			if (lazyAvailable && prop.isLazy()) {
				lazyProperties.add(prop.getName());
				lazyNames.add(prop.getName());
				lazyNumbers.add(i);
				lazyTypes.add(prop.getValue().getType());
				lazyColAliases.add(colAliases);
			}

			propertyColumnUpdateable[i] = prop.getValue().getColumnUpdateability();
			propertyColumnInsertable[i] = prop.getValue().getColumnInsertability();
			propertySelectable[i] = prop.isSelectable();
			propertyUniqueness[i] = prop.getValue().isAlternateUniqueKey();

			if (prop.isLob() && getFactory().getDialect().forceLobAsLastValue()) {
				lobProperties.add(i);
			}

			i++;

		}
		hasFormulaProperties = foundFormula;
		lazyPropertyColumnAliases = ArrayHelper.to2DStringArray(lazyColAliases);
		lazyPropertyNames = ArrayHelper.toStringArray(lazyNames);
		lazyPropertyNumbers = ArrayHelper.toIntArray(lazyNumbers);
		lazyPropertyTypes = ArrayHelper.toTypeArray(lazyTypes);

		// SUBCLASS PROPERTY CLOSURE

		ArrayList columns = new ArrayList();
		ArrayList columnsLazy = new ArrayList();
		ArrayList columnReaderTemplates = new ArrayList();
		ArrayList aliases = new ArrayList();
		ArrayList formulas = new ArrayList();
		ArrayList formulaAliases = new ArrayList();
		ArrayList formulaTemplates = new ArrayList();
		ArrayList formulasLazy = new ArrayList();
		ArrayList types = new ArrayList();
		ArrayList names = new ArrayList();
		ArrayList classes = new ArrayList();
		ArrayList templates = new ArrayList();
		ArrayList propColumns = new ArrayList();
		ArrayList propColumnReaders = new ArrayList();
		ArrayList propColumnReaderTemplates = new ArrayList();
		ArrayList joinedFetchesList = new ArrayList();
		ArrayList cascades = new ArrayList();
		ArrayList definedBySubclass = new ArrayList();
		ArrayList propColumnNumbers = new ArrayList();
		ArrayList propFormulaNumbers = new ArrayList();
		ArrayList columnSelectables = new ArrayList();
		ArrayList propNullables = new ArrayList();

		iter = persistentClass.getSubclassPropertyClosureIterator();
		while (iter.hasNext()) {
			Property prop = (Property) iter.next();
			names.add(prop.getName());
			classes.add(prop.getPersistentClass().getEntityName());
			boolean isDefinedBySubclass = !thisClassProperties.contains(prop);
			definedBySubclass.add(Boolean.valueOf(isDefinedBySubclass));
			propNullables.add(Boolean.valueOf(prop.isOptional()
					|| isDefinedBySubclass));
			types.add(prop.getType());

			Iterator colIter = prop.getColumnIterator();
			String[] cols = new String[prop.getColumnSpan()];
			String[] readers = new String[prop.getColumnSpan()];
			String[] readerTemplates = new String[prop.getColumnSpan()];
			String[] forms = new String[prop.getColumnSpan()];
			int[] colnos = new int[prop.getColumnSpan()];
			int[] formnos = new int[prop.getColumnSpan()];
			int l = 0;
			Boolean lazy = Boolean.valueOf(prop.isLazy() && lazyAvailable);
			while (colIter.hasNext()) {
				Selectable thing = (Selectable) colIter.next();
				if (thing.isFormula()) {
					String template = thing.getTemplate(factory.getDialect(),
							factory.getSqlFunctionRegistry());
					formnos[l] = formulaTemplates.size();
					colnos[l] = -1;
					formulaTemplates.add(template);
					forms[l] = template;
					formulas.add(thing.getText(factory.getDialect()));
					formulaAliases.add(thing.getAlias(factory.getDialect()));
					formulasLazy.add(lazy);
				} else {
					Column col = (Column) thing;
					String colName = col.getQuotedName(factory.getDialect());
					colnos[l] = columns.size(); // before add :-)
					formnos[l] = -1;
					columns.add(colName);
					cols[l] = colName;
					aliases.add(thing.getAlias(factory.getDialect(), prop
							.getValue().getTable()));
					columnsLazy.add(lazy);
					columnSelectables.add(Boolean.valueOf(prop.isSelectable()));

					readers[l] = col.getReadExpr(factory.getDialect());
					String readerTemplate = col.getTemplate(
							factory.getDialect(),
							factory.getSqlFunctionRegistry());
					readerTemplates[l] = readerTemplate;
					columnReaderTemplates.add(readerTemplate);
				}
				l++;
			}
			propColumns.add(cols);
			propColumnReaders.add(readers);
			propColumnReaderTemplates.add(readerTemplates);
			templates.add(forms);
			propColumnNumbers.add(colnos);
			propFormulaNumbers.add(formnos);

			joinedFetchesList.add(prop.getValue().getFetchMode());
			cascades.add(prop.getCascadeStyle());
		}
		subclassColumnClosure = ArrayHelper.toStringArray(columns);
		subclassColumnAliasClosure = ArrayHelper.toStringArray(aliases);
		subclassColumnLazyClosure = ArrayHelper.toBooleanArray(columnsLazy);
		subclassColumnSelectableClosure = ArrayHelper.toBooleanArray(columnSelectables);
		subclassColumnReaderTemplateClosure = ArrayHelper.toStringArray(columnReaderTemplates);

		subclassFormulaClosure = ArrayHelper.toStringArray(formulas);
		subclassFormulaTemplateClosure = ArrayHelper.toStringArray(formulaTemplates);
		subclassFormulaAliasClosure = ArrayHelper.toStringArray(formulaAliases);
		subclassFormulaLazyClosure = ArrayHelper.toBooleanArray(formulasLazy);
		subclassPropertyNameClosure = ArrayHelper.toStringArray(names);
		subclassPropertySubclassNameClosure = ArrayHelper.toStringArray(classes);
		subclassPropertyTypeClosure = ArrayHelper.toTypeArray(types);
		subclassPropertyNullabilityClosure = ArrayHelper.toBooleanArray(propNullables);
		subclassPropertyFormulaTemplateClosure = ArrayHelper.to2DStringArray(templates);
		subclassPropertyColumnNameClosure = ArrayHelper.to2DStringArray(propColumns);
		subclassPropertyColumnReaderClosure = ArrayHelper.to2DStringArray(propColumnReaders);
		subclassPropertyColumnReaderTemplateClosure = ArrayHelper.to2DStringArray(propColumnReaderTemplates);
		subclassPropertyColumnNumberClosure = ArrayHelper.to2DIntArray(propColumnNumbers);
		subclassPropertyFormulaNumberClosure = ArrayHelper.to2DIntArray(propFormulaNumbers);
		subclassPropertyCascadeStyleClosure = new CascadeStyle[cascades.size()];
		
		iter = cascades.iterator();
		int j = 0;
		while (iter.hasNext()) {
			subclassPropertyCascadeStyleClosure[j++] = (CascadeStyle) iter.next();
		}
		subclassPropertyFetchModeClosure = new FetchMode[joinedFetchesList.size()];
		iter = joinedFetchesList.iterator();
		j = 0;
		while (iter.hasNext()) {
			subclassPropertyFetchModeClosure[j++] = (FetchMode) iter.next();
		}

		propertyDefinedOnSubclass = new boolean[definedBySubclass.size()];
		iter = definedBySubclass.iterator();
		j = 0;
		while (iter.hasNext()) {
			propertyDefinedOnSubclass[j++] = ((Boolean) iter.next()).booleanValue();
		}

		filterHelper = new FilterHelper(persistentClass.getFilters(), factory);

		temporaryIdTableName = persistentClass.getTemporaryIdTableName();
		temporaryIdTableDDL = persistentClass.getTemporaryIdTableDDL();
	}

	private boolean isAllOrDirtyOptLocking() {
		return entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.DIRTY
				|| entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.ALL;
	}

	/**
	 * Delete an object
	 */
	public void delete(Serializable id, Object version, Object object,
			SessionImplementor session) {
		final int span = getTableSpan();
		boolean isImpliedOptimisticLocking = !entityMetamodel.isVersioned()
				&& isAllOrDirtyOptLocking();
		Object[] loadedState = null;
		if (isImpliedOptimisticLocking) {
			// need to treat this as if it where optimistic-lock="all" (dirty
			// does *not* make sense);
			// first we need to locate the "loaded" state
			//
			// Note, it potentially could be a proxy, so
			// doAfterTransactionCompletion the location the safe way...
			final EntityKey key = session.generateEntityKey(id, this);
			Object entity = session.getPersistenceContext().getEntity(key);
			if (entity != null) {
				EntityEntry entry = session.getPersistenceContext().getEntry(
						entity);
				loadedState = entry.getLoadedState();
			}
		}

		final String[] deleteStrings;
		if (isImpliedOptimisticLocking && loadedState != null) {
			// we need to utilize dynamic delete statements
			deleteStrings = generateSQLDeletStrings(loadedState);
		} else {
			// otherwise, utilize the static delete statements
			deleteStrings = getSQLDeleteStrings();
		}

		for (int j = span - 1; j >= 0; j--) {
			delete(id, version, j, object, deleteStrings[j], session,
					loadedState);
		}

	}

	private BasicBatchKey deleteBatchKey;

	protected boolean isInverseTable(int j) {
		return false;
	}

	protected boolean isDeleteCallable(int j) {
		return deleteCallable[j];
	}

	protected final OptimisticLockStyle optimisticLockStyle() {
		return entityMetamodel.getOptimisticLockStyle();
	}

	public boolean isBatchable() {
		return optimisticLockStyle() == OptimisticLockStyle.NONE
				|| (!isVersioned() && optimisticLockStyle() == OptimisticLockStyle.VERSION)
				|| getFactory().getSettings().isJdbcBatchVersionedData();
	}

	private void delete(final Serializable id, final Object version,
			final int j, final Object object, final String sql,
			final SessionImplementor session, final Object[] loadedState) {
		if (isInverseTable(j)) {
			return;
		}

		final boolean useVersion = j == 0 && isVersioned();
		final boolean callable = isDeleteCallable(j);
		final Expectation expectation = Expectations
				.appropriateExpectation(deleteResultCheckStyles[j]);
		final boolean useBatch = j == 0 && isBatchable()
				&& expectation.canBeBatched();
		if (useBatch && deleteBatchKey == null) {
			deleteBatchKey = new BasicBatchKey(getEntityName() + "#DELETE",
					expectation);
		}

		if (isTableCascadeDeleteEnabled(j)) {
			return; // EARLY EXIT!
		}

		try {
			// Render the SQL query
			PreparedStatement delete;
			int index = 1;
			if (useBatch) {
				delete = session.getTransactionCoordinator()
						.getJdbcCoordinator().getBatch(deleteBatchKey)
						.getBatchStatement(sql, callable);
			} else {
				delete = session.getTransactionCoordinator()
						.getJdbcCoordinator().getStatementPreparer()
						.prepareStatement(sql, callable);
			}

			try {

				index += expectation.prepare(delete);

				// Do the key. The key is immutable so we can use the _current_
				// object state - not necessarily
				// the state at the time the delete was issued
				getIdentifierType().nullSafeSet(delete, id, index, session);
				index += getIdentifierColumnSpan();

				if (useVersion) {
					getVersionType().nullSafeSet(delete, version, index,
							session);
				} else if (isAllOrDirtyOptLocking() && loadedState != null) {
					boolean[] versionability = getPropertyVersionability();
					Type[] types = getPropertyTypes();
					for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
						if (isPropertyOfTable(i, j) && versionability[i]) {
							// this property belongs to the table and it is not
							// specifically
							// excluded from optimistic locking by
							// optimistic-lock="false"
							boolean[] settable = types[i].toColumnNullness(
									loadedState[i], getFactory());
							types[i].nullSafeSet(delete, loadedState[i], index,
									settable, session);
							index += ArrayHelper.countTrue(settable);
						}
					}
				}

				if (useBatch) {
					session.getTransactionCoordinator().getJdbcCoordinator()
							.getBatch(deleteBatchKey).addToBatch();
				} else {
					check(delete.executeUpdate(), id, j, expectation, delete);
				}

			} catch (SQLException sqle) {
				if (useBatch) {
					session.getTransactionCoordinator().getJdbcCoordinator()
							.abortBatch();
				}
				throw sqle;
			} finally {
				if (!useBatch) {
					delete.close();
				}
			}

		} catch (SQLException sqle) {

		}

	}

	private String[] generateSQLDeletStrings(Object[] loadedState) {
		int span = getTableSpan();
		String[] deleteStrings = new String[span];
		for (int j = span - 1; j >= 0; j--) {
			Delete delete = new Delete().setTableName(getTableName(j))
					.addPrimaryKeyColumns(getKeyColumns(j));
			if (getFactory().getSettings().isCommentsEnabled()) {
				delete.setComment("delete " + getEntityName() + " [" + j + "]");
			}

			boolean[] versionability = getPropertyVersionability();
			Type[] types = getPropertyTypes();
			for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
				if (isPropertyOfTable(i, j) && versionability[i]) {
					// this property belongs to the table and it is not
					// specifically
					// excluded from optimistic locking by
					// optimistic-lock="false"
					String[] propertyColumnNames = getPropertyColumnNames(i);
					boolean[] propertyNullness = types[i].toColumnNullness(
							loadedState[i], getFactory());
					for (int k = 0; k < propertyNullness.length; k++) {
						if (propertyNullness[k]) {
							delete.addWhereFragment(propertyColumnNames[k]
									+ " = ?");
						} else {
							delete.addWhereFragment(propertyColumnNames[k]
									+ " is null");
						}
					}
				}
			}
			deleteStrings[j] = delete.toStatementString();
		}
		return deleteStrings;
	}

	public ClassMetadata getClassMetadata() {
		return this;
	}

	protected String[] getSubclassPropertyNameClosure() {
		return subclassPropertyNameClosure;
	}

	protected Type[] getSubclassPropertyTypeClosure() {
		return subclassPropertyTypeClosure;
	}

	protected String[][] getSubclassPropertyColumnNameClosure() {
		return subclassPropertyColumnNameClosure;
	}

	public String[][] getSubclassPropertyColumnReaderClosure() {
		return subclassPropertyColumnReaderClosure;
	}

	public String[][] getSubclassPropertyColumnReaderTemplateClosure() {
		return subclassPropertyColumnReaderTemplateClosure;
	}

	public String[][] getSubclassPropertyFormulaTemplateClosure() {
		return subclassPropertyFormulaTemplateClosure;
	}

	private void initOrdinaryPropertyPaths(Mapping mapping)
			throws MappingException {
		for (int i = 0; i < getSubclassPropertyNameClosure().length; i++) {
			propertyMapping.initPropertyPaths(
					getSubclassPropertyNameClosure()[i],
					getSubclassPropertyTypeClosure()[i],
					getSubclassPropertyColumnNameClosure()[i],
					getSubclassPropertyColumnReaderClosure()[i],
					getSubclassPropertyColumnReaderTemplateClosure()[i],
					getSubclassPropertyFormulaTemplateClosure()[i], mapping);
		}
	}

	public String[] getIdentifierColumnReaders() {
		return rootTableKeyColumnReaders;
	}

	public String[] getIdentifierColumnReaderTemplates() {
		return rootTableKeyColumnReaderTemplates;
	}

	private void initIdentifierPropertyPaths(Mapping mapping)
			throws MappingException {
		String idProp = getIdentifierPropertyName();
		if (idProp != null) {
			propertyMapping.initPropertyPaths(idProp, getIdentifierType(),
					getIdentifierColumnNames(), getIdentifierColumnReaders(),
					getIdentifierColumnReaderTemplates(), null, mapping);
		}
		if (entityMetamodel.getIdentifierProperty().isEmbedded()) {
			propertyMapping.initPropertyPaths(null, getIdentifierType(),
					getIdentifierColumnNames(), getIdentifierColumnReaders(),
					getIdentifierColumnReaderTemplates(), null, mapping);
		}
		if (!entityMetamodel.hasNonIdentifierPropertyNamedId()) {
			propertyMapping.initPropertyPaths(ENTITY_ID, getIdentifierType(),
					getIdentifierColumnNames(), getIdentifierColumnReaders(),
					getIdentifierColumnReaderTemplates(), null, mapping);
		}
	}

	public String getDiscriminatorColumnName() {
		return DISCRIMINATOR_ALIAS;
	}

	public String getDiscriminatorColumnReaders() {
		return DISCRIMINATOR_ALIAS;
	}

	public String getDiscriminatorColumnReaderTemplate() {
		return DISCRIMINATOR_ALIAS;
	}

	protected String getDiscriminatorFormulaTemplate() {
		return null;
	}

	private void initDiscriminatorPropertyPath(Mapping mapping)
			throws MappingException {
		propertyMapping.initPropertyPaths(ENTITY_CLASS, getDiscriminatorType(),
				new String[] { getDiscriminatorColumnName() },
				new String[] { getDiscriminatorColumnReaders() },
				new String[] { getDiscriminatorColumnReaderTemplate() },
				new String[] { getDiscriminatorFormulaTemplate() },
				getFactory());
	}

	protected void initPropertyPaths(Mapping mapping) throws MappingException {
		initOrdinaryPropertyPaths(mapping);
		initOrdinaryPropertyPaths(mapping); 
		initIdentifierPropertyPaths(mapping);
		if (entityMetamodel.isPolymorphic()) {
			initDiscriminatorPropertyPath(mapping);
		}
	}

	protected String generateInsertString(boolean[] includeProperty, int j) {
		return generateInsertString(false, includeProperty, j);
	}

	public String[] getPropertyColumnNames(int i) {
		return propertyColumnNames[i];
	}

	/**
	 * Generate the SQL that inserts a row
	 */
	protected String generateInsertString(boolean identityInsert,
			boolean[] includeProperty, int j) {

		Insert insert = new Insert(getFactory().getDialect()).setTableName(getTableName(j));

		// add normal properties
		for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {

			if (includeProperty[i] && isPropertyOfTable(i, j) && !lobProperties.contains(i)) {
				insert.addColumns(getPropertyColumnNames(i), propertyColumnInsertable[i], propertyColumnWriters[i]);
			}
		}

		// add the discriminator
		if (j == 0) {
			addDiscriminatorToInsert(insert);
		}

		// add the primary key
		if (j == 0 && identityInsert) {
			insert.addIdentityColumn(getKeyColumns(0)[0]);
		} else {
			insert.addColumns(getKeyColumns(j));
		}

		if (getFactory().getSettings().isCommentsEnabled()) {
			insert.setComment("insert " + getEntityName());
		}

		// HHH-4635
		// Oracle expects all Lob properties to be last in inserts
		// and updates. Insert them at the end.
		for (int i : lobProperties) {
			if (includeProperty[i] && isPropertyOfTable(i, j)) {
				// this property belongs on the table and is to be inserted
				insert.addColumns(getPropertyColumnNames(i),
						propertyColumnInsertable[i], propertyColumnWriters[i]);
			}
		}

		String result = insert.toStatementString();

		// append the SQL to return the generated identifier
		if (j == 0 && identityInsert && useInsertSelectIdentity()) {
			result = getFactory().getDialect().appendIdentitySelectToInsert(
					result);
		}

		return result;
	}

	@Override
	public String getMappedSuperclass() {
		return entityMetamodel.getSuperclass();
	}

	public boolean[] getPropertyLaziness() {
		return entityMetamodel.getPropertyLaziness();
	}

	@Override
	public String[] toColumns(String propertyName) throws QueryException,
			UnsupportedOperationException {
		return propertyMapping.getColumnNames(propertyName);
	}

	public Type toType(String propertyName) throws QueryException {
		return propertyMapping.toType(propertyName);
	}

	@Override
	public boolean isMultiTable() {
		return false;
	}

	protected String[] getIdentifierAliases() {
		return identifierAliases;
	}

	public String identifierSelectFragment(String name, String suffix) {
		return new SelectFragment()
				.setSuffix(suffix)
				.addColumns(name, getIdentifierColumnNames(),
						getIdentifierAliases()).toFragmentString().substring(2); // strip
																					// leading
																					// ", "
	}

	public String propertySelectFragment(String tableAlias, String suffix,
			boolean allProperties) {
		return propertySelectFragmentFragment(tableAlias, suffix, allProperties)
				.toFragmentString();
	}

	protected String[] getSubclassColumnAliasClosure() {
		return subclassColumnAliasClosure;
	}

	public String[] getSubclassColumnReaderTemplateClosure() {
		return subclassColumnReaderTemplateClosure;
	}

	protected String[] getSubclassColumnClosure() {
		return subclassColumnClosure;
	}

	protected boolean isSubclassTableSequentialSelect(int j) {
		return false;
	}

	protected boolean isSubclassTableLazy(int j) {
		return false;
	}

	protected boolean isNullableTable(int j) {
		return false;
	}

	protected JoinFragment createJoin(String name, boolean innerJoin,
			boolean includeSubclasses) {
		final String[] idCols = StringHelper.qualify(name,
				getIdentifierColumnNames()); // all joins join to the pk of the
												// driving table
		final JoinFragment join = getFactory().getDialect()
				.createOuterJoinFragment();
		final int tableSpan = getSubclassTableSpan();
		for (int j = 1; j < tableSpan; j++) { // notice that we skip the first
												// table; it is the driving
												// table!
			final boolean joinIsIncluded = isClassOrSuperclassTable(j)
					|| (includeSubclasses
							&& !isSubclassTableSequentialSelect(j) && !isSubclassTableLazy(j));
			if (joinIsIncluded) {
				join.addJoin(
						getSubclassTableName(j),
						generateTableAlias(name, j),
						idCols,
						getSubclassTableKeyColumns(j),
						innerJoin && isClassOrSuperclassTable(j)
								&& !isInverseTable(j) && !isNullableTable(j) ? JoinType.INNER_JOIN
								: // we can inner join to superclass tables (the
									// row MUST be there)
								JoinType.LEFT_OUTER_JOIN // we can never inner
															// join to subclass
															// tables
				);
			}
		}
		return join;
	}

	public static String generateTableAlias(String rootAlias, int tableNumber) {
		if (tableNumber == 0) {
			return rootAlias;
		}
		StringBuilder buf = new StringBuilder().append(rootAlias);
		if (!rootAlias.endsWith("_")) {
			buf.append('_');
		}
		return buf.append(tableNumber).append('_').toString();
	}

	public SelectFragment propertySelectFragmentFragment(String tableAlias,
			String suffix, boolean allProperties) {
		SelectFragment select = new SelectFragment().setSuffix(suffix)
				.setUsedAliases(getIdentifierAliases());

		int[] columnTableNumbers = getSubclassColumnTableNumberClosure();
		String[] columnAliases = getSubclassColumnAliasClosure();
		String[] columnReaderTemplates = getSubclassColumnReaderTemplateClosure();
		for (int i = 0; i < getSubclassColumnClosure().length; i++) {
			boolean selectable = (allProperties || !subclassColumnLazyClosure[i])
					&& !isSubclassTableSequentialSelect(columnTableNumbers[i])
					&& subclassColumnSelectableClosure[i];
			if (selectable) {
				String subalias = generateTableAlias(tableAlias,
						columnTableNumbers[i]);
				select.addColumnTemplate(subalias, columnReaderTemplates[i],
						columnAliases[i]);
			}
		}

		if (entityMetamodel.hasSubclasses()) {
			addDiscriminatorToSelect(select, tableAlias, suffix);
		}

		if (hasRowId()) {
			select.addColumn(tableAlias, rowIdName, ROWID_ALIAS);
		}

		return select;
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name,
			String suffix) {
	}

	public String selectFragment(String alias, String suffix) {
		return identifierSelectFragment(alias, suffix)
				+ propertySelectFragment(alias, suffix, false);
	}

	protected boolean[] getPropertiesToInsert(Object[] fields) {
		boolean[] notNull = new boolean[fields.length];
		boolean[] insertable = getPropertyInsertability();
		for (int i = 0; i < fields.length; i++) {
			notNull[i] = insertable[i] && fields[i] != null;
		}
		return notNull;
	}

	protected String generateInsertString(boolean identityInsert,
			boolean[] includeProperty) {
		return generateInsertString(identityInsert, includeProperty, 0);
	}

	protected int dehydrate(Serializable id, Object[] fields,
			boolean[] includeProperty, boolean[][] includeColumns, int j,
			PreparedStatement st, SessionImplementor session, boolean isUpdate)
			throws ZormException, SQLException {
		return dehydrate(id, fields, null, includeProperty, includeColumns, j,
				st, session, 1, isUpdate);
	}

	protected Serializable insert(final Object[] fields,
			final boolean[] notNull, String sql, final Object object,
			final SessionImplementor session) throws ZormException {

		Binder binder = new Binder() {
			public void bindValues(PreparedStatement ps) throws SQLException {
				dehydrate(null, fields, notNull, propertyColumnInsertable, 0,
						ps, session, false);
			}

			public Object getEntity() {
				return object;
			}
		};

		return identityDelegate.performInsert(sql, session, binder);
	}

	protected String getSQLIdentityInsertString() {
		return sqlIdentityInsertString;
	}

	public int getVersionProperty() {
		return entityMetamodel.getVersionPropertyIndex();
	}

	public boolean isAbstract() {
		return entityMetamodel.isAbstract();
	}

	public String[] getSubclassPropertyColumnNames(int i) {
		return subclassPropertyColumnNameClosure[i];
	}

	public String[] getPropertyColumnNames(String propertyName) {
		return propertyMapping.getColumnNames(propertyName);
	}

	public boolean hasCascades() {
		return entityMetamodel.hasCascades();
	}

	public int getPropertyIndex(String propertyName) {
		return entityMetamodel.getPropertyIndex(propertyName);
	}

	public boolean hasMutableProperties() {
		return entityMetamodel.hasMutableProperties();
	}

	public Type getPropertyType(String propertyName) throws MappingException {
		return propertyMapping.toType(propertyName);
	}

	public boolean isInherited() {
		return entityMetamodel.isInherited();
	}

	public boolean isSubclassEntityName(String entityName) {
		return entityMetamodel.getSubclassEntityNames().contains(entityName);
	}

	public VersionType getVersionType() {
		return (VersionType) locateVersionType();
	}

	private Type locateVersionType() {
		return entityMetamodel.getVersionProperty() == null ? null
				: entityMetamodel.getVersionProperty().getType();
	}

	public String[] toColumns(String name, final int i) {
		final String alias = generateTableAlias(name,
				getSubclassPropertyTableNumber(i));
		String[] cols = getSubclassPropertyColumnNames(i);
		String[] templates = getSubclassPropertyFormulaTemplateClosure()[i];
		String[] result = new String[cols.length];
		for (int j = 0; j < cols.length; j++) {
			if (cols[j] == null) {
				result[j] = StringHelper.replace(templates[j],
						Template.TEMPLATE, alias);
			} else {
				result[j] = StringHelper.qualify(alias, cols[j]);
			}
		}
		return result;
	}

	public int determineTableNumberForColumn(String columnName) {
		return 0;
	}

	public Serializable insert(Object[] fields, Object object,
			SessionImplementor session) throws ZormException {

		final int span = getTableSpan();
		final Serializable id;
		if (entityMetamodel.isDynamicInsert()) {
			// For the case of dynamic-insert="true", we need to generate the
			// INSERT SQL
			boolean[] notNull = getPropertiesToInsert(fields);
			id = insert(fields, notNull, generateInsertString(true, notNull),
					object, session);
			for (int j = 1; j < span; j++) {
				insert(id, fields, notNull, j,
						generateInsertString(notNull, j), object, session);
			}
		} else {
			// For the case of dynamic-insert="false", use the static SQL
			id = insert(fields, getPropertyInsertability(),
					getSQLIdentityInsertString(), object, session);
			for (int j = 1; j < span; j++) {
				insert(id, fields, getPropertyInsertability(), j,
						getSQLInsertStrings()[j], object, session);
			}
		}
		return id;
	}

	public boolean implementsLifecycle() {
		return getEntityTuplizer().isLifecycleImplementor();
	}

	public boolean hasLazyProperties() {
		return entityMetamodel.hasLazyProperties();
	}

	public boolean hasInsertGeneratedProperties() {
		return entityMetamodel.hasInsertGeneratedValues();
	}

	public boolean[] getPropertyVersionability() {
		return entityMetamodel.getPropertyVersionability();
	}

	public boolean[] getPropertyNullability() {
		return entityMetamodel.getPropertyNullability();
	}

	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		return entityMetamodel.getPropertyInsertGenerationInclusions();
	}

	public boolean isInstrumented() {
		return entityMetamodel.isInstrumented();
	}

	public boolean isSelectBeforeUpdateRequired() {
		return entityMetamodel.isSelectBeforeUpdate();
	}

	public void afterInitialize(Object entity,
			boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
		getEntityTuplizer().afterInitialize(entity, lazyPropertiesAreUnfetched,
				session);
	}

	public void processInsertGeneratedProperties(Serializable id,
			Object entity, Object[] state, SessionImplementor session) {
		if (!hasInsertGeneratedProperties()) {
			throw new AssertionFailure("no insert-generated properties");
		}
		processGeneratedProperties(id, entity, state, session,
				sqlInsertGeneratedValuesSelectString,
				getPropertyInsertGenerationInclusions());
	}

	public Boolean isTransient(Object entity, SessionImplementor session)
			throws ZormException {
		final Serializable id;
		if (canExtractIdOutOfEntity()) {
			id = getIdentifier(entity, session);
		} else {
			id = null;
		}
		// we *always* assume an instance with a null
		// identifier or no identifier property is unsaved!
		if (id == null) {
			return Boolean.TRUE;
		}

		// check the version unsaved-value, if appropriate
		final Object version = getVersion(entity);
		if (isVersioned()) {
			// let this take precedence if defined, since it works for
			// assigned identifiers
			Boolean result = entityMetamodel.getVersionProperty()
					.getUnsavedValue().isUnsaved(version);
			if (result != null) {
				return result;
			}
		}

		// check the id unsaved-value
		Boolean result = entityMetamodel.getIdentifierProperty()
				.getUnsavedValue().isUnsaved(id);
		if (result != null) {
			return result;
		}
		return null;
	}

	private void processGeneratedProperties(Serializable id, Object entity,
			Object[] state, SessionImplementor session, String selectionSQL,
			ValueInclusion[] includeds) {
		// force immediate execution of the insert batch (if one)
		session.getTransactionCoordinator().getJdbcCoordinator().executeBatch();

		try {
			PreparedStatement ps = session.getTransactionCoordinator()
					.getJdbcCoordinator().getStatementPreparer()
					.prepareStatement(selectionSQL);
			try {
				getIdentifierType().nullSafeSet(ps, id, 1, session);
				ResultSet rs = ps.executeQuery();
				try {
					if (!rs.next()) {
						throw new ZormException(
								"Unable to locate row for retrieval of generated properties: "
										+ MessageHelper.infoString(this, id,
												getFactory()));
					}
					for (int i = 0; i < getPropertySpan(); i++) {
						if (includeds[i] != ValueInclusion.NONE) {
							Object hydratedState = getPropertyTypes()[i]
									.hydrate(rs, getPropertyAliases("", i),
											session, entity);
							state[i] = getPropertyTypes()[i].resolve(
									hydratedState, session, entity);
							setPropertyValue(entity, i, state[i]);
						}
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			} finally {
				ps.close();
			}
		} catch (SQLException e) {
			throw getFactory().getSQLExceptionHelper().convert(e,
					"unable to select generated column values", selectionSQL);
		}

	}

	protected boolean useInsertSelectIdentity() {
		return !useGetGeneratedKeys()
				&& getFactory().getDialect().supportsInsertSelectIdentity();
	}

	public String fromJoinFragment(String alias, boolean innerJoin,
			boolean includeSubclasses) {
		return getSubclassTableSpan() == 1 ? "" : // just a performance opt!
				createJoin(alias, innerJoin, includeSubclasses)
						.toFromFragmentString();
	}

	protected boolean useGetGeneratedKeys() {
		return getFactory().getSettings().isGetGeneratedKeysEnabled();
	}

	protected void postConstruct(Mapping mapping) throws MappingException {
		initPropertyPaths(mapping);

		// insert/update/delete SQL
		final int joinSpan = getTableSpan();
		sqlDeleteStrings = new String[joinSpan];
		sqlInsertStrings = new String[joinSpan];
		sqlUpdateStrings = new String[joinSpan];
		sqlLazyUpdateStrings = new String[joinSpan];

		sqlUpdateByRowIdString = rowIdName == null ? null
				: generateUpdateString(getPropertyUpdateability(), 0, true);
		sqlLazyUpdateByRowIdString = rowIdName == null ? null
				: generateUpdateString(getNonLazyPropertyUpdateability(), 0,
						true);

		for (int j = 0; j < joinSpan; j++) {
			sqlInsertStrings[j] = customSQLInsert[j] == null ? generateInsertString(getPropertyInsertability(), j) 
					: customSQLInsert[j];
			sqlUpdateStrings[j] = customSQLUpdate[j] == null ? generateUpdateString(getPropertyUpdateability(), j, false)
					: customSQLUpdate[j];
			sqlLazyUpdateStrings[j] = customSQLUpdate[j] == null ? generateUpdateString(getNonLazyPropertyUpdateability(), j, false)
					: customSQLUpdate[j];
			sqlDeleteStrings[j] = customSQLDelete[j] == null ? generateDeleteString(j)
					: customSQLDelete[j];
		}

		tableHasColumns = new boolean[joinSpan];
		for (int j = 0; j < joinSpan; j++) {
			tableHasColumns[j] = sqlUpdateStrings[j] != null;
		}

		// select SQL
		sqlSnapshotSelectString = generateSnapshotSelectString();
		sqlLazySelectString = generateLazySelectString();
		// 根据ID查找
		sqlVersionSelectString = generateSelectVersionString();
		if (hasInsertGeneratedProperties()) {
			sqlInsertGeneratedValuesSelectString = generateInsertGeneratedValuesSelectString();
		}
		if (hasUpdateGeneratedProperties()) {
			sqlUpdateGeneratedValuesSelectString = generateUpdateGeneratedValuesSelectString();
		}
		if (isIdentifierAssignedByInsert()) {
			identityDelegate = ((PostInsertIdentifierGenerator) getIdentifierGenerator())
					.getInsertGeneratedIdentifierDelegate(this, getFactory()
							.getDialect(), useGetGeneratedKeys());
			sqlIdentityInsertString = customSQLInsert[0] == null ? generateIdentityInsertString(getPropertyInsertability())
					: customSQLInsert[0];
		} else {
			sqlIdentityInsertString = null;
		}

	}

	protected String generateUpdateGeneratedValuesSelectString() {
		return generateGeneratedValuesSelectString(getPropertyUpdateGenerationInclusions());
	}

	public boolean hasUpdateGeneratedProperties() {
		return entityMetamodel.hasUpdateGeneratedValues();
	}

	protected String generateInsertGeneratedValuesSelectString() {
		return generateGeneratedValuesSelectString(getPropertyInsertGenerationInclusions());
	}

	private String generateGeneratedValuesSelectString(
			ValueInclusion[] inclusions) {
		Select select = new Select(getFactory().getDialect());

		if (getFactory().getSettings().isCommentsEnabled()) {
			select.setComment("get generated state " + getEntityName());
		}

		String[] aliasedIdColumns = StringHelper.qualify(getRootAlias(),
				getIdentifierColumnNames());
		String selectClause = concretePropertySelectFragment(getRootAlias(),
				inclusions);
		selectClause = selectClause.substring(2);

		String fromClause = fromTableFragment(getRootAlias())
				+ fromJoinFragment(getRootAlias(), true, false);

		String whereClause = new StringBuilder()
				.append(StringHelper.join("=? and ", aliasedIdColumns))
				.append("=?")
				.append(whereJoinFragment(getRootAlias(), true, false))
				.toString();

		return select.setSelectClause(selectClause).setFromClause(fromClause)
				.setOuterJoins("", "").setWhereClause(whereClause)
				.toStatementString();
	}

	protected String concretePropertySelectFragment(String alias,
			final ValueInclusion[] inclusions) {
		return concretePropertySelectFragment(alias, new InclusionChecker() {
			public boolean includeProperty(int propertyNumber) {
				return inclusions[propertyNumber] != ValueInclusion.NONE;
			}
		});
	}

	protected String generateSelectVersionString() {
		SimpleSelect select = new SimpleSelect(getFactory().getDialect())
				.setTableName(getVersionedTableName());
		if (isVersioned()) {
			select.addColumn(versionColumnName);
		} else {
			select.addColumns(rootTableKeyColumnNames);
		}
		if (getFactory().getSettings().isCommentsEnabled()) {
			select.setComment("get version " + getEntityName());
		}
		return select.addCondition(rootTableKeyColumnNames, "=?")
				.toStatementString();
	}

	protected String getVersionedTableName() {
		return getTableName(0);
	}

	protected String generateLazySelectString() {

		if (!entityMetamodel.hasLazyProperties()) {
			return null;
		}

		HashSet tableNumbers = new HashSet();
		ArrayList columnNumbers = new ArrayList();
		ArrayList formulaNumbers = new ArrayList();
		for (int i = 0; i < lazyPropertyNames.length; i++) {
			int propertyNumber = getSubclassPropertyIndex(lazyPropertyNames[i]);
			int tableNumber = getSubclassPropertyTableNumber(propertyNumber);
			tableNumbers.add(tableNumber);

			int[] colNumbers = subclassPropertyColumnNumberClosure[propertyNumber];
			for (int j = 0; j < colNumbers.length; j++) {
				if (colNumbers[j] != -1) {
					columnNumbers.add(colNumbers[j]);
				}
			}
			int[] formNumbers = subclassPropertyFormulaNumberClosure[propertyNumber];
			for (int j = 0; j < formNumbers.length; j++) {
				if (formNumbers[j] != -1) {
					formulaNumbers.add(formNumbers[j]);
				}
			}
		}

		if (columnNumbers.size() == 0 && formulaNumbers.size() == 0) {
			return null;
		}

		return renderSelect(ArrayHelper.toIntArray(tableNumbers),
				ArrayHelper.toIntArray(columnNumbers),
				ArrayHelper.toIntArray(formulaNumbers));

	}

	protected String renderSelect(final int[] tableNumbers,
			final int[] columnNumbers, final int[] formulaNumbers) {

		Arrays.sort(tableNumbers);

		// render the where and from parts
		int drivingTable = tableNumbers[0];
		final String drivingAlias = generateTableAlias(getRootAlias(),
				drivingTable);
		final String where = createWhereByKey(drivingTable, drivingAlias);
		final String from = createFrom(drivingTable, drivingAlias);

		// now render the joins
		JoinFragment jf = createJoin(tableNumbers, drivingAlias);

		// now render the select clause
		SelectFragment selectFragment = createSelect(columnNumbers,
				formulaNumbers);

		// now tie it all together
		Select select = new Select(getFactory().getDialect());
		select.setSelectClause(selectFragment.toFragmentString().substring(2));
		select.setFromClause(from);
		select.setWhereClause(where);
		select.setOuterJoins(jf.toFromFragmentString(),
				jf.toWhereFragmentString());
		if (getFactory().getSettings().isCommentsEnabled()) {
			select.setComment("sequential select " + getEntityName());
		}
		return select.toStatementString();
	}

	protected SelectFragment createSelect(final int[] subclassColumnNumbers,
			final int[] subclassFormulaNumbers) {

		SelectFragment selectFragment = new SelectFragment();

		int[] columnTableNumbers = getSubclassColumnTableNumberClosure();
		String[] columnAliases = getSubclassColumnAliasClosure();
		String[] columnReaderTemplates = getSubclassColumnReaderTemplateClosure();
		for (int i = 0; i < subclassColumnNumbers.length; i++) {
			int columnNumber = subclassColumnNumbers[i];
			if (subclassColumnSelectableClosure[columnNumber]) {
				final String subalias = generateTableAlias(getRootAlias(),
						columnTableNumbers[columnNumber]);
				selectFragment.addColumnTemplate(subalias,
						columnReaderTemplates[columnNumber],
						columnAliases[columnNumber]);
			}
		}

		int[] formulaTableNumbers = getSubclassFormulaTableNumberClosure();
		String[] formulaTemplates = getSubclassFormulaTemplateClosure();
		String[] formulaAliases = getSubclassFormulaAliasClosure();
		for (int i = 0; i < subclassFormulaNumbers.length; i++) {
			int formulaNumber = subclassFormulaNumbers[i];
			final String subalias = generateTableAlias(getRootAlias(),
					formulaTableNumbers[formulaNumber]);
			selectFragment.addFormula(subalias,
					formulaTemplates[formulaNumber],
					formulaAliases[formulaNumber]);
		}

		return selectFragment;
	}
	
	protected String[] getSubclassFormulaTemplateClosure() {
		return subclassFormulaTemplateClosure;
	}
	
	protected String[] getSubclassFormulaAliasClosure() {
		return subclassFormulaAliasClosure;
	}

	protected JoinFragment createJoin(int[] tableNumbers, String drivingAlias) {
		final String[] keyCols = StringHelper.qualify(drivingAlias,
				getSubclassTableKeyColumns(tableNumbers[0]));
		final JoinFragment jf = getFactory().getDialect()
				.createOuterJoinFragment();
		for (int i = 1; i < tableNumbers.length; i++) { // skip the driving
														// table
			final int j = tableNumbers[i];
			jf.addJoin(
					getSubclassTableName(j),
					generateTableAlias(getRootAlias(), j),
					keyCols,
					getSubclassTableKeyColumns(j),
					isInverseSubclassTable(j) || isNullableSubclassTable(j) ? JoinType.LEFT_OUTER_JOIN
							: JoinType.INNER_JOIN);
		}
		return jf;
	}

	protected boolean isInverseSubclassTable(int j) {
		return false;
	}

	protected boolean isNullableSubclassTable(int j) {
		return false;
	}

	protected String createFrom(int tableNumber, String alias) {
		return getSubclassTableName(tableNumber) + ' ' + alias;
	}

	protected String createWhereByKey(int tableNumber, String alias) {
		return StringHelper.join("=? and ", StringHelper.qualify(alias,
				getSubclassTableKeyColumns(tableNumber)))
				+ "=?";
	}

	protected String generateSnapshotSelectString() {

		Select select = new Select(getFactory().getDialect());

		if (getFactory().getSettings().isCommentsEnabled()) {
			select.setComment("get current state " + getEntityName());
		}

		String[] aliasedIdColumns = StringHelper.qualify(getRootAlias(),
				getIdentifierColumnNames());
		String selectClause = StringHelper.join(", ", aliasedIdColumns)
				+ concretePropertySelectFragment(getRootAlias(),
						getPropertyUpdateability());

		String fromClause = fromTableFragment(getRootAlias())
				+ fromJoinFragment(getRootAlias(), true, false);

		String whereClause = new StringBuilder()
				.append(StringHelper.join("=? and ", aliasedIdColumns))
				.append("=?")
				.append(whereJoinFragment(getRootAlias(), true, false))
				.toString();

		return select.setSelectClause(selectClause).setFromClause(fromClause)
				.setOuterJoins("", "").setWhereClause(whereClause)
				.toStatementString();
	}

	protected String concretePropertySelectFragment(String alias,
			final boolean[] includeProperty) {
		return concretePropertySelectFragment(alias, new InclusionChecker() {
			public boolean includeProperty(int propertyNumber) {
				return includeProperty[propertyNumber];
			}
		});
	}

	protected static interface InclusionChecker {
		public boolean includeProperty(int propertyNumber);
	}

	protected String concretePropertySelectFragment(String alias,
			InclusionChecker inclusionChecker) {
		int propertyCount = getPropertyNames().length;
		int[] propertyTableNumbers = getPropertyTableNumbersInSelect();
		SelectFragment frag = new SelectFragment();
		for (int i = 0; i < propertyCount; i++) {
			if (inclusionChecker.includeProperty(i)) {
				frag.addColumnTemplates(
						generateTableAlias(alias, propertyTableNumbers[i]),
						propertyColumnReaderTemplates[i],
						propertyColumnAliases[i]);
				frag.addFormulas(
						generateTableAlias(alias, propertyTableNumbers[i]),
						propertyColumnFormulaTemplates[i],
						propertyColumnAliases[i]);
			}
		}
		return frag.toFragmentString();
	}

	protected String generateUpdateString(boolean[] includeProperty, int j,
			boolean useRowId) {
		return generateUpdateString(includeProperty, j, null, useRowId);
	}

	protected String generateDeleteString(int j) {
		Delete delete = new Delete().setTableName(getTableName(j))
				.addPrimaryKeyColumns(getKeyColumns(j));
		if (j == 0) {
			delete.setVersionColumnName(getVersionColumnName());
		}
		if (getFactory().getSettings().isCommentsEnabled()) {
			delete.setComment("delete " + getEntityName());
		}
		return delete.toStatementString();
	}

	private String getVersionColumnName() {
		return versionColumnName;
	}

	public boolean isIdentifierAssignedByInsert() {
		return entityMetamodel.getIdentifierProperty()
				.isIdentifierAssignedByInsert();
	}

	protected String generateIdentityInsertString(boolean[] includeProperty) {
		Insert insert = identityDelegate.prepareIdentifierGeneratingInsert();
		insert.setTableName(getTableName(0));

		// add normal properties
		for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
			if (includeProperty[i] && isPropertyOfTable(i, 0)) {
				// this property belongs on the table and is to be inserted
				insert.addColumns(getPropertyColumnNames(i),
						propertyColumnInsertable[i], propertyColumnWriters[i]);
			}
		}

		// add the discriminator
		addDiscriminatorToInsert(insert);

		// delegate already handles PK columns

		if (getFactory().getSettings().isCommentsEnabled()) {
			insert.setComment("insert " + getEntityName());
		}

		return insert.toStatementString();
	}

	@Override
	public void insert(Serializable id, Object[] fields, Object object,
			SessionImplementor session) {

		final int span = getTableSpan();
		if (entityMetamodel.isDynamicInsert()) {
		} else {
			for (int j = 0; j < span; j++) {
				insert(id, fields, getPropertyInsertability(), j,getSQLInsertStrings()[j], object, session);
			}
		}
	}

	public boolean hasNaturalIdentifier() {
		return entityMetamodel.hasNaturalIdentifier();
	}

	@Override
	public Object load(Serializable id, Object optionalObject,
			LockOptions lockOptions, SessionImplementor session) {
		final UniqueEntityLoader loader = getAppropriateLoader(lockOptions,session);
		return loader.load(id, optionalObject, session, lockOptions);
	}

	private UniqueEntityLoader getAppropriateLoader(LockOptions lockOptions,
			SessionImplementor session) {
		return (UniqueEntityLoader) getLoaders().get(lockOptions.getLockMode());
	}

	protected Map getLoaders() {
		return loaders;
	}

	private BasicBatchKey inserBatchKey;

	protected void insert(final Serializable id, final Object[] fields,
			final boolean[] notNull, final int j, final String sql,
			final Object object, final SessionImplementor session) {
		final Expectation expectation = Expectations
				.appropriateExpectation(insertResultCheckStyles[j]);
		final boolean useBatch = j == 0 && expectation.canBeBatched();
		if (useBatch && inserBatchKey == null) {
			inserBatchKey = new BasicBatchKey(getEntityName() + "#INSERT",
					expectation);
		}

		final boolean callable = isInsertCallable(j);
		try {
			final PreparedStatement insert;
			if (useBatch) {
				insert = session.getTransactionCoordinator()
						.getJdbcCoordinator().getBatch(inserBatchKey)
						.getBatchStatement(sql, callable);
			} else {
				insert = session.getTransactionCoordinator()
						.getJdbcCoordinator().getStatementPreparer()
						.prepareStatement(sql, callable);
			}

			try {
				int index = 1;
				index += expectation.prepare(insert);

				dehydrate(id, fields, null, notNull, propertyColumnInsertable,
						j, insert, session, index, false);

				if (useBatch) {
					session.getTransactionCoordinator()
					        .getJdbcCoordinator()
							.getBatch(inserBatchKey)
							.addToBatch();
				} else {
					expectation.verifyOutcome(insert.executeUpdate(), insert,
							-1);
				}

			} catch (SQLException e) {
				if (useBatch) {
					session.getTransactionCoordinator().getJdbcCoordinator()
							.abortBatch();
				}
				throw e;
			} finally {
				if (!useBatch) {
					insert.close();
				}
			}
		} catch (SQLException e) {
		}

	}

	protected int dehydrate(final Serializable id, final Object[] fields,
			final Object rowId, final boolean[] includeProperty,
			final boolean[][] includeColumns, final int j,
			final PreparedStatement ps, final SessionImplementor session,
			int index, boolean isUpdate) throws SQLException, ZormException {

		for (int i = 0; i < entityMetamodel.getPropertySpan(); i++) {
			if (includeProperty[i] && isPropertyOfTable(i, j)
					&& !lobProperties.contains(i)) {
				getPropertyTypes()[i].nullSafeSet(ps, fields[i], index,
						includeColumns[i], session);
				index += ArrayHelper.countTrue(includeColumns[i]);
			}
		}

		if (!isUpdate) {
			index += dehydrateId(id, rowId, ps, session, index);
		}

		for (int i : lobProperties) {
			if (includeProperty[i] && isPropertyOfTable(i, j)) {
				getPropertyTypes()[i].nullSafeSet(ps, fields[i], index,
						includeColumns[i], session);
				index += ArrayHelper.countTrue(includeColumns[i]);
			}
		}

		if (isUpdate) {
			index += dehydrateId(id, rowId, ps, session, index);
		}

		return index;

	}

	protected String[] getSubclassFormulaClosure() {
		return subclassFormulaClosure;
	}

	@Override
	public EntityInstrumentationMetadata getInstrumentationMetadata() {
		return entityMetamodel.getInstrumentationMetadata();
	}

	private int dehydrateId(final Serializable id, final Object rowId,
			final PreparedStatement ps, final SessionImplementor session,
			int index) throws SQLException {
		if (rowId != null) {
			ps.setObject(index, rowId);
			return 1;
		} else if (id != null) {
			getIdentifierType().nullSafeSet(ps, id, index, session);
			return getIdentifierColumnSpan();
		}
		return 0;
	}

	protected int getIdentifierColumnSpan() {
		return identifierColumnSpan;
	}

	protected boolean isInsertCallable(int j) {
		return insertCallable[j];
	}

	private String[] getSQLInsertStrings() {
		return sqlInsertStrings;
	}

	protected String[] getSQLDeleteStrings() {
		return sqlDeleteStrings;
	}

	public boolean[] getPropertyInsertability() {
		return entityMetamodel.getPropertyInsertability();
	}

	protected abstract int getTableSpan();

	public boolean isMutable() {
		return entityMetamodel.isMutable();
	}

	public Type[] getPropertyTypes() {
		return entityMetamodel.getPropertyTypes();
	}

	@Override
	public Object getVersion(Object object) {
		return getEntityTuplizer().getVersion(object);
	}

	public int[] getNaturalIdentifierProperties() {
		return entityMetamodel.getNaturalIdentifierProperties();
	}

	public Object[] getPropertyValues(Object object) {
		return getEntityTuplizer().getPropertyValues(object);
	}

	public boolean[] getPropertyUpdateability() {
		return entityMetamodel.getPropertyUpdateability();
	}

	public EntityTuplizer getEntityTuplizer() {
		return entityTuplizer;
	}

	@Override
	public Serializable getIdentifier(Object object) {
		return getEntityTuplizer().getIdentifier(object, null);
	}

	public Serializable getIdentifier(Object entity, SessionImplementor session) {
		return getEntityTuplizer().getIdentifier(entity, session);
	}

	public boolean isVersioned() {
		return entityMetamodel.isVersioned();
	}

	public IdentifierGenerator getIdentifierGenerator() throws ZormException {
		return entityMetamodel.getIdentifierProperty().getIdentifierGenerator();
	}

	public String getRootEntityName() {
		return entityMetamodel.getRootName();
	}

	public boolean isBatchLoadable() {
		return batchSize > 1;
	}

	@Override
	public EntityMode getEntityMode() {
		return entityMetamodel.getEntityMode();
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap,
			SessionImplementor session) {
		return getEntityTuplizer().getPropertyValuesToInsert(entity, mergeMap,
				session);
	}

	public boolean canExtractIdOutOfEntity() {
		return hasIdentifierProperty() || hasIdentifierMapper();
	}

	public boolean hasIdentifierProperty() {
		return !entityMetamodel.getIdentifierProperty().isVirtual();
	}

	private boolean hasIdentifierMapper() {
		return entityMetamodel.getIdentifierProperty().hasIdentifierMapper();
	}

	public String[] getIdentifierColumnNames() {
		return rootTableKeyColumnNames;
	}

	protected int getPropertySpan() {
		return entityMetamodel.getPropertySpan();
	}

	protected boolean hasWhere() {
		return sqlWhereString != null;
	}

	public String[] getKeyColumnNames() {
		return getIdentifierColumnNames();
	}

	public int getSubclassPropertyTableNumber(String propertyPath) {
		String rootPropertyName = StringHelper.root(propertyPath);
		Type type = propertyMapping.toType(rootPropertyName);
		int index = ArrayHelper.indexOf(getSubclassPropertyNameClosure(),
				rootPropertyName);
		return index == -1 ? 0 : getSubclassPropertyTableNumber(index);
	}

	@Override
	public Declarer getSubclassPropertyDeclarer(String propertyPath) {
		int tableIndex = getSubclassPropertyTableNumber(propertyPath);
		if (tableIndex == 0) {
			return Declarer.CLASS;
		} else if (isClassOrSuperclassTable(tableIndex)) {
			return Declarer.SUPERCLASS;
		} else {
			return Declarer.SUBCLASS;
		}
	}

	public CascadeStyle[] getPropertyCascadeStyles() {
		return entityMetamodel.getCascadeStyles();
	}

	public int[] findModified(Object[] old, Object[] current, Object entity,
			SessionImplementor session) throws ZormException {
		int[] props = TypeHelper.findModified(entityMetamodel.getProperties(),
				current, old, propertyColumnUpdateable,
				hasUninitializedLazyProperties(entity), session);
		if (props == null) {
			return null;
		} else {
			return props;
		}
	}

	public static int getTableId(String tableName, String[] tables) {
		for (int j = 0; j < tables.length; j++) {
			if (tableName.equalsIgnoreCase(tables[j])) {
				return j;
			}
		}
		throw new AssertionFailure("Table " + tableName + " not found");
	}

	@Override
	public EntityPersister getSubclassEntityPersister(Object instance,
			SessionFactoryImplementor factory) {
		if (!hasSubclasses()) {
			return this;
		} else {
			final String concreteEntityName = getEntityTuplizer()
					.determineConcreteSubclassEntityName(instance, factory);
			if (concreteEntityName == null
					|| getEntityName().equals(concreteEntityName)) {
				// the contract of
				// EntityTuplizer.determineConcreteSubclassEntityName says that
				// returning null
				// is an indication that the specified entity-name
				// (this.getEntityName) should be used.
				return this;
			} else {
				return factory.getEntityPersister(concreteEntityName);
			}
		}
	}

	public Object[] getDatabaseSnapshot(Serializable id,
			SessionImplementor session) throws ZormException {

		try {
			PreparedStatement ps = session.getTransactionCoordinator()
					.getJdbcCoordinator().getStatementPreparer()
					.prepareStatement(getSQLSnapshotSelectString());
			try {
				getIdentifierType().nullSafeSet(ps, id, 1, session);
				ResultSet rs = ps.executeQuery();
				try {
					if (!rs.next()) {
						return null;
					}
					Type[] types = getPropertyTypes();
					Object[] values = new Object[types.length];
					boolean[] includeProperty = getPropertyUpdateability();
					for (int i = 0; i < types.length; i++) {
						if (includeProperty[i]) {
							values[i] = types[i].hydrate(rs,
									getPropertyAliases("", i), session, null); 
						}
					}
					return values;
				} finally {
					rs.close();
				}
			} finally {
				ps.close();
			}
		} catch (SQLException e) {
			throw getFactory().getSQLExceptionHelper().convert(
					e,
					"could not retrieve snapshot: "
							+ MessageHelper.infoString(this, id, getFactory()),
					getSQLSnapshotSelectString());
		}

	}

}

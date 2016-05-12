package com.zorm.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zorm.DynamicFilterAliasGenerator;
import com.zorm.FilterAliasGenerator;
import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.engine.Mapping;
import com.zorm.engine.OptimisticLockStyle;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.exception.ZormException;
import com.zorm.mapping.Column;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.Selectable;
import com.zorm.mapping.Subclass;
import com.zorm.mapping.Table;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.CaseFragment;
import com.zorm.sql.SelectFragment;
import com.zorm.type.StandardBasicTypes;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class JoinedSubclassEntityPersister extends AbstractEntityPersister {
	private final int tableSpan;
	private final String[] tableNames;
	private final String[] naturalOrderTableNames;
	private final String[][] tableKeyColumns;
	private final String[][] tableKeyColumnReaders;
	private final String[][] tableKeyColumnReaderTemplates;
	private final String[][] naturalOrderTableKeyColumns;
	private final String[][] naturalOrderTableKeyColumnReaders;
	private final String[][] naturalOrderTableKeyColumnReaderTemplates;
	// private final boolean[] naturalOrderCascadeDeleteEnabled;

	private final String[] spaces;

	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;

	// properties of this class, including inherited properties
	private final int[] naturalOrderPropertyTableNumbers;
	private final int[] propertyTableNumbers;

	// the closure of all properties in the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassColumnTableNumberClosure;
	private final int[] subclassFormulaTableNumberClosure;

	private final boolean[] subclassTableSequentialSelect;
	private final boolean[] subclassTableIsLazyClosure;

	// subclass discrimination works by assigning particular
	// values to certain combinations of null primary key
	// values in the outer join using an SQL CASE
	private final Map subclassesByDiscriminatorValue = new HashMap();
	private final String[] discriminatorValues;
	private final String[] notNullColumnNames;
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	private final Object discriminatorValue;
	private final String discriminatorSQLString;

	private final int coreTableSpan;
	private final boolean[] isNullableTable;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JoinedSubclassEntityPersister(final PersistentClass persistentClass,
			final SessionFactoryImplementor factory, final Mapping mapping)
			throws ZormException {
		super(persistentClass, factory);

		// DISCRIMINATOR
		if (persistentClass.isPolymorphic()) {
			try {
				discriminatorValue = persistentClass.getSubclassId();
				discriminatorSQLString = discriminatorValue.toString();
			} catch (Exception e) {
				throw new MappingException(
						"Could not format discriminator value to SQL string", e);
			}
		} else {
			discriminatorValue = null;
			discriminatorSQLString = null;
		}

		if (optimisticLockStyle() == OptimisticLockStyle.ALL
				|| optimisticLockStyle() == OptimisticLockStyle.DIRTY) {
			throw new MappingException(
					"optimistic-lock=all|dirty not supported for joined-subclass mappings ["
							+ getEntityName() + "]");
		}

		// MULTITABLES

		final int idColumnSpan = getIdentifierColumnSpan();

		ArrayList tables = new ArrayList();
		ArrayList keyColumns = new ArrayList();
		ArrayList keyColumnReaders = new ArrayList();
		ArrayList keyColumnReaderTemplates = new ArrayList();
		Iterator titer = persistentClass.getTableClosureIterator();
		Iterator kiter = persistentClass.getKeyClosureIterator();
		while (titer.hasNext()) {
			Table tab = (Table) titer.next();
			KeyValue key = (KeyValue) kiter.next();
			String tabname = tab.getQualifiedName(factory.getDialect(), factory
					.getSettings().getDefaultCatalogName(), factory
					.getSettings().getDefaultSchemaName());
			tables.add(tabname);
			String[] keyCols = new String[idColumnSpan];
			String[] keyColReaders = new String[idColumnSpan];
			String[] keyColReaderTemplates = new String[idColumnSpan];
			Iterator citer = key.getColumnIterator();
			for (int k = 0; k < idColumnSpan; k++) {
				Column column = (Column) citer.next();
				keyCols[k] = column.getQuotedName(factory.getDialect());
				keyColReaders[k] = column.getReadExpr(factory.getDialect());
				keyColReaderTemplates[k] = column.getTemplate(
						factory.getDialect(), factory.getSqlFunctionRegistry());
			}
			keyColumns.add(keyCols);
			keyColumnReaders.add(keyColReaders);
			keyColumnReaderTemplates.add(keyColReaderTemplates);
		}

		coreTableSpan = tables.size();

		isNullableTable = new boolean[persistentClass.getJoinClosureSpan()];

		Iterator joinIter = persistentClass.getJoinClosureIterator();
		while (joinIter.hasNext()) {
		}

		naturalOrderTableNames = ArrayHelper.toStringArray(tables);
		naturalOrderTableKeyColumns = ArrayHelper.to2DStringArray(keyColumns);
		naturalOrderTableKeyColumnReaders = ArrayHelper
				.to2DStringArray(keyColumnReaders);
		naturalOrderTableKeyColumnReaderTemplates = ArrayHelper
				.to2DStringArray(keyColumnReaderTemplates);

		ArrayList subtables = new ArrayList();
		ArrayList isConcretes = new ArrayList();
		ArrayList isDeferreds = new ArrayList();
		ArrayList isLazies = new ArrayList();

		keyColumns = new ArrayList();
		titer = persistentClass.getSubclassTableClosureIterator();
		while (titer.hasNext()) {
			Table tab = (Table) titer.next();
			isConcretes.add(persistentClass.isClassOrSuperclassTable(tab));
			isDeferreds.add(Boolean.FALSE);
			isLazies.add(Boolean.FALSE);
			String tabname = tab.getQualifiedName(factory.getDialect(), factory
					.getSettings().getDefaultCatalogName(), factory
					.getSettings().getDefaultSchemaName());
			subtables.add(tabname);
			String[] key = new String[idColumnSpan];
			Iterator citer = tab.getPrimaryKey().getColumnIterator();
			for (int k = 0; k < idColumnSpan; k++) {
				key[k] = ((Column) citer.next()).getQuotedName(factory
						.getDialect());
			}
			keyColumns.add(key);
		}

		// Add joins
		joinIter = persistentClass.getSubclassJoinClosureIterator();
		while (joinIter.hasNext()) {
		}

		String[] naturalOrderSubclassTableNameClosure = ArrayHelper
				.toStringArray(subtables);
		String[][] naturalOrderSubclassTableKeyColumnClosure = ArrayHelper
				.to2DStringArray(keyColumns);
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray(isConcretes);
		subclassTableSequentialSelect = ArrayHelper.toBooleanArray(isDeferreds);
		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray(isLazies);

		constraintOrderedTableNames = new String[naturalOrderSubclassTableNameClosure.length];
		constraintOrderedKeyColumnNames = new String[naturalOrderSubclassTableNameClosure.length][];
		int currentPosition = 0;
		for (int i = naturalOrderSubclassTableNameClosure.length - 1; i >= 0; i--, currentPosition++) {
			constraintOrderedTableNames[currentPosition] = naturalOrderSubclassTableNameClosure[i];
			constraintOrderedKeyColumnNames[currentPosition] = naturalOrderSubclassTableKeyColumnClosure[i];
		}

		tableSpan = naturalOrderTableNames.length;
		tableNames = reverse(naturalOrderTableNames, coreTableSpan);
		tableKeyColumns = reverse(naturalOrderTableKeyColumns, coreTableSpan);
		tableKeyColumnReaders = reverse(naturalOrderTableKeyColumnReaders,
				coreTableSpan);
		tableKeyColumnReaderTemplates = reverse(
				naturalOrderTableKeyColumnReaderTemplates, coreTableSpan);
		subclassTableNameClosure = reverse(
				naturalOrderSubclassTableNameClosure, coreTableSpan);
		subclassTableKeyColumnClosure = reverse(
				naturalOrderSubclassTableKeyColumnClosure, coreTableSpan);

		spaces = ArrayHelper.join(tableNames, ArrayHelper
				.toStringArray(persistentClass.getSynchronizedTables()));

		// Custom sql
		customSQLInsert = new String[tableSpan];
		customSQLUpdate = new String[tableSpan];
		customSQLDelete = new String[tableSpan];
		insertCallable = new boolean[tableSpan];
		updateCallable = new boolean[tableSpan];
		deleteCallable = new boolean[tableSpan];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];

		PersistentClass pc = persistentClass;
		int jk = coreTableSpan - 1;
		while (pc != null) {
			customSQLInsert[jk] = pc.getCustomSQLInsert();
			insertCallable[jk] = customSQLInsert[jk] != null
					&& pc.isCustomInsertCallable();
			insertResultCheckStyles[jk] = pc.getCustomSQLInsertCheckStyle() == null ? ExecuteUpdateResultCheckStyle
					.determineDefault(customSQLInsert[jk], insertCallable[jk])
					: pc.getCustomSQLInsertCheckStyle();
			customSQLUpdate[jk] = pc.getCustomSQLUpdate();
			updateCallable[jk] = customSQLUpdate[jk] != null
					&& pc.isCustomUpdateCallable();
			updateResultCheckStyles[jk] = pc.getCustomSQLUpdateCheckStyle() == null ? ExecuteUpdateResultCheckStyle
					.determineDefault(customSQLUpdate[jk], updateCallable[jk])
					: pc.getCustomSQLUpdateCheckStyle();
			customSQLDelete[jk] = pc.getCustomSQLDelete();
			deleteCallable[jk] = customSQLDelete[jk] != null
					&& pc.isCustomDeleteCallable();
			deleteResultCheckStyles[jk] = pc.getCustomSQLDeleteCheckStyle() == null ? ExecuteUpdateResultCheckStyle
					.determineDefault(customSQLDelete[jk], deleteCallable[jk])
					: pc.getCustomSQLDeleteCheckStyle();
			jk--;
			pc = pc.getSuperclass();
		}

		if (jk != -1) {
			throw new AssertionFailure(
					"Tablespan does not match height of joined-subclass hiearchy.");
		}

		joinIter = persistentClass.getJoinClosureIterator();
		while (joinIter.hasNext()) {
		}

		// PROPERTIES
		int hydrateSpan = getPropertySpan();
		naturalOrderPropertyTableNumbers = new int[hydrateSpan];
		propertyTableNumbers = new int[hydrateSpan];
		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		while (iter.hasNext()) {
			Property prop = (Property) iter.next();
			String tabname = prop
					.getValue()
					.getTable()
					.getQualifiedName(factory.getDialect(),
							factory.getSettings().getDefaultCatalogName(),
							factory.getSettings().getDefaultSchemaName());
			propertyTableNumbers[i] = getTableId(tabname, tableNames);
			naturalOrderPropertyTableNumbers[i] = getTableId(tabname,
					naturalOrderTableNames);
			i++;
		}

		// subclass closure properties

		ArrayList columnTableNumbers = new ArrayList();
		ArrayList formulaTableNumbers = new ArrayList();
		ArrayList propTableNumbers = new ArrayList();

		iter = persistentClass.getSubclassPropertyClosureIterator();
		while (iter.hasNext()) {
			Property prop = (Property) iter.next();
			Table tab = prop.getValue().getTable();
			String tabname = tab.getQualifiedName(factory.getDialect(), factory
					.getSettings().getDefaultCatalogName(), factory
					.getSettings().getDefaultSchemaName());
			Integer tabnum = getTableId(tabname, subclassTableNameClosure);
			propTableNumbers.add(tabnum);

			Iterator citer = prop.getColumnIterator();
			while (citer.hasNext()) {
				Selectable thing = (Selectable) citer.next();
				if (thing.isFormula()) {
					formulaTableNumbers.add(tabnum);
				} else {
					columnTableNumbers.add(tabnum);
				}
			}

		}

		subclassColumnTableNumberClosure = ArrayHelper
				.toIntArray(columnTableNumbers);
		subclassPropertyTableNumberClosure = ArrayHelper
				.toIntArray(propTableNumbers);
		subclassFormulaTableNumberClosure = ArrayHelper
				.toIntArray(formulaTableNumbers);

		// SUBCLASSES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[subclassSpan - 1] = getEntityName();
		if (persistentClass.isPolymorphic()) {
			subclassesByDiscriminatorValue.put(discriminatorValue,
					getEntityName());
			discriminatorValues = new String[subclassSpan];
			discriminatorValues[subclassSpan - 1] = discriminatorSQLString;
			notNullColumnTableNumbers = new int[subclassSpan];
			final int id = getTableId(
					persistentClass.getTable().getQualifiedName(
							factory.getDialect(),
							factory.getSettings().getDefaultCatalogName(),
							factory.getSettings().getDefaultSchemaName()),
					subclassTableNameClosure);
			notNullColumnTableNumbers[subclassSpan - 1] = id;
			notNullColumnNames = new String[subclassSpan];
			notNullColumnNames[subclassSpan - 1] = subclassTableKeyColumnClosure[id][0]; 
		} else {
			discriminatorValues = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
		}

		iter = persistentClass.getSubclassIterator();
		int k = 0;
		while (iter.hasNext()) {
			Subclass sc = (Subclass) iter.next();
			subclassClosure[k] = sc.getEntityName();
			try {
				if (persistentClass.isPolymorphic()) {
					Integer subclassId = sc.getSubclassId();
					subclassesByDiscriminatorValue.put(subclassId,
							sc.getEntityName());
					discriminatorValues[k] = subclassId.toString();
					int id = getTableId(
							sc.getTable().getQualifiedName(
									factory.getDialect(),
									factory.getSettings()
											.getDefaultCatalogName(),
									factory.getSettings()
											.getDefaultSchemaName()),
							subclassTableNameClosure);
					notNullColumnTableNumbers[k] = id;
					notNullColumnNames[k] = subclassTableKeyColumnClosure[id][0]; // (
																					// (Column)
																					// sc.getTable().getPrimaryKey().getColumnIterator().next()
																					// ).getName();
				}
			} catch (Exception e) {
				throw new MappingException("Error parsing discriminator value",
						e);
			}
			k++;
		}


		initSubclassPropertyAliasesMap(persistentClass);

		postConstruct(mapping);

	}

	protected boolean isNullableTable(int j) {
		if (j < coreTableSpan) {
			return false;
		}
		return isNullableTable[j - coreTableSpan];
	}

	protected boolean isSubclassTableSequentialSelect(int j) {
		return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
	}

	public String getSubclassPropertyTableName(int i) {
		return subclassTableNameClosure[subclassPropertyTableNumberClosure[i]];
	}

	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLString;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassesByDiscriminatorValue.get(value);
	}

	public Serializable[] getPropertySpaces() {
		return spaces; 
	}

	protected String getTableName(int j) {
		return naturalOrderTableNames[j];
	}

	protected String[] getKeyColumns(int j) {
		return naturalOrderTableKeyColumns[j];
	}

	protected boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	protected boolean isPropertyOfTable(int property, int j) {
		return naturalOrderPropertyTableNumbers[property] == j;
	}

	private static final void reverse(Object[] objects, int len) {
		Object[] temp = new Object[len];
		for (int i = 0; i < len; i++) {
			temp[i] = objects[len - i - 1];
		}
		for (int i = 0; i < len; i++) {
			objects[i] = temp[i];
		}
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @param objects
	 * @param n
	 *
	 * @return New array with the first n elements in reversed order
	 */
	private static String[] reverse(String[] objects, int n) {

		int size = objects.length;
		String[] temp = new String[size];

		for (int i = 0; i < n; i++) {
			temp[i] = objects[n - i - 1];
		}

		for (int i = n; i < size; i++) {
			temp[i] = objects[i];
		}

		return temp;
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @param objects
	 * @param n
	 *
	 * @return New array with the first n elements in reversed order
	 */
	private static String[][] reverse(String[][] objects, int n) {
		int size = objects.length;
		String[][] temp = new String[size][];
		for (int i = 0; i < n; i++) {
			temp[i] = objects[n - i - 1];
		}

		for (int i = n; i < size; i++) {
			temp[i] = objects[i];
		}

		return temp;
	}

	public String fromTableFragment(String alias) {
		return getTableName() + ' ' + alias;
	}

	public String getTableName() {
		return tableNames[0];
	}

	public void addDiscriminatorToSelect(SelectFragment select, String name,
			String suffix) {
		if (hasSubclasses()) {
			select.setExtraSelectList(discriminatorFragment(name),
					getDiscriminatorAlias());
		}
	}

	private CaseFragment discriminatorFragment(String alias) {
		CaseFragment cases = getFactory().getDialect().createCaseFragment();

		for (int i = 0; i < discriminatorValues.length; i++) {
			cases.addWhenColumnNotNull(
					generateTableAlias(alias, notNullColumnTableNumbers[i]),
					notNullColumnNames[i], discriminatorValues[i]);
		}

		return cases;
	}

	public String filterFragment(String alias) {
		return hasWhere() ? " and "
				+ getSQLWhereString(generateFilterConditionAlias(alias)) : "";
	}

	public String generateFilterConditionAlias(String rootAlias) {
		return generateTableAlias(rootAlias, tableSpan - 1);
	}

	public String[] getIdentifierColumnNames() {
		return tableKeyColumns[0];
	}

	public String[] getIdentifierColumnReaderTemplates() {
		return tableKeyColumnReaderTemplates[0];
	}

	public String[] getIdentifierColumnReaders() {
		return tableKeyColumnReaders[0];
	}

	public String[] toColumns(String alias, String propertyName)
			throws QueryException {
		if (ENTITY_CLASS.equals(propertyName)) {
			// This doesn't actually seem to work but it *might*
			// work on some dbs. Also it doesn't work if there
			// are multiple columns of results because it
			// is not accounting for the suffix:
			// return new String[] { getDiscriminatorColumnName() };

			return new String[] { discriminatorFragment(alias)
					.toFragmentString() };
		} else {
			return super.toColumns(alias, propertyName);
		}
	}

	protected int[] getPropertyTableNumbersInSelect() {
		return propertyTableNumbers;
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return subclassPropertyTableNumberClosure[i];
	}

	public int getTableSpan() {
		return tableSpan;
	}

	public boolean isMultiTable() {
		return true;
	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return subclassColumnTableNumberClosure;
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return subclassFormulaTableNumberClosure;
	}

	protected int[] getPropertyTableNumbers() {
		return naturalOrderPropertyTableNumbers;
	}

	protected String[] getSubclassTableKeyColumns(int j) {
		return subclassTableKeyColumnClosure[j];
	}

	public String getSubclassTableName(int j) {
		return subclassTableNameClosure[j];
	}

	public int getSubclassTableSpan() {
		return subclassTableNameClosure.length;
	}

	protected boolean isSubclassTableLazy(int j) {
		return subclassTableIsLazyClosure[j];
	}

	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

	public String getPropertyTableName(String propertyName) {
		Integer index = getEntityMetamodel().getPropertyIndexOrNull(
				propertyName);
		if (index == null) {
			return null;
		}
		return tableNames[propertyTableNumbers[index.intValue()]];
	}

	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	public String getRootTableName() {
		return naturalOrderTableNames[0];
	}

	public String getRootTableAlias(String drivingAlias) {
		return generateTableAlias(drivingAlias,
				getTableId(getRootTableName(), tableNames));
	}

	public Declarer getSubclassPropertyDeclarer(String propertyPath) {
		if ("class".equals(propertyPath)) {
			// special case where we need to force include all subclass joins
			return Declarer.SUBCLASS;
		}
		return super.getSubclassPropertyDeclarer(propertyPath);
	}

	@Override
	public int determineTableNumberForColumn(String columnName) {
		final String[] subclassColumnNameClosure = getSubclassColumnClosure();
		for (int i = 0, max = subclassColumnNameClosure.length; i < max; i++) {
			final boolean quoted = subclassColumnNameClosure[i]
					.startsWith("\"")
					&& subclassColumnNameClosure[i].endsWith("\"");
			if (quoted) {
				if (subclassColumnNameClosure[i].equals(columnName)) {
					return getSubclassColumnTableNumberClosure()[i];
				}
			} else {
				if (subclassColumnNameClosure[i].equalsIgnoreCase(columnName)) {
					return getSubclassColumnTableNumberClosure()[i];
				}
			}
		}
		throw new ZormException("Could not locate table which owns column ["
				+ columnName + "] referenced in order-by mapping");
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator(subclassTableNameClosure,
				rootAlias);
	}

}

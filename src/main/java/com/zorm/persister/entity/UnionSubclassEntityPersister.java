package com.zorm.persister.entity;

import java.io.Serializable;
import java.util.*;

import com.zorm.FilterAliasGenerator;
import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.StaticFilterAliasGenerator;
import com.zorm.config.Settings;
import com.zorm.dialect.Dialect;
import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.engine.Mapping;
import com.zorm.engine.ValueInclusion;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.mapping.Column;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Subclass;
import com.zorm.mapping.Table;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.sql.SelectFragment;
import com.zorm.type.StandardBasicTypes;
import com.zorm.type.Type;
import com.zorm.type.VersionType;
import com.zorm.util.ArrayHelper;
import com.zorm.util.JoinedIterator;
import com.zorm.util.SingletonIterator;

public class UnionSubclassEntityPersister extends AbstractEntityPersister{

	// the class hierarchy structure
		private final String subquery;
		private final String tableName;
		//private final String rootTableName;
		private final String[] subclassClosure;
		private final String[] spaces;
		private final String[] subclassSpaces;
		private final Object discriminatorValue;
		private final String discriminatorSQLValue;
		private final Map subclassByDiscriminatorValue = new HashMap();

		private final String[] constraintOrderedTableNames;
		private final String[][] constraintOrderedKeyColumnNames;
	
	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws ZormException {
        super( persistentClass, factory );

		// TABLE

		tableName = persistentClass.getTable().getQualifiedName( 
				factory.getDialect(), 
				factory.getSettings().getDefaultCatalogName(), 
				factory.getSettings().getDefaultSchemaName() 
		);

		//Custom SQL

		String sql;
		boolean callable = false;
		ExecuteUpdateResultCheckStyle checkStyle = null;
		sql = persistentClass.getCustomSQLInsert();
		callable = sql != null && persistentClass.isCustomInsertCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
	            : persistentClass.getCustomSQLInsertCheckStyle() == null
						? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
	                    : persistentClass.getCustomSQLInsertCheckStyle();
		customSQLInsert = new String[] { sql };
		insertCallable = new boolean[] { callable };
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[] { checkStyle };

		sql = persistentClass.getCustomSQLUpdate();
		callable = sql != null && persistentClass.isCustomUpdateCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
	            : persistentClass.getCustomSQLUpdateCheckStyle() == null
						? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
	                    : persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLUpdate = new String[] { sql };
		updateCallable = new boolean[] { callable };
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[] { checkStyle };

		sql = persistentClass.getCustomSQLDelete();
		callable = sql != null && persistentClass.isCustomDeleteCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
	            : persistentClass.getCustomSQLDeleteCheckStyle() == null
						? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
	                    : persistentClass.getCustomSQLDeleteCheckStyle();
		customSQLDelete = new String[] { sql };
		deleteCallable = new boolean[] { callable };
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[] { checkStyle };

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );

		// PROPERTIES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();

		// SUBCLASSES
		subclassByDiscriminatorValue.put( 
				persistentClass.getSubclassId(),
				persistentClass.getEntityName() 
		);
		if ( persistentClass.isPolymorphic() ) {
			Iterator iter = persistentClass.getSubclassIterator();
			int k=1;
			while ( iter.hasNext() ) {
				Subclass sc = (Subclass) iter.next();
				subclassClosure[k++] = sc.getEntityName();
				subclassByDiscriminatorValue.put( sc.getSubclassId(), sc.getEntityName() );
			}
		}
		
		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		Iterator iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i=1; i<spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}
		
		HashSet subclassTables = new HashSet();
		iter = persistentClass.getSubclassTableClosureIterator();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			subclassTables.add( table.getQualifiedName(
					factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(), 
					factory.getSettings().getDefaultSchemaName() 
			) );
		}
		subclassSpaces = ArrayHelper.toStringArray(subclassTables);
        //子类数据库表
		subquery = generateSubquery(persistentClass, mapping);

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList tableNames = new ArrayList();
			ArrayList keyColumns = new ArrayList();
			if ( !isAbstract() ) {
				tableNames.add( tableName );
				keyColumns.add( getIdentifierColumnNames() );
			}
			iter = persistentClass.getSubclassTableClosureIterator();
			while ( iter.hasNext() ) {
				Table tab = ( Table ) iter.next();
				if ( !tab.isAbstractUnionTable() ) {
					String tableName = tab.getQualifiedName(
							factory.getDialect(),
							factory.getSettings().getDefaultCatalogName(),
							factory.getSettings().getDefaultSchemaName()
					);
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					Iterator citer = tab.getPrimaryKey().getColumnIterator();
					for ( int k=0; k<idColumnSpan; k++ ) {
						key[k] = ( ( Column ) citer.next() ).getQuotedName( factory.getDialect() );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] { tableName };
			constraintOrderedKeyColumnNames = new String[][] { getIdentifierColumnNames() };
		}

		initSubclassPropertyAliasesMap(persistentClass);
		
		postConstruct(mapping);

	}

	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}
	
	public String getTableName() {
		return subquery;
	}

	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassByDiscriminatorValue.get(value);
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	protected String getDiscriminatorFormula() {
		return null;
	}

	protected String getTableName(int j) {
		return tableName;
	}

	protected String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}
	
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}
	
	protected boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	public String fromTableFragment(String name) {
		return getTableName() + ' '  + name;
	}

	public String filterFragment(String name) {
		return hasWhere() ?
			" and " + getSQLWhereString(name) :
			"";
	}

	public String getSubclassPropertyTableName(int i) {
		return getTableName();
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		select.addColumn( name, getDiscriminatorColumnName(),  getDiscriminatorAlias() );
	}
	
	protected int[] getPropertyTableNumbersInSelect() {
		return new int[ getPropertySpan() ];
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	public boolean isMultiTable() {
		return isAbstract() || hasSubclasses();
	}

	public int getTableSpan() {
		return 1;
	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[ getSubclassColumnClosure().length ];
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[ getSubclassFormulaClosure().length ];
	}

	protected boolean[] getTableHasColumns() {
		return new boolean[] { true };
	}

	protected int[] getPropertyTableNumbers() {
		return new int[ getPropertySpan() ];
	}

	protected String generateSubquery(PersistentClass model, Mapping mapping) {

		Dialect dialect = getFactory().getDialect();
		Settings settings = getFactory().getSettings();
		
		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName(
					dialect,
					settings.getDefaultCatalogName(),
					settings.getDefaultSchemaName()
				);
		}

		HashSet columns = new LinkedHashSet();
		Iterator titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = (Table) titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator citer = table.getColumnIterator();
				while ( citer.hasNext() ) columns.add( citer.next() );
			}
		}

		StringBuilder buf = new StringBuilder()
			.append("( ");

		Iterator siter = new JoinedIterator(
			new SingletonIterator(model),
			model.getSubclassIterator()
		);

		while ( siter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				buf.append("select ");
				Iterator citer = columns.iterator();
				while ( citer.hasNext() ) {
					Column col = (Column) citer.next();
					if ( !table.containsColumn(col) ) {
						int sqlType = col.getSqlTypeCode(mapping);
						buf.append( dialect.getSelectClauseNullString(sqlType) )
							.append(" as ");
					}
					buf.append( col.getName() );
					buf.append(", ");
				}
				buf.append( clazz.getSubclassId() )
					.append(" as clazz_");
				buf.append(" from ")
					.append( table.getQualifiedName(
							dialect,
							settings.getDefaultCatalogName(),
							settings.getDefaultSchemaName()
					) );
				buf.append(" union ");
				if ( dialect.supportsUnionAll() ) {
					buf.append("all ");
				}
			}
		}
		
		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append(" )").toString();
	}

	protected String[] getSubclassTableKeyColumns(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return getIdentifierColumnNames();
	}

	public String getSubclassTableName(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return tableName;
	}

	public int getSubclassTableSpan() {
		return 1;
	}

	protected boolean isClassOrSuperclassTable(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return true;
	}

	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
	}

	public String[] getConstraintOrderedTableNameClosure() {
			return constraintOrderedTableNames;
	}

	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator(rootAlias);
	}


}
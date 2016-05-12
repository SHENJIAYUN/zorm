package com.zorm.persister.entity;

import java.io.Serializable;
import java.util.*;

import com.zorm.DynamicFilterAliasGenerator;
import com.zorm.FilterAliasGenerator;
import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.mapping.Column;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.Selectable;
import com.zorm.mapping.Subclass;
import com.zorm.mapping.Table;
import com.zorm.mapping.Value;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.InFragment;
import com.zorm.sql.Insert;
import com.zorm.sql.SelectFragment;
import com.zorm.type.DiscriminatorType;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;
import com.zorm.util.MarkerObject;

@SuppressWarnings("unused")
public class SingleTableEntityPersister extends AbstractEntityPersister{
	// the class hierarchy structure
		private final int joinSpan;
		private final String[] qualifiedTableNames;
		private final boolean[] isInverseTable;
		private final boolean[] isNullableTable;
		private final String[][] keyColumnNames;
		private final boolean[] cascadeDeleteEnabled;
		private final boolean hasSequentialSelects;
		
		private final String[] spaces;

		private final String[] subclassClosure;

		private final String[] subclassTableNameClosure;
		private final boolean[] subclassTableIsLazyClosure;
		private final boolean[] isInverseSubclassTable;
		private final boolean[] isNullableSubclassTable;
		private final boolean[] subclassTableSequentialSelect;
		private final String[][] subclassTableKeyColumnClosure;
		private final boolean[] isClassOrSuperclassTable;

		private final int[] propertyTableNumbers;

		private final int[] subclassPropertyTableNumberClosure;

		private final int[] subclassColumnTableNumberClosure;
		private final int[] subclassFormulaTableNumberClosure;

		private final Map subclassesByDiscriminatorValue = new HashMap();
		private final boolean forceDiscriminator;
		private final String discriminatorColumnName;
		private final String discriminatorColumnReaders;
		private final String discriminatorColumnReaderTemplate;
		private final String discriminatorAlias;
		private final Type discriminatorType;
		private final Object discriminatorValue;
		private final String discriminatorSQLValue;
		private final boolean discriminatorInsertable;

		private final String[] constraintOrderedTableNames;
		private final String[][] constraintOrderedKeyColumnNames;

		private final Map propertyTableNumbersByNameAndSubclass = new HashMap();
		
		private final Map sequentialSelectStringsByEntityName = new HashMap();

		private static final Object NULL_DISCRIMINATOR = new MarkerObject("<null discriminator>");
		private static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject("<not null discriminator>");
		private static final String NULL_STRING = "null";
		private static final String NOT_NULL_STRING = "not null";
		
		public SingleTableEntityPersister(
				final PersistentClass persistentClass, 
				final SessionFactoryImplementor factory,
				final Mapping mapping) throws ZormException {

			super( persistentClass, factory );

			// CLASS + TABLE

			joinSpan = persistentClass.getJoinClosureSpan()+1;
			qualifiedTableNames = new String[joinSpan];
			isInverseTable = new boolean[joinSpan];
			isNullableTable = new boolean[joinSpan];
			keyColumnNames = new String[joinSpan][];
			final Table table = persistentClass.getRootTable();
			qualifiedTableNames[0] = table.getQualifiedName( 
					factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(), 
					factory.getSettings().getDefaultSchemaName() 
			);
			isInverseTable[0] = false;
			isNullableTable[0] = false;
			keyColumnNames[0] = getIdentifierColumnNames();
			cascadeDeleteEnabled = new boolean[joinSpan];

			// Custom sql
			customSQLInsert = new String[joinSpan];
			customSQLUpdate = new String[joinSpan];
			customSQLDelete = new String[joinSpan];
			insertCallable = new boolean[joinSpan];
			updateCallable = new boolean[joinSpan];
			deleteCallable = new boolean[joinSpan];
			insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];
			updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];
			deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];

			customSQLInsert[0] = persistentClass.getCustomSQLInsert();
			insertCallable[0] = customSQLInsert[0] != null && persistentClass.isCustomInsertCallable();
			insertResultCheckStyles[0] = persistentClass.getCustomSQLInsertCheckStyle() == null
										  ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLInsert[0], insertCallable[0] )
										  : persistentClass.getCustomSQLInsertCheckStyle();
			customSQLUpdate[0] = persistentClass.getCustomSQLUpdate();
			updateCallable[0] = customSQLUpdate[0] != null && persistentClass.isCustomUpdateCallable();
			updateResultCheckStyles[0] = persistentClass.getCustomSQLUpdateCheckStyle() == null
										  ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLUpdate[0], updateCallable[0] )
										  : persistentClass.getCustomSQLUpdateCheckStyle();
			customSQLDelete[0] = persistentClass.getCustomSQLDelete();
			deleteCallable[0] = customSQLDelete[0] != null && persistentClass.isCustomDeleteCallable();
			deleteResultCheckStyles[0] = persistentClass.getCustomSQLDeleteCheckStyle() == null
										  ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLDelete[0], deleteCallable[0] )
										  : persistentClass.getCustomSQLDeleteCheckStyle();

			constraintOrderedTableNames = new String[qualifiedTableNames.length];
			constraintOrderedKeyColumnNames = new String[qualifiedTableNames.length][];
			for ( int i = qualifiedTableNames.length - 1, position = 0; i >= 0; i--, position++ ) {
				constraintOrderedTableNames[position] = qualifiedTableNames[i];
				constraintOrderedKeyColumnNames[position] = keyColumnNames[i];
			}

			spaces = ArrayHelper.join(
					qualifiedTableNames, 
					ArrayHelper.toStringArray( persistentClass.getSynchronizedTables() )
			);
			
			boolean hasDeferred = false;
			ArrayList subclassTables = new ArrayList();
			ArrayList joinKeyColumns = new ArrayList();
			ArrayList<Boolean> isConcretes = new ArrayList<Boolean>();
			ArrayList<Boolean> isDeferreds = new ArrayList<Boolean>();
			ArrayList<Boolean> isInverses = new ArrayList<Boolean>();
			ArrayList<Boolean> isNullables = new ArrayList<Boolean>();
			ArrayList<Boolean> isLazies = new ArrayList<Boolean>();
			subclassTables.add( qualifiedTableNames[0] );
			joinKeyColumns.add( getIdentifierColumnNames() );
			isConcretes.add(Boolean.TRUE);
			isDeferreds.add(Boolean.FALSE);
			isInverses.add(Boolean.FALSE);
			isNullables.add(Boolean.FALSE);
			isLazies.add(Boolean.FALSE);
			
			subclassTableSequentialSelect = ArrayHelper.toBooleanArray(isDeferreds);
			subclassTableNameClosure = ArrayHelper.toStringArray(subclassTables);
			subclassTableIsLazyClosure = ArrayHelper.toBooleanArray(isLazies);
			subclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( joinKeyColumns );
			isClassOrSuperclassTable = ArrayHelper.toBooleanArray(isConcretes);
			isInverseSubclassTable = ArrayHelper.toBooleanArray(isInverses);
			isNullableSubclassTable = ArrayHelper.toBooleanArray(isNullables);
			hasSequentialSelects = hasDeferred;

			// DISCRIMINATOR

			if ( persistentClass.isPolymorphic() ) {
				Value discrimValue = persistentClass.getDiscriminator();
				if (discrimValue==null) {
					throw new MappingException("discriminator mapping required for single table polymorphic persistence");
				}
				forceDiscriminator = persistentClass.isForceDiscriminator();
				Selectable selectable = (Selectable) discrimValue.getColumnIterator().next();
				if ( discrimValue.hasFormula() ) {
					discriminatorFormulaTemplate = null;
					discriminatorColumnName = null;
					discriminatorColumnReaders = null;
					discriminatorColumnReaderTemplate = null;
					discriminatorAlias = "clazz_";
				}
				else {
					Column column = (Column) selectable;
					discriminatorColumnName = column.getQuotedName( factory.getDialect() );
					discriminatorColumnReaders = column.getReadExpr( factory.getDialect() );
					discriminatorColumnReaderTemplate = column.getTemplate( factory.getDialect(), factory.getSqlFunctionRegistry() );
					discriminatorAlias = column.getAlias( factory.getDialect(), persistentClass.getRootTable() );
					discriminatorFormulaTemplate = null;
				}
				discriminatorType = persistentClass.getDiscriminator().getType();
				if ( persistentClass.isDiscriminatorValueNull() ) {
					discriminatorValue = NULL_DISCRIMINATOR;
					discriminatorSQLValue = InFragment.NULL;
					discriminatorInsertable = false;
				}
				else if ( persistentClass.isDiscriminatorValueNotNull() ) {
					discriminatorValue = NOT_NULL_DISCRIMINATOR;
					discriminatorSQLValue = InFragment.NOT_NULL;
					discriminatorInsertable = false;
				}
				else {
					discriminatorInsertable = persistentClass.isDiscriminatorInsertable() && !discrimValue.hasFormula();
					try {
						DiscriminatorType dtype = (DiscriminatorType) discriminatorType;
						discriminatorValue = dtype.stringToObject( persistentClass.getDiscriminatorValue() );
						discriminatorSQLValue = dtype.objectToSQLString( discriminatorValue, factory.getDialect() );
					}
					catch (ClassCastException cce) {
						throw new MappingException("Illegal discriminator type: " + discriminatorType.getName() );
					}
					catch (Exception e) {
						throw new MappingException("Could not format discriminator value to SQL string", e);
					}
				}
			}
			else {
				forceDiscriminator = false;
				discriminatorInsertable = false;
				discriminatorColumnName = null;
				discriminatorColumnReaders = null;
				discriminatorColumnReaderTemplate = null;
				discriminatorAlias = null;
				discriminatorType = null;
				discriminatorValue = null;
				discriminatorSQLValue = null;
				discriminatorFormulaTemplate = null;
			}

			// PROPERTIES

			propertyTableNumbers = new int[ getPropertySpan() ];
			Iterator iter = persistentClass.getPropertyClosureIterator();
			int i=0;
			while( iter.hasNext() ) {
				Property prop = (Property) iter.next();
				propertyTableNumbers[i++] = persistentClass.getJoinNumber(prop);

			}

			
			ArrayList columnJoinNumbers = new ArrayList();
			ArrayList formulaJoinedNumbers = new ArrayList();
			ArrayList propertyJoinNumbers = new ArrayList();
			
			iter = persistentClass.getSubclassPropertyClosureIterator();
			while ( iter.hasNext() ) {
				Property prop = (Property) iter.next();
				Integer join = persistentClass.getJoinNumber(prop);
				propertyJoinNumbers.add(join);

				propertyTableNumbersByNameAndSubclass.put( 
						prop.getPersistentClass().getEntityName() + '.' + prop.getName(), 
						join 
				);

				Iterator citer = prop.getColumnIterator();
				while ( citer.hasNext() ) {
					Selectable thing = (Selectable) citer.next();
					if ( thing.isFormula() ) {
						formulaJoinedNumbers.add(join);
					}
					else {
						columnJoinNumbers.add(join);
					}
				}
			}
			subclassColumnTableNumberClosure = ArrayHelper.toIntArray(columnJoinNumbers);
			subclassFormulaTableNumberClosure = ArrayHelper.toIntArray(formulaJoinedNumbers);
			subclassPropertyTableNumberClosure = ArrayHelper.toIntArray(propertyJoinNumbers);

			int subclassSpan = persistentClass.getSubclassSpan() + 1;
			subclassClosure = new String[subclassSpan];
			subclassClosure[0] = getEntityName();
			if ( persistentClass.isPolymorphic() ) {
				subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );
			}

			// SUBCLASSES
			if ( persistentClass.isPolymorphic() ) {
				iter = persistentClass.getSubclassIterator();
				int k=1;
				while ( iter.hasNext() ) {
					Subclass sc = (Subclass) iter.next();
					subclassClosure[k++] = sc.getEntityName();
					if ( sc.isDiscriminatorValueNull() ) {
						subclassesByDiscriminatorValue.put( NULL_DISCRIMINATOR, sc.getEntityName() );
					}
					else if ( sc.isDiscriminatorValueNotNull() ) {
						subclassesByDiscriminatorValue.put( NOT_NULL_DISCRIMINATOR, sc.getEntityName() );
					}
					else {
						try {
							DiscriminatorType dtype = (DiscriminatorType) discriminatorType;
							subclassesByDiscriminatorValue.put(
								dtype.stringToObject( sc.getDiscriminatorValue() ),
								sc.getEntityName()
							);
						}
						catch (ClassCastException cce) {
							throw new MappingException("Illegal discriminator type: " + discriminatorType.getName() );
						}
						catch (Exception e) {
							throw new MappingException("Error parsing discriminator value", e);
						}
					}
				}
			}


			initSubclassPropertyAliasesMap(persistentClass);
			
			postConstruct(mapping);

		}
		
		@Override
		protected String getDiscriminatorAlias() {
			return discriminatorAlias;
		}
		
		public String getDiscriminatorColumnReaders() {
			return discriminatorColumnReaders;
		}	
		
		public String getDiscriminatorColumnReaderTemplate() {
			return discriminatorColumnReaderTemplate;
		}	
		
		private final String discriminatorFormulaTemplate;
		
		protected String getDiscriminatorFormulaTemplate() {
			return discriminatorFormulaTemplate;
		}
		
		@Override
		public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
			return new DynamicFilterAliasGenerator(qualifiedTableNames, rootAlias);
		}
		
		@Override
		public String getDiscriminatorColumnName() {
			return discriminatorColumnName;
		}
		
		public String[] getSubclassClosure() {
			return subclassClosure;
		}

		@Override
		public void postInstantiate() throws MappingException {
			super.postInstantiate();
			if (hasSequentialSelects) {
				String[] entityNames = getSubclassClosure();
				for ( int i=1; i<entityNames.length; i++ ) {
					Loadable loadable = (Loadable) getFactory().getEntityPersister( entityNames[i] );
					if ( !loadable.isAbstract() ) { //perhaps not really necessary...
						String sequentialSelect = generateSequentialSelect(loadable);
						sequentialSelectStringsByEntityName.put( entityNames[i], sequentialSelect );
					}
				}
			}
		}


		private String generateSequentialSelect(Loadable loadable) {
			return null;
		}
		
		protected boolean isInverseSubclassTable(int j) {
			return isInverseSubclassTable[j];
		}
		
		protected boolean isNullableSubclassTable(int j) {
			return isNullableSubclassTable[j];
		}

		@Override
		public Serializable[] getPropertySpaces() {
			return spaces;
		}

		public String fromTableFragment(String name) {
			return getTableName() + ' ' + name;
		}
		
		protected boolean isSubclassTableSequentialSelect(int j) {
			return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
		}
		
		protected boolean isInverseTable(int j) {
			return isInverseTable[j];
		}

		
		@Override
		protected int[] getSubclassColumnTableNumberClosure() {
			return subclassColumnTableNumberClosure;
		}
		
		protected boolean isDiscriminatorFormula() {
			return discriminatorColumnName==null;
		}
		
		protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
			if ( isDiscriminatorFormula() ) {
				select.addFormula( name, getDiscriminatorFormulaTemplate(), getDiscriminatorAlias() );
			}
			else {
				select.addColumn( name, getDiscriminatorColumnName(),  getDiscriminatorAlias() );
			}
		}

		@Override
		protected int[] getSubclassFormulaTableNumberClosure() {
			return subclassFormulaTableNumberClosure;
		}

		@Override
		public String getSubclassTableName(int j) {
			return subclassTableNameClosure[j];
		}

		@Override
		protected String[] getSubclassTableKeyColumns(int j) {
			return subclassTableKeyColumnClosure[j];
		}

		@Override
		protected boolean isClassOrSuperclassTable(int j) {
			return isClassOrSuperclassTable[j];
		}

		@Override
		protected int getSubclassTableSpan() {
			return subclassTableNameClosure.length;
		}

		@Override
		protected boolean isTableCascadeDeleteEnabled(int j) {
			return false;
		}

		@Override
		protected String getTableName(int j) {
			return qualifiedTableNames[j];
		}

		@Override
		protected String[] getKeyColumns(int j) {
			return  keyColumnNames[j];
		}

		@Override
		protected boolean isPropertyOfTable(int property, int j) {
			return propertyTableNumbers[property]==j;
		}
		
		protected void addDiscriminatorToInsert(Insert insert) {

			if (discriminatorInsertable) {
				insert.addColumn( getDiscriminatorColumnName(), discriminatorSQLValue );
			}

		}

		@Override
		protected int[] getPropertyTableNumbersInSelect() {
			return propertyTableNumbers;
		}

		@Override
		protected int[] getPropertyTableNumbers() {
			return propertyTableNumbers;
		}

		@Override
		protected int getSubclassPropertyTableNumber(int i) {
			return subclassPropertyTableNumberClosure[i];
		}

		@Override
		protected String filterFragment(String alias) throws MappingException {
			String result = discriminatorFilterFragment(alias);
			if ( hasWhere() ) result += " and " + getSQLWhereString(alias);
			return result;
		}

		@Override
		protected int getTableSpan() {
			return joinSpan;
		}


		@Override
		public Type getDiscriminatorType() {
			return  discriminatorType;
		}

		@Override
		public boolean isMultiTable() {
			return getTableSpan() > 1;
		}

		private String discriminatorFilterFragment(String alias) {
			return "";
		}

		@Override
		public String getPropertyTableName(String propertyName) {
			Integer index = getEntityMetamodel().getPropertyIndexOrNull(propertyName);
			if (index==null) return null;
			return qualifiedTableNames[ propertyTableNumbers[ index.intValue() ] ];
		}

		@Override
		public String getSubclassPropertyTableName(int i) {
			return subclassTableNameClosure[ subclassPropertyTableNumberClosure[i] ];
		}

		@Override
		public String getTableName() {
			return qualifiedTableNames[0];
		}
		
		protected boolean isSubclassTableLazy(int j) {
			return subclassTableIsLazyClosure[j];
		}
		
		protected boolean isNullableTable(int j) {
			return isNullableTable[j];
		}

		@Override
		public String oneToManyFilterFragment(String alias)
				throws MappingException {
			return forceDiscriminator ?
					discriminatorFilterFragment(alias) :
					"";
		}
		
		public String getSubclassForDiscriminatorValue(Object value) {
			if (value==null) {
				return (String) subclassesByDiscriminatorValue.get(NULL_DISCRIMINATOR);
			}
			else {
				String result = (String) subclassesByDiscriminatorValue.get(value);
				if (result==null) result = (String) subclassesByDiscriminatorValue.get(NOT_NULL_DISCRIMINATOR);
				return result;
			}
		}

}

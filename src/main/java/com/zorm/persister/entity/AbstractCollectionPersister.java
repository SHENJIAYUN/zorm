package com.zorm.persister.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zorm.FetchMode;
import com.zorm.FilterAliasGenerator;
import com.zorm.collection.PersistentCollection;
import com.zorm.config.Configuration;
import com.zorm.dialect.Dialect;
import com.zorm.engine.BasicBatchKey;
import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.exception.ZormException;
import com.zorm.id.IdentifierGenerator;
import com.zorm.jdbc.Expectation;
import com.zorm.jdbc.Expectations;
import com.zorm.jdbc.SqlExceptionHelper;
import com.zorm.loader.CollectionInitializer;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Column;
import com.zorm.mapping.IdentifierCollection;
import com.zorm.mapping.IndexedCollection;
import com.zorm.mapping.List;
import com.zorm.mapping.Selectable;
import com.zorm.mapping.Table;
import com.zorm.meta.CollectionMetadata;
import com.zorm.persister.ElementPropertyMapping;
import com.zorm.persister.NamedQueryCollectionInitializer;
import com.zorm.persister.SQLLoadableCollection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.sql.Alias;
import com.zorm.sql.SelectFragment;
import com.zorm.sql.SimpleSelect;
import com.zorm.type.CollectionType;
import com.zorm.type.Type;
import com.zorm.type.EntityType;
import com.zorm.util.ArrayHelper;
import com.zorm.util.FilterHelper;
import com.zorm.util.MessageHelper;
import com.zorm.util.StringHelper;

public abstract class AbstractCollectionPersister implements CollectionMetadata, SQLLoadableCollection{

	private final String role;

	// SQL statements
	private final String sqlDeleteString;
	private final String sqlInsertRowString;
	private final String sqlUpdateRowString;
	private final String sqlDeleteRowString;
	private final String sqlSelectSizeString;
	private final String sqlSelectRowByIndexString;
	private final String sqlDetectRowByIndexString;
	private final String sqlDetectRowByElementString;

	protected final boolean hasWhere;
	protected final String sqlWhereString;
	private final String sqlWhereStringTemplate;

//	private final boolean hasOrder;
//	private final OrderByTranslation orderByTranslation;

//	private final boolean hasManyToManyOrder;
//	private final OrderByTranslation manyToManyOrderByTranslation;

	private final int baseIndex;

	private final String nodeName;
	private final String elementNodeName;
	private final String indexNodeName;

	protected final boolean indexContainsFormula;
	protected final boolean elementIsPureFormula;

	// types
	private final Type keyType;
	private final Type indexType;
	protected final Type elementType;
	private final Type identifierType;

	// columns
	protected final String[] keyColumnNames;
	protected final String[] indexColumnNames;
	protected final String[] indexFormulaTemplates;
	protected final String[] indexFormulas;
	protected final boolean[] indexColumnIsSettable;
	protected final String[] elementColumnNames;
	protected final String[] elementColumnWriters;
	protected final String[] elementColumnReaders;
	protected final String[] elementColumnReaderTemplates;
	protected final String[] elementFormulaTemplates;
	protected final String[] elementFormulas;
	protected final boolean[] elementColumnIsSettable;
	protected final boolean[] elementColumnIsInPrimaryKey;
	protected final String[] indexColumnAliases;
	protected final String[] elementColumnAliases;
	protected final String[] keyColumnAliases;

	protected final String identifierColumnName;
	private final String identifierColumnAlias;
	// private final String unquotedIdentifierColumnName;

	protected final String qualifiedTableName;

	private final String queryLoaderName;

	private final boolean isPrimitiveArray;
	private final boolean isArray;
	protected final boolean hasIndex;
	protected final boolean hasIdentifier;
	private final boolean isLazy;
	private final boolean isExtraLazy;
	private final boolean isInverse;
	private final boolean isMutable;
	private final boolean isVersioned;
	protected final int batchSize;
	private final FetchMode fetchMode;
	private final boolean hasOrphanDelete;
	private final boolean subselectLoadable;

	// extra information about the element type
	private final Class elementClass;
	private final String entityName;

	private final Dialect dialect;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final SessionFactoryImplementor factory;
	private final EntityPersister ownerPersister;
	private final IdentifierGenerator identifierGenerator;
	private final PropertyMapping elementPropertyMapping;
	private final EntityPersister elementPersister;
//	private final CollectionRegionAccessStrategy cacheAccessStrategy;
	private final CollectionType collectionType;
	private CollectionInitializer initializer;

//	private final CacheEntryStructure cacheEntryStructure;

	// dynamic filters for the collection
	private final FilterHelper filterHelper;

	// dynamic filters specifically for many-to-many inside the collection
	private final FilterHelper manyToManyFilterHelper;

	private final String manyToManyWhereString;
	private final String manyToManyWhereTemplate;

	// custom sql
	private final boolean insertCallable;
	private final boolean updateCallable;
	private final boolean deleteCallable;
	private final boolean deleteAllCallable;
	private ExecuteUpdateResultCheckStyle insertCheckStyle;
	private ExecuteUpdateResultCheckStyle updateCheckStyle;
	private ExecuteUpdateResultCheckStyle deleteCheckStyle;
	private ExecuteUpdateResultCheckStyle deleteAllCheckStyle;

	private final Serializable[] spaces;

	private Map collectionPropertyColumnAliases = new HashMap();
	private Map collectionPropertyColumnNames = new HashMap();
	
	protected abstract String generateInsertRowString();
	protected abstract String generateDeleteString();
	protected abstract String generateDeleteRowString();
	protected abstract String generateUpdateRowString();
	protected abstract CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException;

	public AbstractCollectionPersister(
			final Collection collection,
			final Configuration cfg,
			final SessionFactoryImplementor factory) throws MappingException{
		this.factory = factory;
		dialect = factory.getDialect();
		sqlExceptionHelper = factory.getSQLExceptionHelper();
		collectionType = collection.getCollectionType();
		role = collection.getRole();
		entityName = collection.getOwnerEntityName();
		ownerPersister = factory.getEntityPersister( entityName );
		queryLoaderName = collection.getLoaderName();
		nodeName = collection.getNodeName();
		isMutable = collection.isMutable();

		Table table = collection.getCollectionTable();
		fetchMode = collection.getElement().getFetchMode();
		elementType = collection.getElement().getType();
		isPrimitiveArray = collection.isPrimitiveArray();
		isArray = collection.isArray();
		subselectLoadable = collection.isSubselectLoadable();

		qualifiedTableName = table.getQualifiedName(
				dialect,
				factory.getSettings().getDefaultCatalogName(),
				factory.getSettings().getDefaultSchemaName()
				);

		int spacesSize = 1 + collection.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = qualifiedTableName;
		Iterator iter = collection.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}

		sqlWhereString = StringHelper.isNotEmpty( collection.getWhere() ) ? "( " + collection.getWhere() + ") " : null;
		hasWhere = sqlWhereString != null;
		sqlWhereStringTemplate = hasWhere ?
				Template.renderWhereStringTemplate( sqlWhereString, dialect, factory.getSqlFunctionRegistry() ) :
				null;

		hasOrphanDelete = collection.hasOrphanDelete();

		int batch = collection.getBatchSize();
		if ( batch == -1 ) {
			batch = factory.getSettings().getDefaultBatchFetchSize();
		}
		batchSize = batch;

		isVersioned = collection.isOptimisticLocked();

		// KEY

		keyType = collection.getKey().getType();
		iter = collection.getKey().getColumnIterator();
		int keySpan = collection.getKey().getColumnSpan();
		keyColumnNames = new String[keySpan];
		keyColumnAliases = new String[keySpan];
		int k = 0;
		while ( iter.hasNext() ) {
			Column col = ( (Column) iter.next() );
			keyColumnNames[k] = col.getQuotedName( dialect );
			keyColumnAliases[k] = col.getAlias( dialect, collection.getOwner().getRootTable() );
			k++;
		}

		// ELEMENT

		String elemNode = collection.getElementNodeName();
		if ( elementType.isEntityType() ) {
			String entityName = ( (EntityType) elementType ).getAssociatedEntityName();
			elementPersister = factory.getEntityPersister( entityName );
			if ( elemNode == null ) {
				elemNode = cfg.getClassMapping( entityName ).getNodeName();
			}
		}
		else {
			elementPersister = null;
		}
		elementNodeName = elemNode;

		int elementSpan = collection.getElement().getColumnSpan();
		elementColumnAliases = new String[elementSpan];
		elementColumnNames = new String[elementSpan];
		elementColumnWriters = new String[elementSpan];
		elementColumnReaders = new String[elementSpan];
		elementColumnReaderTemplates = new String[elementSpan];
		elementFormulaTemplates = new String[elementSpan];
		elementFormulas = new String[elementSpan];
		elementColumnIsSettable = new boolean[elementSpan];
		elementColumnIsInPrimaryKey = new boolean[elementSpan];
		boolean isPureFormula = true;
		boolean hasNotNullableColumns = false;
		int j = 0;
		iter = collection.getElement().getColumnIterator();
		while ( iter.hasNext() ) {
			Selectable selectable = (Selectable) iter.next();
			elementColumnAliases[j] = selectable.getAlias( dialect, table );
			
			Column col = (Column) selectable;
			elementColumnNames[j] = col.getQuotedName( dialect );
			elementColumnWriters[j] = col.getWriteExpr();
			elementColumnReaders[j] = col.getReadExpr( dialect );
			elementColumnReaderTemplates[j] = col.getTemplate( dialect, factory.getSqlFunctionRegistry() );
			elementColumnIsSettable[j] = true;
			elementColumnIsInPrimaryKey[j] = !col.isNullable();
			 if ( !col.isNullable() ) {
				hasNotNullableColumns = true;
			 }
			 isPureFormula = false;
			 j++;
		}
		elementIsPureFormula = isPureFormula;

		if ( !hasNotNullableColumns ) {
			Arrays.fill( elementColumnIsInPrimaryKey, true );
		}

		// INDEX AND ROW SELECT

		hasIndex = collection.isIndexed();
		if ( hasIndex ) {
			// NativeSQL: collect index column and auto-aliases
			IndexedCollection indexedCollection = (IndexedCollection) collection;
			indexType = indexedCollection.getIndex().getType();
			int indexSpan = indexedCollection.getIndex().getColumnSpan();
			iter = indexedCollection.getIndex().getColumnIterator();
			indexColumnNames = new String[indexSpan];
			indexFormulaTemplates = new String[indexSpan];
			indexFormulas = new String[indexSpan];
			indexColumnIsSettable = new boolean[indexSpan];
			indexColumnAliases = new String[indexSpan];
			int i = 0;
			boolean hasFormula = false;
			while ( iter.hasNext() ) {
				Selectable s = (Selectable) iter.next();
				indexColumnAliases[i] = s.getAlias( dialect );
				
				Column indexCol = (Column) s;
				indexColumnNames[i] = indexCol.getQuotedName( dialect );
				indexColumnIsSettable[i] = true;
				
				i++;
			}
			indexContainsFormula = hasFormula;
			baseIndex = indexedCollection.isList() ?
					( (List) indexedCollection ).getBaseIndex() : 0;

			indexNodeName = indexedCollection.getIndexNodeName();

		}
		else {
			indexContainsFormula = false;
			indexColumnIsSettable = null;
			indexFormulaTemplates = null;
			indexFormulas = null;
			indexType = null;
			indexColumnNames = null;
			indexColumnAliases = null;
			baseIndex = 0;
			indexNodeName = null;
		}

		hasIdentifier = collection.isIdentified();
		if ( hasIdentifier ) {
			if ( collection.isOneToMany() ) {
				throw new MappingException( "one-to-many collections with identifiers are not supported" );
			}
			IdentifierCollection idColl = (IdentifierCollection) collection;
			identifierType = idColl.getIdentifier().getType();
			iter = idColl.getIdentifier().getColumnIterator();
			Column col = (Column) iter.next();
			identifierColumnName = col.getQuotedName( dialect );
			identifierColumnAlias = col.getAlias( dialect );
			// unquotedIdentifierColumnName = identifierColumnAlias;
			identifierGenerator = idColl.getIdentifier().createIdentifierGenerator(
					cfg.getIdentifierGeneratorFactory(),
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName(),
					null
					);
		}
		else {
			identifierType = null;
			identifierColumnName = null;
			identifierColumnAlias = null;
			identifierGenerator = null;
		}

		// GENERATE THE SQL:

		if ( collection.getCustomSQLInsert() == null ) {
			sqlInsertRowString = generateInsertRowString();
			insertCallable = false;
			insertCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			sqlInsertRowString = collection.getCustomSQLInsert();
			insertCallable = collection.isCustomInsertCallable();
			insertCheckStyle = collection.getCustomSQLInsertCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( collection.getCustomSQLInsert(), insertCallable )
					: collection.getCustomSQLInsertCheckStyle();
		}

		if ( collection.getCustomSQLUpdate() == null ) {
			sqlUpdateRowString = generateUpdateRowString();
			updateCallable = false;
			updateCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			sqlUpdateRowString = collection.getCustomSQLUpdate();
			updateCallable = collection.isCustomUpdateCallable();
			updateCheckStyle = collection.getCustomSQLUpdateCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( collection.getCustomSQLUpdate(), insertCallable )
					: collection.getCustomSQLUpdateCheckStyle();
		}

		if ( collection.getCustomSQLDelete() == null ) {
			sqlDeleteRowString = generateDeleteRowString();
			deleteCallable = false;
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			sqlDeleteRowString = collection.getCustomSQLDelete();
			deleteCallable = collection.isCustomDeleteCallable();
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		if ( collection.getCustomSQLDeleteAll() == null ) {
			sqlDeleteString = generateDeleteString();
			deleteAllCallable = false;
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			sqlDeleteString = collection.getCustomSQLDeleteAll();
			deleteAllCallable = collection.isCustomDeleteAllCallable();
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		sqlSelectSizeString = generateSelectSizeString( collection.isIndexed() && !collection.isMap() );
		sqlDetectRowByIndexString = generateDetectRowByIndexString();
		sqlDetectRowByElementString = generateDetectRowByElementString();
		sqlSelectRowByIndexString = generateSelectRowByIndexString();

		isLazy = collection.isLazy();
		isExtraLazy = collection.isExtraLazy();

		isInverse = collection.isInverse();

		if ( collection.isArray() ) {
			elementClass = ( (com.zorm.mapping.Array) collection ).getElementClass();
		}
		else {
			elementClass = null; // elementType.returnedClass();
		}

	   if ( !elementType.isEntityType() ) {
			elementPropertyMapping = new ElementPropertyMapping(
					elementColumnNames,
					elementType
					);
		}
		else {
			if ( elementPersister instanceof PropertyMapping ) { // not all classpersisters implement PropertyMapping!
				elementPropertyMapping = (PropertyMapping) elementPersister;
			}
			else {
				elementPropertyMapping = new ElementPropertyMapping(
						elementColumnNames,
						elementType
						);
			}
		}

//		hasOrder = collection.getOrderBy() != null;
//		if ( hasOrder ) {
//			orderByTranslation = Template.translateOrderBy(
//					collection.getOrderBy(),
//					new ColumnMapperImpl(),
//					factory,
//					dialect,
//					factory.getSqlFunctionRegistry()
//			);
//		}
//		else {
//			orderByTranslation = null;
//		}

		// Handle any filters applied to this collection
		filterHelper = new FilterHelper( collection.getFilters(), factory);

		// Handle any filters applied to this collection for many-to-many
		manyToManyFilterHelper = new FilterHelper( collection.getManyToManyFilters(), factory);
		manyToManyWhereString = StringHelper.isNotEmpty( collection.getManyToManyWhere() ) ?
				"( " + collection.getManyToManyWhere() + ")" :
				null;
		manyToManyWhereTemplate = manyToManyWhereString == null ?
				null :
				Template.renderWhereStringTemplate( manyToManyWhereString, factory.getDialect(), factory.getSqlFunctionRegistry() );

//		hasManyToManyOrder = collection.getManyToManyOrdering() != null;
//		if ( hasManyToManyOrder ) {
//			manyToManyOrderByTranslation = Template.translateOrderBy(
//					collection.getManyToManyOrdering(),
//					new ColumnMapperImpl(),
//					factory,
//					dialect,
//					factory.getSqlFunctionRegistry()
//			);
//		}
//		else {
//			manyToManyOrderByTranslation = null;
//		}

		initCollectionPropertyMap();
	}
	
	private BasicBatchKey recreateBatchKey;
	
	@Override
	public void recreate(
			PersistentCollection collection, 
			Serializable id,
			SessionImplementor session) {
		if(!isInverse && isRowInsertEnabled()){
			try{
				Iterator entries = collection.entries(this);
				if(entries.hasNext()){
					Expectation expectation = Expectations.appropriateExpectation( getInsertCheckStyle());
				    collection.preInsert(this);
				    int i = 0;
				    int count = 0;
				    while(entries.hasNext()){
				    	final Object entry = entries.next();
				    	if(collection.entryExists(entry,i)){
				    		int offset = 1;
				    		PreparedStatement st = null;
				    		boolean callable = isInsertCallable();
				    		boolean useBatch = expectation.canBeBatched();
				    		String sql = getSQLInsertRowString();
				    		
				    		if(useBatch){
				    			if ( recreateBatchKey == null ) {
									recreateBatchKey = new BasicBatchKey(
											getRole() + "#RECREATE",
											expectation
											);
								}
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( recreateBatchKey )
										.getBatchStatement( sql, callable );
				    		}
				    		else{
				    			st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getStatementPreparer()
										.prepareStatement( sql, callable );
				    		}
				    		
				    		try{
				    			offset += expectation.prepare(st);
				    			
				    			int loc = writeKey(st, id, offset, session);
				    			
				    			if ( hasIdentifier ) {
									loc = writeIdentifier( st, collection.getIdentifier( entry, i ), loc, session );
								}
				    			
				    			loc = writeElement(st,collection.getElement(entry),loc,session);
				    		
				    		    if(useBatch){
				    		    	session.getTransactionCoordinator()
									    .getJdbcCoordinator()
									    .getBatch( recreateBatchKey )
									    .addToBatch();
				    		    }else{
				    		    	expectation.verifyOutcome( st.executeUpdate(), st, -1 );
				    		    }
				    		    collection.afterRowInsert( this, entry, i );
				    		    count++;
				    		}
				    		catch ( SQLException sqle ) {
								if ( useBatch ) {
									session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
								}
								throw sqle;
							}
							finally {
								if ( !useBatch ) {
									st.close();
								}
							}
				    	}
				    	i++;
				    }
				}
			}
			catch ( SQLException sqle ) {
				throw sqlExceptionHelper.convert(
						sqle,
						"could not insert collection: " +
								MessageHelper.collectionInfoString( this, collection, id, session ),
						getSQLInsertRowString()
						);
			}
		}
	}
	
	protected int writeElement(PreparedStatement st, Object elt, int i, SessionImplementor session)
			throws ZormException, SQLException {
		getElementType().nullSafeSet( st, elt, i, elementColumnIsSettable, session );
		return i + ArrayHelper.countTrue( elementColumnIsSettable );

	}
	
	protected int writeKey(PreparedStatement st, Serializable key, int i, SessionImplementor session)
			throws ZormException, SQLException {

		if ( key == null ) {
			throw new NullPointerException( "null key for collection: " + role ); // an assertion
		}
		getKeyType().nullSafeSet( st, key, i, session );
		return i + keyColumnAliases.length;
	}
	
	public int writeIdentifier(PreparedStatement st, Object id, int i, SessionImplementor session)
			throws ZormException, SQLException {

		getIdentifierType().nullSafeSet( st, id, i, session );
		return i + 1;
	}
	
	public Type getIdentifierType() {
		return identifierType;
	}
	
	protected String getSQLInsertRowString() {
		return sqlInsertRowString;
	}
	
	protected boolean isInsertCallable() {
		return insertCallable;
	}
	
	protected ExecuteUpdateResultCheckStyle getInsertCheckStyle() {
		return insertCheckStyle;
	}
	
	public boolean isArray() {
		return isArray;
	}
	
	public EntityPersister getElementPersister() {
		if ( elementPersister == null ) {
			throw new AssertionFailure( "not an association" );
		}
		return elementPersister;
	}
	
	public String oneToManyFilterFragment(String alias) throws MappingException {
		return "";
	}
	
	public String[] getElementColumnNames(String alias) {
		return qualify( alias, elementColumnNames, elementFormulaTemplates );
	}
	
	public void initCollectionPropertyMap() {

		initCollectionPropertyMap( "key", keyType, keyColumnAliases, keyColumnNames );
		initCollectionPropertyMap( "element", elementType, elementColumnAliases, elementColumnNames );
		if ( hasIndex ) {
			initCollectionPropertyMap( "index", indexType, indexColumnAliases, indexColumnNames );
		}
		if ( hasIdentifier ) {
			initCollectionPropertyMap(
					"id",
					identifierType,
					new String[] { identifierColumnAlias },
					new String[] { identifierColumnName } );
		}
	}
	
	private void initCollectionPropertyMap(String aliasName, Type type, String[] columnAliases, String[] columnNames) {
		collectionPropertyColumnAliases.put( aliasName, columnAliases );
		collectionPropertyColumnNames.put( aliasName, columnNames );
	}
	
	@Override
	public Type getIndexType() {
		return indexType;
	}
	
	@Override
	public CollectionMetadata getCollectionMetadata() {
		return this;
	}
	
	public boolean hasIndex() {
		return hasIndex;
	}
	
	protected String generateSelectRowByIndexString() {
		if ( !hasIndex() ) {
			return null;
		}
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getIndexColumnNames(), "=?" )
				.addCondition( indexFormulas, "=?" )
				.addColumns( getElementColumnNames(), elementColumnAliases )
				.addColumns( indexFormulas, indexColumnAliases )
				.toStatementString();
	}
	
	protected String generateDetectRowByElementString() {
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getElementColumnNames(), "=?" )
				.addCondition( elementFormulas, "=?" )
				.addColumn( "1" )
				.toStatementString();
	}
	
	protected String generateDetectRowByIndexString() {
		if ( !hasIndex() ) {
			return null;
		}
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getIndexColumnNames(), "=?" )
				.addCondition( indexFormulas, "=?" )
				.addColumn( "1" )
				.toStatementString();
	}
	
	protected String generateSelectSizeString(boolean isIntegerIndexed) {
		String selectValue = isIntegerIndexed ?
				"max(" + getIndexColumnNames()[0] + ") + 1" : // lists, arrays
				"count(" + getElementColumnNames()[0] + ")"; // sets, maps, bags
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addColumn( selectValue )
				.toStatementString();
	}
	
	public String[] getElementColumnNames() {
		return elementColumnNames;
	}
	
	@Override
	public FetchMode getFetchMode() {
		return fetchMode;
	}
	
	public String[] getIndexColumnNames() {
		return indexColumnNames;
	}
	
	public Type toType(String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			return indexType;
		}
		return elementPropertyMapping.toType( propertyName );
	}
	
	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			return qualify( alias, indexColumnNames, indexFormulaTemplates );
		}
		return elementPropertyMapping.toColumns( alias, propertyName );
	}
	
	private String[] indexFragments;
	
	public String[] toColumns(String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			if ( indexFragments == null ) {
				String[] tmp = new String[indexColumnNames.length];
				for ( int i = 0; i < indexColumnNames.length; i++ ) {
					tmp[i] = indexColumnNames[i] == null
							? indexFormulas[i]
							: indexColumnNames[i];
					indexFragments = tmp;
				}
			}
			return indexFragments;
		}

		return elementPropertyMapping.toColumns( propertyName );
	}
	
	private static String[] qualify(String alias, String[] columnNames, String[] formulaTemplates) {
		int span = columnNames.length;
		String[] result = new String[span];
		for ( int i = 0; i < span; i++ ) {
			if ( columnNames[i] == null ) {
				result[i] = StringHelper.replace( formulaTemplates[i], Template.TEMPLATE, alias );
			}
			else {
				result[i] = StringHelper.qualify( alias, columnNames[i] );
			}
		}
		return result;
	}
	
	public Type getType() {
		return elementPropertyMapping.getType(); 
	}
	
	public String getName() {
		return getRole();
	}
	
	public String getRole() {
		return role;
	}
	
	public Type getElementType() {
		return elementType;
	}
	
	public String getTableName() {
		return qualifiedTableName;
	}
	
	public String[] getKeyColumnNames() {
		return keyColumnNames;
	}
	
	protected Dialect getDialect() {
		return dialect;
	}
	
	public SessionFactoryImplementor getFactory() {
		return factory;
	}
	
	public boolean hasOrphanDelete() {
		return hasOrphanDelete;
	}
	
	@Override
	public int getBatchSize() {
		return batchSize;
	}
	
	public boolean isLazy() {
		return isLazy;
	}
	
	public boolean isExtraLazy() {
		return isExtraLazy;
	}
	
	public int getSize(Serializable key, SessionImplementor session) {
		try {
			PreparedStatement st = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( sqlSelectSizeString );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				ResultSet rs = st.executeQuery();
				try {
					return rs.next() ? rs.getInt( 1 ) - baseIndex : 0;
				}
				finally {
					rs.close();
				}
			}
			finally {
				st.close();
			}
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not retrieve collection size: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
					);
		}
	}
	
	public void postInstantiate() throws MappingException {
		initializer = queryLoaderName == null ?
				createCollectionInitializer( LoadQueryInfluencers.NONE ) :
				new NamedQueryCollectionInitializer( queryLoaderName, this );
	}
	
	@Override
	public Type getKeyType() {
		return keyType;
	}
	
	@Override
	public boolean isCollection() {
		return true;
	}
	
	public boolean isInverse() {
		return isInverse;
	}
	
	public Object readKey(ResultSet rs, String[] aliases, SessionImplementor session) throws ZormException, SQLException{
		return getKeyType().nullSafeGet( rs, aliases, session, null );
	}
	
	public String getManyToManyFilterFragment(String alias, Map enabledFilters) {
		StringBuilder buffer = new StringBuilder();
		manyToManyFilterHelper.render( buffer, elementPersister.getFilterAliasGenerator(alias), enabledFilters );

		if ( manyToManyWhereString != null ) {
			buffer.append( " and " )
					.append( StringHelper.replace( manyToManyWhereTemplate, Template.TEMPLATE, alias ) );
		}

		return buffer.toString();
	}
	
	protected SelectFragment generateSelectFragment(String alias, String columnSuffix) {
		return new SelectFragment()
				.setSuffix( columnSuffix )
				.addColumns( alias, keyColumnNames, keyColumnAliases );
	}
	
	protected void appendElementColumns(SelectFragment frag, String elemAlias) {
		for ( int i = 0; i < elementColumnIsSettable.length; i++ ) {
			if ( elementColumnIsSettable[i] ) {
				frag.addColumnTemplate( elemAlias, elementColumnReaderTemplates[i], elementColumnAliases[i] );
			}
			else {
				frag.addFormula( elemAlias, elementFormulaTemplates[i], elementColumnAliases[i] );
			}
		}
	}
	
	protected void appendIndexColumns(SelectFragment frag, String alias) {
		if ( hasIndex ) {
			for ( int i = 0; i < indexColumnIsSettable.length; i++ ) {
				if ( indexColumnIsSettable[i] ) {
					frag.addColumn( alias, indexColumnNames[i], indexColumnAliases[i] );
				}
				else {
					frag.addFormula( alias, indexFormulaTemplates[i], indexColumnAliases[i] );
				}
			}
		}
	}
	
	protected void appendIdentifierColumns(SelectFragment frag, String alias) {
		if ( hasIdentifier ) {
			frag.addColumn( alias, identifierColumnName, identifierColumnAlias );
		}
	}
	
	public String selectFragment(String alias, String columnSuffix) {
		SelectFragment frag = generateSelectFragment( alias, columnSuffix );
		appendElementColumns( frag, alias );
		appendIndexColumns( frag, alias );
		appendIdentifierColumns( frag, alias );

		return frag.toFragmentString()
				.substring( 2 ); // strip leading ','
	}
	
	public String getSQLOrderByString(String alias) {
		return "";
	}
	
	public CollectionType getCollectionType() {
		return collectionType;
	}
	
	public String[] getKeyColumnAliases(String suffix) {
		return new Alias( suffix ).toAliasStrings( keyColumnAliases );
	}
	
	public String[] getIndexColumnAliases(String suffix) {
		if ( hasIndex ) {
			return new Alias( suffix ).toAliasStrings( indexColumnAliases );
		}
		else {
			return null;
		}
	}
	
	public String[] getElementColumnAliases(String suffix) {
		return new Alias( suffix ).toAliasStrings( elementColumnAliases );
	}
	
	public boolean isMutable() {
		return isMutable;
	}
	
	public Serializable[] getCollectionSpaces() {
		return spaces;
	}
	
	public EntityPersister getOwnerEntityPersister() {
		return ownerPersister;
	}
	
	public String getIdentifierColumnAlias(String suffix) {
		if ( hasIdentifier ) {
			return new Alias( suffix ).toAliasString( identifierColumnAlias );
		}
		else {
			return null;
		}
	}
	
	public String getManyToManyOrderByString(String alias) {
		return "";
	}

	protected boolean isRowDeleteEnabled() {
		return true;
	}

	protected boolean isRowInsertEnabled() {
		return true;
	}
	
	public boolean hasWhere() {
		return hasWhere;
	}
	
	protected String getSQLWhereString(String alias) {
		return StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
	}
	
	public abstract FilterAliasGenerator getFilterAliasGenerator(final String rootAlias);
	
	@SuppressWarnings("rawtypes")
	public String filterFragment(String alias, Map enabledFilters) throws MappingException {

		StringBuilder sessionFilterFragment = new StringBuilder();
		filterHelper.render( sessionFilterFragment, getFilterAliasGenerator(alias), enabledFilters );

		return sessionFilterFragment.append( filterFragment( alias ) ).toString();
	}
	
	protected String filterFragment(String alias) throws MappingException {
		return hasWhere() ? " and " + getSQLWhereString( alias ) : "";
	}
	
	public Object readElement(ResultSet rs, Object owner, String[] aliases, SessionImplementor session) throws ZormException, SQLException {
		return getElementType().nullSafeGet( rs, aliases, session, owner );
	}

}

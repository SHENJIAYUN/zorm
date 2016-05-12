package com.zorm.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.zorm.LockOptions;
import com.zorm.dialect.Dialect;
import com.zorm.engine.RowSelection;
import com.zorm.engine.TypedValue;
import com.zorm.entity.ResultTransformer;
import com.zorm.exception.QueryException;
import com.zorm.jdbc.ScrollMode;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;
import com.zorm.util.ParserHelper;

public class QueryParameters {
	private Type[] positionalParameterTypes;
	private Object[] positionalParameterValues;
	private Object optionalObject;
	private String optionalEntityName;
	private Serializable optionalId;
	private LockOptions lockOptions;
	private boolean isReadOnlyInitialized;
	private boolean readOnly;
	private RowSelection rowSelection;
	private String processedSQL;
	private Type[] processedPositionalParameterTypes;
	private Object[] processedPositionalParameterValues;
	private String comment;
	private boolean callable = false;
	private ScrollMode scrollMode;
	private Map<String, TypedValue> namedParameters;
	private boolean autodiscovertypes = false;
	private final ResultTransformer resultTransformer; 
	private boolean isNaturalKeyLookup;
	private boolean cacheable;
	private String cacheRegion;
	private Serializable[] collectionKeys;
	
	public QueryParameters() {
		this( ArrayHelper.EMPTY_TYPE_ARRAY, ArrayHelper.EMPTY_OBJECT_ARRAY );
	}

	public QueryParameters(Type type, Object value) {
		this( new Type[] { type }, new Object[] { value } );
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalObjectId) {
		this( positionalParameterTypes, positionalParameterValues );
		this.optionalObject = optionalObject;
		this.optionalId = optionalObjectId;
		this.optionalEntityName = optionalEntityName;

	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues) {
		this( positionalParameterTypes, positionalParameterValues, null, null, false, false, false, null, null, false, null );
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Serializable[] collectionKeys) {
		this( positionalParameterTypes, positionalParameterValues, null, collectionKeys );
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Map<String,TypedValue> namedParameters,
			final Serializable[] collectionKeys) {
		this(
				positionalParameterTypes,
				positionalParameterValues,
				namedParameters,
				null,
				null,
				false,
				false,
				false,
				null,
				null,
				collectionKeys,
				null
		);
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final LockOptions lockOptions,
			final RowSelection rowSelection,
			final boolean isReadOnlyInitialized,
			final boolean readOnly,
			final boolean cacheable,
			final String cacheRegion,
			//final boolean forceCacheRefresh,
			final String comment,
			final boolean isLookupByNaturalKey,
			final ResultTransformer transformer) {
		this(
				positionalParameterTypes,
				positionalParameterValues,
				null,
				lockOptions,
				rowSelection,
				isReadOnlyInitialized,
				readOnly,
				cacheable,
				cacheRegion,
				comment,
				null,
				transformer
		);
		isNaturalKeyLookup = isLookupByNaturalKey;
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Map<String,TypedValue> namedParameters,
			final LockOptions lockOptions,
			final RowSelection rowSelection,
			final boolean isReadOnlyInitialized,
			final boolean readOnly,
			final boolean cacheable,
			final String cacheRegion,
			//final boolean forceCacheRefresh,
			final String comment,
			final Serializable[] collectionKeys,
			ResultTransformer transformer) {
		this.positionalParameterTypes = positionalParameterTypes;
		this.positionalParameterValues = positionalParameterValues;
		this.namedParameters = namedParameters;
		this.lockOptions = lockOptions;
		this.rowSelection = rowSelection;
		//this.forceCacheRefresh = forceCacheRefresh;
		this.comment = comment;
		this.isReadOnlyInitialized = isReadOnlyInitialized;
		this.readOnly = readOnly;
		this.resultTransformer = transformer;
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Map<String,TypedValue> namedParameters,
			final LockOptions lockOptions,
			final RowSelection rowSelection,
			final boolean isReadOnlyInitialized,
			final boolean readOnly,
			final boolean cacheable,
			final String cacheRegion,
			//final boolean forceCacheRefresh,
			final String comment,
			final Serializable[] collectionKeys,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final ResultTransformer transformer) {
		this(
				positionalParameterTypes,
				positionalParameterValues,
				namedParameters,
				lockOptions,
				rowSelection,
				isReadOnlyInitialized,
				readOnly,
				cacheable,
				cacheRegion,
				comment,
				collectionKeys,
				transformer
		);
		this.optionalEntityName = optionalEntityName;
		this.optionalId = optionalId;
		this.optionalObject = optionalObject;
	}

	
	public void setPositionalParameterTypes(Type[] types) {
		positionalParameterTypes = types;
	}
	
	public void setPositionalParameterValues(Object[] objects) {
		positionalParameterValues = objects;
	}
	
	public void setOptionalObject(Object optionalObject) {
		this.optionalObject = optionalObject;
	}
	
	public void setOptionalEntityName(String optionalEntityName) {
		this.optionalEntityName = optionalEntityName;
	}
	
	public void setOptionalId(Serializable optionalId) {
		this.optionalId = optionalId;
	}
	
	public void setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}

	public boolean isReadOnlyInitialized() {
		return isReadOnlyInitialized;
	}

	public boolean isReadOnly() {
		if ( ! isReadOnlyInitialized() ) {
			throw new IllegalStateException( "cannot call isReadOnly() when isReadOnlyInitialized() returns false" );
		}
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		this.isReadOnlyInitialized = true;
	}

	public RowSelection getRowSelection() {
		return rowSelection;
	}
	
	public Type[] getPositionalParameterTypes() {
		return positionalParameterTypes;
	}

	public Object[] getPositionalParameterValues() {
		return positionalParameterValues;
	}

	public void processFilters(String sql, SessionImplementor session) {
		processFilters( sql, session.getLoadQueryInfluencers().getEnabledFilters(), session.getFactory() );
	}
	
	public void processFilters(String sql, Map filters, SessionFactoryImplementor factory) {
		if ( filters.size() == 0 || !sql.contains( ParserHelper.HQL_VARIABLE_PREFIX ) ) {
			// HELLA IMPORTANT OPTIMIZATION!!!
			processedPositionalParameterValues = getPositionalParameterValues();
			processedPositionalParameterTypes = getPositionalParameterTypes();
			processedSQL = sql;
		}
		else {
//			final Dialect dialect = factory.getDialect();
//			String symbols = new StringBuilder().append( ParserHelper.HQL_SEPARATORS )
//					.append( dialect.openQuote() )
//					.append( dialect.closeQuote() )
//					.toString();
//			StringTokenizer tokens = new StringTokenizer( sql, symbols, true );
//			StringBuilder result = new StringBuilder();
//
//			List parameters = new ArrayList();
//			List parameterTypes = new ArrayList();
//
//			int positionalIndex = 0;
//			while ( tokens.hasMoreTokens() ) {
//				final String token = tokens.nextToken();
//				if ( token.startsWith( ParserHelper.HQL_VARIABLE_PREFIX ) ) {
//					final String filterParameterName = token.substring( 1 );
//					final String[] parts = LoadQueryInfluencers.parseFilterParameterName( filterParameterName );
//					final FilterImpl filter = ( FilterImpl ) filters.get( parts[0] );
//					final Object value = filter.getParameter( parts[1] );
//					final Type type = filter.getFilterDefinition().getParameterType( parts[1] );
//					if ( value != null && Collection.class.isAssignableFrom( value.getClass() ) ) {
//						Iterator itr = ( ( Collection ) value ).iterator();
//						while ( itr.hasNext() ) {
//							Object elementValue = itr.next();
//							result.append( '?' );
//							parameters.add( elementValue );
//							parameterTypes.add( type );
//							if ( itr.hasNext() ) {
//								result.append( ", " );
//							}
//						}
//					}
//					else {
//						result.append( '?' );
//						parameters.add( value );
//						parameterTypes.add( type );
//					}
//				}
//				else {
//					if ( "?".equals( token ) && positionalIndex < getPositionalParameterValues().length ) {
//						parameters.add( getPositionalParameterValues()[positionalIndex] );
//						parameterTypes.add( getPositionalParameterTypes()[positionalIndex] );
//						positionalIndex++;
//					}
//					result.append( token );
//				}
			}
//			processedPositionalParameterValues = parameters.toArray();
//			processedPositionalParameterTypes = ( Type[] ) parameterTypes.toArray( new Type[parameterTypes.size()] );
//			processedSQL = result.toString();
//		}
	}

	public String getFilteredSQL() {
		return processedSQL;
	}

	public String getComment() {
		return comment;
	}

	public boolean isCallable() {
		return callable;
	}

	public ScrollMode getScrollMode() {
		return scrollMode;
	}

	public Object[] getFilteredPositionalParameterValues() {
		return processedPositionalParameterValues;
	}

	public Type[] getFilteredPositionalParameterTypes() {
		return processedPositionalParameterTypes;
	}

	public Map<String,TypedValue> getNamedParameters() {
		return namedParameters;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public boolean hasAutoDiscoverScalarTypes() {
		return autodiscovertypes ;
	}

	public Object getOptionalObject() {
		return optionalObject;
	}
	
	public Serializable getOptionalId() {
		return optionalId;
	}
	
	public String getOptionalEntityName() {
		return optionalEntityName;
	}

	public  ResultTransformer getResultTransformer() {
		return  resultTransformer;
	}

	public boolean isReadOnly(SessionImplementor session) {
		return isReadOnlyInitialized
				? isReadOnly()
				: session.getPersistenceContext().isDefaultReadOnly();
	}

	public void validateParameters() throws QueryException{
		int types = positionalParameterTypes == null ? 0 : positionalParameterTypes.length;
		int values = positionalParameterValues == null ? 0 : positionalParameterValues.length;
		if ( types != values ) {
			throw new QueryException(
					"Number of positional parameter types:" + types +
							" does not match number of positional parameters: " + values
			);
		}
	}

	public QueryParameters createCopyUsing(RowSelection selection) {
		QueryParameters copy = new QueryParameters(
				this.positionalParameterTypes,
				this.positionalParameterValues,
				this.namedParameters,
				this.lockOptions,
				selection,
				this.isReadOnlyInitialized,
				this.readOnly,
				this.cacheable,
				this.cacheRegion,
				this.comment,
				this.collectionKeys,
				this.optionalObject,
				this.optionalEntityName,
				this.optionalId,
				this.resultTransformer
		);
		copy.processedSQL = this.processedSQL;
		copy.processedPositionalParameterTypes = this.processedPositionalParameterTypes;
		copy.processedPositionalParameterValues = this.processedPositionalParameterValues;
		return copy;
	}

	public boolean isCacheable() {
		return cacheable;
	}
	
}

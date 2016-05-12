package com.zorm.query;

import java.io.Serializable;
import java.util.*;

import com.zorm.FlushMode;
import com.zorm.LockOptions;
import com.zorm.dialect.Dialect;
import com.zorm.engine.RowSelection;
import com.zorm.engine.TypedValue;
import com.zorm.entity.ResultTransformer;
import com.zorm.exception.QueryException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;
import com.zorm.type.StandardBasicTypes;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;
import com.zorm.util.MarkerObject;
import com.zorm.util.ParserHelper;
import com.zorm.util.StringHelper;

public abstract class AbstractQueryImpl implements Query{
	private static final Object UNSET_PARAMETER = new MarkerObject("<unset parameter>");
	private static final Object UNSET_TYPE = new MarkerObject("<unset type>");
	
	private final String queryString;
	protected final SessionImplementor session;
	protected final ParameterMetadata parameterMetadata;
	
	// parameter bind values...
    private List values = new ArrayList(4);
	private List types = new ArrayList(4);
	private Map<String,TypedValue> namedParameters = new HashMap<String, TypedValue>(4);
	private Map namedParameterLists = new HashMap(4);

	private Object optionalObject;
	private Serializable optionalId;
	private String optionalEntityName;
	
	private RowSelection selection;
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private FlushMode flushMode;
//	private CacheMode cacheMode;
	private FlushMode sessionFlushMode;
//	private CacheMode sessionCacheMode;
	private Serializable collectionKey;
	private Boolean readOnly;
	private ResultTransformer resultTransformer;
	
	public AbstractQueryImpl(
			String queryString,
	        FlushMode flushMode,
	        SessionImplementor session,
	        ParameterMetadata parameterMetadata) {
		this.session = session;
		this.queryString = queryString;
		this.selection = new RowSelection();
		this.flushMode = flushMode;
		this.parameterMetadata = parameterMetadata;
	}
	
	public abstract LockOptions getLockOptions();
	
	@Override
	public Query setComment(String comment) {
		this.comment = comment;
		return this;
	}
	
	@Override
	public String getQueryString() {
		return queryString;
	}
	
	@Override
	public Query setInteger(int position, int val) {
		setParameter(position, val, StandardBasicTypes.INTEGER);
		return this;
	}
	
	public Query setParameter(int position, Object val, Type type) {
		if ( parameterMetadata.getOrdinalParameterCount() == 0 ) {
			throw new IllegalArgumentException("No positional parameters in query: " + getQueryString() );
		}
		if ( position < 0 || position > parameterMetadata.getOrdinalParameterCount() - 1 ) {
			throw new IllegalArgumentException("Positional parameter does not exist: " + position + " in query: " + getQueryString() );
		}
		int size = values.size();
		if ( position < size ) {
			values.set( position, val );
			types.set( position, type );
		}
		else {
			// prepend value and type list with null for any positions before the wanted position.
			for ( int i = 0; i < position - size; i++ ) {
				values.add( UNSET_PARAMETER );
				types.add( UNSET_TYPE );
			}
			values.add( val );
			types.add( type );
		}
		return this;
	}
	
	protected List getTypes() {
		return types;
	}
	
	protected List getValues() {
		return values;
	}
	
	public RowSelection getSelection() {
		return selection;
	}
	
	public boolean isReadOnly() {
		return ( readOnly == null ?
				getSession().getPersistenceContext().isDefaultReadOnly() :
				readOnly.booleanValue() 
		);
	}
	
	public Type[] typeArray() {
		return ArrayHelper.toTypeArray( getTypes() );
	}
	
	public Object[] valueArray() {
		return getValues().toArray();
	}
	
	public QueryParameters getQueryParameters(Map namedParams) {
		return new QueryParameters(
				typeArray(),
				valueArray(),
				namedParams,
				getLockOptions(),
				getSelection(),
				true,
				isReadOnly(),
				cacheable,
				cacheRegion,
				comment,
				collectionKey == null ? null : new Serializable[] { collectionKey },
				optionalObject,
				optionalEntityName,
				optionalId,
				resultTransformer
		);
	}
	
	protected void after() {
		if (sessionFlushMode!=null) {
			getSession().setFlushMode(sessionFlushMode);
			sessionFlushMode = null;
		}
	}
	
	private String expandParameterList(String query, String name, TypedValue typedList, Map namedParamsCopy) {
		Collection vals = (Collection) typedList.getValue();
		
		// HHH-1123
		// Some DBs limit number of IN expressions.  For now, warn...
		final Dialect dialect = session.getFactory().getDialect();
		final int inExprLimit = dialect.getInExpressionCountLimit();

		Type type = typedList.getType();

		boolean isJpaPositionalParam = parameterMetadata.getNamedParameterDescriptor( name ).isJpaStyle();
		String paramPrefix = isJpaPositionalParam ? "?" : ParserHelper.HQL_VARIABLE_PREFIX;
		String placeholder =
				new StringBuilder( paramPrefix.length() + name.length() )
						.append( paramPrefix ).append(  name )
						.toString();

		if ( query == null ) {
			return query;
		}
		int loc = query.indexOf( placeholder );

		if ( loc < 0 ) {
			return query;
		}

		String beforePlaceholder = query.substring( 0, loc );
		String afterPlaceholder =  query.substring( loc + placeholder.length() );

		// check if placeholder is already immediately enclosed in parentheses
		// (ignoring whitespace)
		boolean isEnclosedInParens =
				StringHelper.getLastNonWhitespaceCharacter( beforePlaceholder ) == '(' &&
				StringHelper.getFirstNonWhitespaceCharacter( afterPlaceholder ) == ')';

		if ( vals.size() == 1  && isEnclosedInParens ) {
			// short-circuit for performance when only 1 value and the
			// placeholder is already enclosed in parentheses...
			namedParamsCopy.put( name, new TypedValue( type, vals.iterator().next() ) );
			return query;
		}

		StringBuilder list = new StringBuilder( 16 );
		Iterator iter = vals.iterator();
		int i = 0;
		while ( iter.hasNext() ) {
			String alias = ( isJpaPositionalParam ? 'x' + name : name ) + i++ + '_';
			namedParamsCopy.put( alias, new TypedValue( type, iter.next() ) );
			list.append( ParserHelper.HQL_VARIABLE_PREFIX ).append( alias );
			if ( iter.hasNext() ) {
				list.append( ", " );
			}
		}
		return StringHelper.replace(
				beforePlaceholder,
				afterPlaceholder,
				placeholder.toString(),
				list.toString(),
				true,
				true
		);
	}
	
	protected String expandParameterLists(Map namedParamsCopy) {
		String query = this.queryString;
		Iterator iter = namedParameterLists.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			query = expandParameterList( query, (String) me.getKey(), (TypedValue) me.getValue(), namedParamsCopy );
		}
		return query;
	}
	
	SessionImplementor getSession() {
		return session;
	}
	
	protected void before() {
		if ( flushMode!=null ) {
			sessionFlushMode = getSession().getFlushMode();
			getSession().setFlushMode(flushMode);
		}
//		if ( cacheMode!=null ) {
//			sessionCacheMode = getSession().getCacheMode();
//			getSession().setCacheMode(cacheMode);
//		}
	}
	
	protected Map getNamedParams() {
		return new HashMap( namedParameters );
	}
	
	protected void verifyParameters() throws QueryException {
		verifyParameters(false);
	}
	
	protected void verifyParameters(boolean reserveFirstParameter) throws ZormException {
		if ( parameterMetadata.getNamedParameterNames().size() != namedParameters.size() + namedParameterLists.size() ) {
			Set missingParams = new HashSet( parameterMetadata.getNamedParameterNames() );
			missingParams.removeAll( namedParameterLists.keySet() );
			missingParams.removeAll( namedParameters.keySet() );
			throw new QueryException( "Not all named parameters have been set: " + missingParams, getQueryString() );
		}

		int positionalValueSpan = 0;
		for ( int i = 0; i < values.size(); i++ ) {
			Object object = types.get( i );
			if( values.get( i ) == UNSET_PARAMETER || object == UNSET_TYPE ) {
				if ( reserveFirstParameter && i==0 ) {
					continue;
				}
				else {
					throw new QueryException( "Unset positional parameter at position: " + i, getQueryString() );
				}
			}
			positionalValueSpan += ( (Type) object ).getColumnSpan( session.getFactory() );
		}

		if ( parameterMetadata.getOrdinalParameterCount() != positionalValueSpan ) {
			if ( reserveFirstParameter && parameterMetadata.getOrdinalParameterCount() - 1 != positionalValueSpan ) {
				throw new QueryException(
				 		"Expected positional parameter count: " +
				 		(parameterMetadata.getOrdinalParameterCount()-1) +
				 		", actual parameters: " +
				 		values,
				 		getQueryString()
				 	);
			}
			else if ( !reserveFirstParameter ) {
				throw new QueryException(
				 		"Expected positional parameter count: " +
				 		parameterMetadata.getOrdinalParameterCount() +
				 		", actual parameters: " +
				 		values,
				 		getQueryString()
				 	);
			}
		}
	}
}

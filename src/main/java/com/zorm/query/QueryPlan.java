package com.zorm.query;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.engine.RowSelection;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImpl;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;
import com.zorm.util.IdentitySet;

@SuppressWarnings("unused")
public class QueryPlan implements Serializable{
	private static final long serialVersionUID = -141689559787493242L;
	private static final Log log = LogFactory.getLog(QueryPlan.class);
	
	private final String sourceQuery;
	private final QueryTranslator[] translators;
	
	private final String[] sqlStrings;
	
	private final ParameterMetadata parameterMetadata;
	private final ReturnMetadata returnMetadata;
	private final Set querySpaces;

	private final Set enabledFilterNames;
	private final boolean shallow;
	
	public QueryPlan(String hql, boolean shallow, Map enabledFilters, SessionFactoryImplementor factory) {
		this( hql, null, shallow, enabledFilters, factory );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected QueryPlan(String hql, String collectionRole, boolean shallow, Map enabledFilters,  SessionFactoryImplementor factory){
		//查询语句
		this.sourceQuery = hql;
		this.shallow = shallow;
		Set copy = new HashSet();
		copy.addAll( enabledFilters.keySet() );
		this.enabledFilterNames = java.util.Collections.unmodifiableSet( copy );
        //将query语句中的类名替换成全限定名
		final String[] concreteQueryStrings = QuerySplitter.concreteQueries( hql, factory );
		final int length = concreteQueryStrings.length;
		this.translators = new QueryTranslator[length];

		List<String> sqlStringList = new ArrayList<String>();
		Set combinedQuerySpaces = new HashSet();

		final boolean hasCollectionRole = (collectionRole == null);
		final Map querySubstitutions = factory.getSettings().getQuerySubstitutions();
		final QueryTranslatorFactory queryTranslatorFactory = factory.getSettings().getQueryTranslatorFactory();

		for ( int i=0; i<length; i++ ) {
			if ( hasCollectionRole ) {
				translators[i] = queryTranslatorFactory
						.createQueryTranslator( hql, concreteQueryStrings[i], enabledFilters, factory );
				//解析query语句
				translators[i].compile( querySubstitutions, shallow );
			}
			else {
			}
			combinedQuerySpaces.addAll( translators[i].getQuerySpaces() );
			sqlStringList.addAll( translators[i].collectSqlStrings() );
		}

		this.sqlStrings = ArrayHelper.toStringArray( sqlStringList );
		this.querySpaces = combinedQuerySpaces;

		if ( length == 0 ) {
			parameterMetadata = new ParameterMetadata( null, null );
			returnMetadata = null;
		}
		else {
			this.parameterMetadata = buildParameterMetadata( translators[0].getParameterTranslations(), hql );
			if ( translators[0].isManipulationStatement() ) {
				returnMetadata = null;
			}
			else {
				final Type[] types = ( length > 1 ) ? new Type[translators[0].getReturnTypes().length] : translators[0].getReturnTypes();
				returnMetadata = new ReturnMetadata( translators[0].getReturnAliases(), types );
			}
		}
	}
	
	private ParameterMetadata buildParameterMetadata(ParameterTranslations parameterTranslations, String hql) {
		long start = System.currentTimeMillis();
		ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( hql );
		long end = System.currentTimeMillis();

		int ordinalParamCount = parameterTranslations.getOrdinalParameterCount();
		int[] locations = ArrayHelper.toIntArray( recognizer.getOrdinalParameterLocationList() );
		if ( parameterTranslations.supportsOrdinalParameterMetadata() && locations.length != ordinalParamCount ) {
			throw new ZormException( "ordinal parameter mismatch" );
		}
		ordinalParamCount = locations.length;
		OrdinalParameterDescriptor[] ordinalParamDescriptors = new OrdinalParameterDescriptor[ordinalParamCount];
		for ( int i = 1; i <= ordinalParamCount; i++ ) {
			ordinalParamDescriptors[ i - 1 ] = new OrdinalParameterDescriptor(
					i,
			        parameterTranslations.supportsOrdinalParameterMetadata()
		                    ? parameterTranslations.getOrdinalParameterExpectedType( i )
		                    : null,
			        locations[ i - 1 ]
			);
		}

		Iterator itr = recognizer.getNamedParameterDescriptionMap().entrySet().iterator();
		Map<String, NamedParameterDescriptor> namedParamDescriptorMap = new HashMap<String, NamedParameterDescriptor>();
		while( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final String name = ( String ) entry.getKey();
			final ParamLocationRecognizer.NamedParameterDescription description =
					( ParamLocationRecognizer.NamedParameterDescription ) entry.getValue();
			namedParamDescriptorMap.put(
					name,
					new NamedParameterDescriptor(
							name,
					        parameterTranslations.getNamedParameterExpectedType( name ),
					        description.buildPositionsArray(),
					        description.isJpaStyle()
					)
			);
		}

		return new ParameterMetadata( ordinalParamDescriptors, namedParamDescriptorMap );
	}

	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	public List performList(
			QueryParameters queryParameters,
	        SessionImplementor session) throws ZormException {
		boolean hasLimit = queryParameters.getRowSelection() != null &&
		                   queryParameters.getRowSelection().definesLimits();
		boolean needsLimit = hasLimit && translators.length > 1;
		QueryParameters queryParametersToUse;
		if ( needsLimit ) {
			RowSelection selection = new RowSelection();
			selection.setFetchSize( queryParameters.getRowSelection().getFetchSize() );
			selection.setTimeout( queryParameters.getRowSelection().getTimeout() );
			queryParametersToUse = queryParameters.createCopyUsing( selection );
		}
		else {
			queryParametersToUse = queryParameters;
		}

		List combinedResults = new ArrayList();
		IdentitySet distinction = new IdentitySet();
		int includedCount = -1;
		translator_loop:
		for ( QueryTranslator translator : translators ) {
			List tmp = translator.list( session, queryParametersToUse );
			if ( needsLimit ) {
				// NOTE : firstRow is zero-based
				final int first = queryParameters.getRowSelection().getFirstRow() == null
						? 0
						: queryParameters.getRowSelection().getFirstRow();
				final int max = queryParameters.getRowSelection().getMaxRows() == null
						? -1
						: queryParameters.getRowSelection().getMaxRows();
				for ( final Object result : tmp ) {
					if ( !distinction.add( result ) ) {
						continue;
					}
					includedCount++;
					if ( includedCount < first ) {
						continue;
					}
					combinedResults.add( result );
					if ( max >= 0 && includedCount > max ) {
						// break the outer loop !!!
						break translator_loop;
					}
				}
			}
			else {
				combinedResults.addAll( tmp );
			}
		}
		return combinedResults;
	}

	public int performExecuteUpdate(QueryParameters queryParameters,
			SessionImpl session) {
		int result = 0;
		for ( QueryTranslator translator : translators ) {
			result += translator.executeUpdate( queryParameters, session );
		}
		return result;
	}

}

package com.zorm.query;

import java.io.Serializable;




import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;






import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.config.Environment;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.BoundedConcurrentHashMap;
import com.zorm.util.CollectionHelper;
import com.zorm.util.ConfigurationHelper;


public class QueryPlanCache implements Serializable{
	private static final long serialVersionUID = 1801489603139752753L;
	private static final Log log = LogFactory.getLog(QueryPlanCache.class);
	
	public static final int DEFAULT_PARAMETER_METADATA_MAX_COUNT = 128;
	public static final int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;
	private final SessionFactoryImplementor factory;
	private final BoundedConcurrentHashMap queryPlanCache;
	private final BoundedConcurrentHashMap<String,ParameterMetadata> parameterMetadataCache;
	
	public QueryPlanCache(final SessionFactoryImplementor factory) {
		this.factory = factory;

		Integer maxParameterMetadataCount = ConfigurationHelper.getInteger(
				Environment.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE,
				factory.getProperties()
		);
		if ( maxParameterMetadataCount == null ) {
			maxParameterMetadataCount = ConfigurationHelper.getInt(
					Environment.QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES,
					factory.getProperties(),
					DEFAULT_PARAMETER_METADATA_MAX_COUNT
			);
		}
		Integer maxQueryPlanCount = ConfigurationHelper.getInteger(
				Environment.QUERY_PLAN_CACHE_MAX_SIZE,
				factory.getProperties()
		);
		if ( maxQueryPlanCount == null ) {
			maxQueryPlanCount = ConfigurationHelper.getInt(
					Environment.QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES,
					factory.getProperties(),
					DEFAULT_QUERY_PLAN_MAX_COUNT
			);
		}

		queryPlanCache = new BoundedConcurrentHashMap( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
		parameterMetadataCache = new BoundedConcurrentHashMap<String, ParameterMetadata>(
				maxParameterMetadataCount,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);

	}

	public QueryPlan getQueryPlan(String queryString, boolean shallow, Map enabledFilters) 
	   throws QueryException,MappingException{
		QueryPlanKey key = new QueryPlanKey( queryString, shallow, enabledFilters );
		QueryPlan value = (QueryPlan)queryPlanCache.get(key);
		if(value==null){
			log.debug("Unable to locate query plan in cache; generating "+queryString);
			value = new QueryPlan(queryString,shallow,enabledFilters,factory);
			queryPlanCache.putIfAbsent(key, value);
		}else{
			log.debug("Located query plan in cache "+queryString);
		}
		return value;
	}
	
	private static class QueryPlanKey implements Serializable {
		private final String query;
		private final boolean shallow;
		private final Set<DynamicFilterKey> filterKeys;
		private final int hashCode;

		public QueryPlanKey(String query, boolean shallow, Map enabledFilters) {
			this.query = query;
			this.shallow = shallow;
			if ( CollectionHelper.isEmpty( enabledFilters ) ) {
				filterKeys = Collections.emptySet();
			}
			else {
				Set<DynamicFilterKey> tmp = new HashSet<DynamicFilterKey>(
						CollectionHelper.determineProperSizing( enabledFilters ),
						CollectionHelper.LOAD_FACTOR
				);
				for ( Object o : enabledFilters.values() ) {
					tmp.add( new DynamicFilterKey( (FilterImpl) o ) );
				}
				this.filterKeys = Collections.unmodifiableSet( tmp );
			}

			int hash = query.hashCode();
			hash = 29 * hash + ( shallow ? 1 : 0 );
			hash = 29 * hash + filterKeys.hashCode();
			this.hashCode = hash;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final QueryPlanKey that = ( QueryPlanKey ) o;

			return shallow == that.shallow
					&& filterKeys.equals( that.filterKeys )
					&& query.equals( that.query );

		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}
	
	private static class DynamicFilterKey implements Serializable {
		private final String filterName;
		private final Map<String,Integer> parameterMetadata;
		private final int hashCode;

		@SuppressWarnings({ "UnnecessaryBoxing" })
		private DynamicFilterKey(FilterImpl filter) {
			this.filterName = filter.getName();
			if ( filter.getParameters().isEmpty() ) {
				parameterMetadata = Collections.emptyMap();
			}
			else {
				parameterMetadata = new HashMap<String,Integer>(
						CollectionHelper.determineProperSizing( filter.getParameters() ),
						CollectionHelper.LOAD_FACTOR
				);
				for ( Object o : filter.getParameters().entrySet() ) {
					final Map.Entry entry = (Map.Entry) o;
					final String key = (String) entry.getKey();
					final Integer valueCount;
					if ( Collection.class.isInstance( entry.getValue() ) ) {
						valueCount = ( (Collection) entry.getValue() ).size();
					}
					else {
						valueCount = 1;
					}
					parameterMetadata.put( key, valueCount );
				}
			}

			int hash = filterName.hashCode();
			hash = 31 * hash + parameterMetadata.hashCode();
			this.hashCode = hash;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			DynamicFilterKey that = ( DynamicFilterKey ) o;

			return filterName.equals( that.filterName )
					&& parameterMetadata.equals( that.parameterMetadata );

		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}
}

package com.zorm.config;

import java.util.*;

import com.zorm.mapping.PersistentClass;
import com.zorm.persister.entity.Joinable;
import com.zorm.session.SessionFactoryImplementor;

public class FilterConfiguration {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap;
	private final Map<String, String> aliasEntityMap;
	private final PersistentClass persistentClass;
	
	public FilterConfiguration(String name, String condition, boolean autoAliasInjection, Map<String, String> aliasTableMap, Map<String, String> aliasEntityMap, PersistentClass persistentClass) {
		this.name = name;
		this.condition = condition;
		this.autoAliasInjection = autoAliasInjection;
		this.aliasTableMap = aliasTableMap;
		this.aliasEntityMap = aliasEntityMap;
		this.persistentClass = persistentClass;
	}

	public String getName() {
		return name;
	}

	public String getCondition() {
		return condition;
	}

	public boolean useAutoAliasInjection() {
		return autoAliasInjection;
	}

	public Map<String, String> getAliasTableMap(SessionFactoryImplementor factory) {
		Map<String,String> mergedAliasTableMap = mergeAliasMaps(factory);
		if (!mergedAliasTableMap.isEmpty()){
			return mergedAliasTableMap;
		} else if (persistentClass != null){
			String table = persistentClass.getTable().getQualifiedName(factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName());
			return Collections.singletonMap(null, table);
		} else{
			return Collections.emptyMap();
		}
	}
	
	private Map<String,String> mergeAliasMaps(SessionFactoryImplementor factory){
		Map<String,String> ret = new HashMap<String, String>();
		if (aliasTableMap != null){
			ret.putAll(aliasTableMap);
		}
		if (aliasEntityMap != null){
			for (Map.Entry<String, String> entry : aliasEntityMap.entrySet()){
				ret.put(entry.getKey(), 
						Joinable.class.cast(factory.getEntityPersister(entry.getValue())).getTableName());
			}
		}
		return ret;
	}
}

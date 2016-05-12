package com.zorm.engine;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zorm.Filter;
import com.zorm.session.SessionFactoryImplementor;

public class LoadQueryInfluencers implements Serializable{
	public static LoadQueryInfluencers NONE = new LoadQueryInfluencers();
	private final SessionFactoryImplementor sessionFactory;
	private String internalFetchProfile;
	private Map<String,Filter> enabledFilters;
	private Set<String> enabledFetchProfileNames;
	
	public LoadQueryInfluencers() {
		this( null, Collections.<String, Filter>emptyMap(), Collections.<String>emptySet() );
	}
	
	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, new HashMap<String,Filter>(), new HashSet<String>() );
	}

	private LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, Map<String,Filter> enabledFilters, Set<String> enabledFetchProfileNames) {
		this.sessionFactory = sessionFactory;
		this.enabledFilters = enabledFilters;
		this.enabledFetchProfileNames = enabledFetchProfileNames;
	}

	public Map<String, Filter> getEnabledFilters() {
		for ( Filter filter : enabledFilters.values() ) {
			filter.validate();
		}
		return enabledFilters;
	}
}

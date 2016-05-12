package com.zorm.query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.zorm.Filter;

public class FilterImpl implements Filter, Serializable {

	private static final long serialVersionUID = 3456763716591326257L;
	public static final String MARKER = "$FILTER_PLACEHOLDER$";

	private transient FilterDefinition definition;
	private String filterName;
	private Map<String,Object> parameters = new HashMap<String, Object>();
	
	
	public FilterImpl(FilterDefinition configuration) {
		this.definition = configuration;
		filterName = definition.getFilterName();
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Filter setParameter(String name, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String,?> getParameters() {
		return parameters;
	}

	@Override
	public void validate() {
	}
}

package com.zorm.query;

import java.util.List;
import java.util.Map;

import com.zorm.FlushMode;
import com.zorm.LockOptions;
import com.zorm.session.SessionImplementor;

public class QueryImpl  extends AbstractQueryImpl{
	private LockOptions lockOptions = new LockOptions();
	
	public QueryImpl(
			String queryString,
	        FlushMode flushMode,
	        SessionImplementor session,
	        ParameterMetadata parameterMetadata) {
		super( queryString, flushMode, session, parameterMetadata );
	}

	public QueryImpl(String queryString, SessionImplementor session, ParameterMetadata parameterMetadata) {
		this( queryString, null, session, parameterMetadata );
	}

	@Override
	public List list() {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();
		try {
			return getSession().list(
					expandParameterLists(namedParams),
			        getQueryParameters(namedParams)
				);
		}
		finally {
			after();
		}
	}
	
	public LockOptions getLockOptions() {
		return lockOptions;
	}
	
	@Override
	public int executeUpdate() {
        verifyParameters();
        Map namedParams = getNamedParams();
        before();
    	try {
            return getSession().executeUpdate(
                    expandParameterLists( namedParams ),
                    getQueryParameters( namedParams )
	            );
		}
		finally {
			after();
		}
	}

	@Override
	public Query setString(int position, String val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Query setDouble(String name, double val) {
		// TODO Auto-generated method stub
		return null;
	}
}

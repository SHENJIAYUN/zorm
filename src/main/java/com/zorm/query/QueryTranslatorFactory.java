package com.zorm.query;

import java.util.Map;

import com.zorm.session.SessionFactoryImplementor;

public interface QueryTranslatorFactory {
	public QueryTranslator createQueryTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory);

	public QueryTranslator createFilterTranslator(String hql, String string,
			Map enabledFilters, SessionFactoryImplementor factory);
}

package com.zorm.query;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.session.SessionFactoryImplementor;

public class ASTQueryTranslatorFactory implements QueryTranslatorFactory{

	private static final Log log = LogFactory.getLog(ASTQueryTranslatorFactory.class);
	
	public ASTQueryTranslatorFactory() {
       log.info("Using ASTQueryTranslatorFactory");
	}

	@Override
	public QueryTranslator createQueryTranslator(String queryIdentifier,
			String queryString, Map filters, SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

	@Override
	public QueryTranslator createFilterTranslator(String hql, String string,
			Map enabledFilters, SessionFactoryImplementor factory) {
		return null;
	}
}

package com.zorm.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.session.SessionImpl;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;


/*
 * 将自定义查询语句转化为sql语句
 */
public interface QueryTranslator {

	public static final String ERROR_CANNOT_FETCH_WITH_ITERATE = "fetch may not be used with scroll() or iterate()";
	public static final String ERROR_NAMED_PARAMETER_DOES_NOT_APPEAR = "Named parameter does not appear in Query: ";
    public static final String ERROR_CANNOT_DETERMINE_TYPE = "Could not determine type of: ";
	public static final String ERROR_CANNOT_FORMAT_LITERAL =  "Could not format constant value to SQL literal: ";
	
	void compile(Map querySubstitutions, boolean shallow) throws QueryException, MappingException;

	Set getQuerySpaces();

	List<String> collectSqlStrings();

	ParameterTranslations getParameterTranslations();

	boolean isManipulationStatement();

	Type[] getReturnTypes();

	public String getQueryString() ;
	
	String[] getReturnAliases();

	List list(SessionImplementor session, QueryParameters queryParameters);

	int executeUpdate(QueryParameters queryParameters, SessionImpl session);

}

package com.zorm.query;

import java.sql.*;
import java.util.Iterator;
import java.util.Map;

import com.zorm.engine.TypedValue;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class ParameterBinder {


	public static interface NamedParameterSource {
		public int[] getNamedParameterLocations(String name);
	}

	private ParameterBinder() {
	}

	public static int bindQueryParameters(
	        final PreparedStatement st,
	        final QueryParameters queryParameters,
	        final int start,
	        final NamedParameterSource source,
	        SessionImplementor session) throws SQLException, ZormException {
		int col = start;
		col += bindPositionalParameters( st, queryParameters, col, session );
		col += bindNamedParameters( st, queryParameters, col, source, session );
		return col;
	}

	public static int bindPositionalParameters(
	        final PreparedStatement st,
	        final QueryParameters queryParameters,
	        final int start,
	        final SessionImplementor session) throws SQLException, ZormException {
		return bindPositionalParameters(
		        st,
		        queryParameters.getPositionalParameterValues(),
		        queryParameters.getPositionalParameterTypes(),
		        start,
		        session
		);
	}

	public static int bindPositionalParameters(
	        final PreparedStatement st,
	        final Object[] values,
	        final Type[] types,
	        final int start,
	        final SessionImplementor session) throws SQLException, ZormException {
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( st, values[i], start + span, session );
			span += types[i].getColumnSpan( session.getFactory() );
		}
		return span;
	}

	public static int bindNamedParameters(
	        final PreparedStatement ps,
	        final QueryParameters queryParameters,
	        final int start,
	        final NamedParameterSource source,
	        final SessionImplementor session) throws SQLException, ZormException {
		return bindNamedParameters( ps, queryParameters.getNamedParameters(), start, source, session );
	}

	public static int bindNamedParameters(
	        final PreparedStatement ps,
	        final Map namedParams,
	        final int start,
	        final NamedParameterSource source,
	        final SessionImplementor session) throws SQLException,ZormException {
		if ( namedParams != null ) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = ( Map.Entry ) iter.next();
				String name = ( String ) e.getKey();
				TypedValue typedval = (TypedValue) e.getValue();
				int[] locations = source.getNamedParameterLocations( name );
				for ( int i = 0; i < locations.length; i++ ) {
					typedval.getType().nullSafeSet( ps, typedval.getValue(), locations[i] + start, session );
				}
				result += locations.length;
			}
			return result;
		}
        return 0;
	}
}


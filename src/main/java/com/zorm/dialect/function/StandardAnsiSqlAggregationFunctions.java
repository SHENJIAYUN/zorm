package com.zorm.dialect.function;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.StandardBasicTypes;
import com.zorm.type.Type;

public class StandardAnsiSqlAggregationFunctions {
	/**
	 * Definition of a standard ANSI SQL compliant <tt>COUNT</tt> function
	 */
	public static class CountFunction extends StandardSQLFunction {
		public static final CountFunction INSTANCE = new CountFunction();

		public CountFunction() {
			super( "count", StandardBasicTypes.LONG );
		}

		@Override
		public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
			if ( arguments.size() > 1 ) {
				if ( "distinct".equalsIgnoreCase( arguments.get( 0 ).toString() ) ) {
					return renderCountDistinct( arguments );
				}
			}
			return super.render( firstArgumentType, arguments, factory );
		}

		private String renderCountDistinct(List arguments) {
			StringBuilder buffer = new StringBuilder();
			buffer.append( "count(distinct " );
			String sep = "";
			Iterator itr = arguments.iterator();
			itr.next(); // intentionally skip first
			while ( itr.hasNext() ) {
				buffer.append( sep )
						.append( itr.next() );
				sep = ", ";
			}
			return buffer.append( ")" ).toString();
		}
	}
	
	public static void primeFunctionMap(Map<String, SQLFunction> functionMap) {
		//functionMap.put( AvgFunction.INSTANCE.getName(), AvgFunction.INSTANCE );
		functionMap.put( CountFunction.INSTANCE.getName(), CountFunction.INSTANCE );
		//functionMap.put( MaxFunction.INSTANCE.getName(), MaxFunction.INSTANCE );
		//functionMap.put( MinFunction.INSTANCE.getName(), MinFunction.INSTANCE );
		//functionMap.put( SumFunction.INSTANCE.getName(), SumFunction.INSTANCE );
	}
}

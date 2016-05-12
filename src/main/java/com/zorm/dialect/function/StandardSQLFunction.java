package com.zorm.dialect.function;

import java.util.List;

import com.zorm.engine.Mapping;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.Type;

public class StandardSQLFunction implements SQLFunction{
	private final String name;
	private final Type registeredType;

	/**
	 * Construct a standard SQL function definition with a variable return type;
	 * the actual return type will depend on the types to which the function
	 * is applied.
	 * <p/>
	 * Using this form, the return type is considered non-static and assumed
	 * to be the type of the first argument.
	 *
	 * @param name The name of the function.
	 */
	public StandardSQLFunction(String name) {
		this( name, null );
	}

	/**
	 * Construct a standard SQL function definition with a static return type.
	 *
	 * @param name The name of the function.
	 * @param registeredType The static return type.
	 */
	public StandardSQLFunction(String name, Type registeredType) {
		this.name = name;
		this.registeredType = registeredType;
	}

	/**
	 * Function name accessor
	 *
	 * @return The function name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Function static return type accessor.
	 *
	 * @return The static function return type; or null if return type is
	 * not static.
	 */
	public Type getType() {
		return registeredType;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getReturnType(Type firstArgumentType, Mapping mapping) {
		return registeredType == null ? firstArgumentType : registeredType;
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
		StringBuilder buf = new StringBuilder();
		buf.append( name ).append( '(' );
		for ( int i = 0; i < arguments.size(); i++ ) {
			buf.append( arguments.get( i ) );
			if ( i < arguments.size() - 1 ) {
				buf.append( ", " );
			}
		}
		return buf.append( ')' ).toString();
	}

	public String toString() {
		return name;
	}
}

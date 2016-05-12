package com.zorm.dialect.function;

import java.util.List;

import com.zorm.engine.Mapping;
import com.zorm.exception.QueryException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.Type;

/**
 * Provides support routines for the HQL functions as used
 * in the various SQL Dialects
 *
 * Provides an interface for supporting various HQL functions that are
 * translated to SQL. The Dialect and its sub-classes use this interface to
 * provide details required for processing of the function.
 *
 * @author JIA
 */
public interface SQLFunction {
	/**
	 * Does this function have any arguments?
	 *
	 * @return True if the function expects to have parameters; false otherwise.
	 */
	public boolean hasArguments();

	/**
	 * If there are no arguments, are parentheses required?
	 *
	 * @return True if a no-arg call of this function requires parentheses.
	 */
	public boolean hasParenthesesIfNoArguments();

	/**
	 * The return type of the function.  May be either a concrete type which is preset, or variable depending upon
	 * the type of the first function argument.
	 * <p/>
	 * Note, the 'firstArgumentType' parameter should match the one passed into {@link #render}
	 *
	 * @param firstArgumentType The type of the first argument
	 * @param mapping The mapping source.
	 *
	 * @return The type to be expected as a return.
	 *
	 * @throws org.hibernate.QueryException Indicates an issue resolving the return type.
	 */
	public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException;


	/**
	 * Render the function call as SQL fragment.
	 * <p/>
	 * Note, the 'firstArgumentType' parameter should match the one passed into {@link #getReturnType}
	 *
	 * @param firstArgumentType The type of the first argument
	 * @param arguments The function arguments
	 * @param factory The SessionFactory
	 *
	 * @return The rendered function call
	 *
	 * @throws org.hibernate.QueryException Indicates a problem rendering the
	 * function call.
	 */
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException;
}

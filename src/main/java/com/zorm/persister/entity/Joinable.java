package com.zorm.persister.entity;

import java.util.Map;

import com.zorm.exception.MappingException;

public interface Joinable {
	/**
	 * An identifying name; a class name or collection role name.
	 */
	public String getName();
	/**
	 * The table to join to.
	 */
	public String getTableName();

	/**
	 * All columns to select, when loading.
	 */
	public String selectFragment(Joinable rhs, String rhsAlias, String lhsAlias, String currentEntitySuffix, String currentCollectionSuffix, boolean includeCollectionColumns);

	/**
	 * Get the where clause part of any joins
	 * (optional operation)
	 */
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses);
	/**
	 * Get the from clause part of any joins
	 * (optional operation)
	 */
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses);
	/**
	 * The columns to join on
	 */
	public String[] getKeyColumnNames();
	/**
	 * Get the where clause filter, given a query alias and considering enabled session filters
	 */
	public String filterFragment(String alias, Map enabledFilters) throws MappingException;

	public String oneToManyFilterFragment(String alias) throws MappingException;
	/**
	 * Is this instance actually a CollectionPersister?
	 */
	public boolean isCollection();

	/**
	 * Very, very, very ugly...
	 *
	 * @return Does this persister "consume" entity column aliases in the result
	 * set?
	 */
	public boolean consumesEntityAlias();

	/**
	 * Very, very, very ugly...
	 *
	 * @return Does this persister "consume" collection column aliases in the result
	 * set?
	 */
	public boolean consumesCollectionAlias();
}

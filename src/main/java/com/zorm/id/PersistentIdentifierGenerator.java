package com.zorm.id;

import com.zorm.dialect.Dialect;
import com.zorm.exception.ZormException;

public interface PersistentIdentifierGenerator extends IdentifierGenerator{
	/**
	 * The configuration parameter holding the schema name
	 */
	public static final String SCHEMA = "schema";

	/**
	 * The configuration parameter holding the table name for the
	 * generated id
	 */
	public static final String TABLE = "target_table";

	/**
	 * The configuration parameter holding the table names for all
	 * tables for which the id must be unique
	 */
	public static final String TABLES = "identity_tables";

	/**
	 * The configuration parameter holding the primary key column
	 * name of the generated id
	 */
	public static final String PK = "target_column";

    /**
     * The configuration parameter holding the catalog name
     */
    public static final String CATALOG = "catalog";

	/**
	 * The key under whcih to find the {@link org.hibernate.cfg.ObjectNameNormalizer} in the config param map.
	 */
	public static final String IDENTIFIER_NORMALIZER = "identifier_normalizer";

	/**
	 * The SQL required to create the underlying database objects.
	 *
	 * @param dialect The dialect against which to generate the create command(s)
	 * @return The create command(s)
	 * @throws HibernateException problem creating the create command(s)
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws ZormException;

	/**
	 * The SQL required to remove the underlying database objects.
	 *
	 * @param dialect The dialect against which to generate the drop command(s)
	 * @return The drop command(s)
	 * @throws HibernateException problem creating the drop command(s)
	 */
	public String[] sqlDropStrings(Dialect dialect) throws ZormException;

	/**
	 * Return a key unique to the underlying database objects. Prevents us from
	 * trying to create/remove them multiple times.
	 * 
	 * @return Object an identifying key for this generator
	 */
	public Object generatorKey();
}

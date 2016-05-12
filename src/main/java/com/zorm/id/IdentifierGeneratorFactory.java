package com.zorm.id;

import java.util.Properties;

import com.zorm.dialect.Dialect;
import com.zorm.type.Type;

public interface IdentifierGeneratorFactory {
	/**
	 * Get the dialect.
	 *
	 * @return the dialect
	 */
	public Dialect getDialect();
	
	/**
	 * Allow injection of the dialect to use.
	 *
	 * @param dialect The dialect
	 */
	public void setDialect(Dialect dialect);
	
	/**
	 * Given a strategy, retrieve the appropriate identifier generator instance.
	 *
	 * @param strategy The generation strategy.
	 * @param type The mapping type for the identifier values.
	 * @param config Any configuraion properties given in the generator mapping.
	 *
	 * @return The appropriate generator instance.
	 */
	public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config);

	/**
	 * Retrieve the class that will be used as the {@link IdentifierGenerator} for the given strategy.
	 *
	 * @param strategy The strategy
	 * @return The generator class.
	 */
	public Class getIdentifierGeneratorClass(String strategy);
}

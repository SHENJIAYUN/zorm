package com.zorm.config;

import com.zorm.util.StringHelper;

/**
 * Provides centralized normalization of how database object names are handled.
 *
 * @author Steve Ebersole
 */
public abstract class ObjectNameNormalizer {

	/**
	 * Helper contract for dealing with {@link NamingStrategy} in different situations.
	 */
	public static interface NamingStrategyHelper {
		/**
		 * Called when the user supplied no explicit name/identifier for the given database object.
		 *
		 * @param strategy The naming strategy in effect
		 *
		 * @return The implicit name
		 */
		public String determineImplicitName(NamingStrategy strategy);

		/**
		 * Called when the user has supplied an explicit name for the database object.
		 *
		 * @param strategy The naming strategy in effect
		 * @param name The {@link ObjectNameNormalizer#normalizeIdentifierQuoting normalized} explicit object name.
		 *
		 * @return The strategy-handled name.
		 */
		public String handleExplicitName(NamingStrategy strategy, String name);
	}

	/**
	 * Performs the actual contract of normalizing a database name.
	 *
	 * @param explicitName The name the user explicitly gave for the database object.
	 * @param helper The {@link NamingStrategy} helper.
	 *
	 * @return The normalized identifier.
	 */
	public String normalizeDatabaseIdentifier(final String explicitName, NamingStrategyHelper helper) {
		String objectName = null;
		// apply naming strategy
		if ( StringHelper.isEmpty( explicitName ) ) {
			// No explicit name given, so allow the naming strategy the chance
			//    to determine it based on the corresponding mapped java name
			objectName = helper.determineImplicitName( getNamingStrategy() );
		}
		else {
			// An explicit name was given:
			//    in some cases we allow the naming strategy to "fine tune" these, but first
			//    handle any quoting for consistent handling in naming strategies
			objectName = normalizeIdentifierQuoting( explicitName );
			objectName = helper.handleExplicitName( getNamingStrategy(), objectName );
			return normalizeIdentifierQuoting( objectName );
		}
        // Conceivable that the naming strategy could return a quoted identifier, or
			//    that user enabled <delimited-identifiers/>
		return normalizeIdentifierQuoting( objectName );
	}

	/**
	 * Allow normalizing of just the quoting aspect of identifiers.  This is useful for
	 * schema and catalog in terms of initially making this public.
	 * <p/>
	 * This implements the rules set forth in JPA 2 (section "2.13 Naming of Database Objects") which
	 * states that the double-quote (") is the character which should be used to denote a <tt>quoted
	 * identifier</tt>.  Here, we handle recognizing that and converting it to the more elegant
	 * bactick (`) approach used in Hibernate..  Additionally we account for applying what JPA2 terms
	 *  
	 *
	 * @param identifier The identifier to be quoting-normalized.
	 * @return The identifier accounting for any quoting that need be applied.
	 */
	public String normalizeIdentifierQuoting(String identifier) {
		if ( StringHelper.isEmpty( identifier ) ) {
			return null;
		}

		// Convert the JPA2 specific quoting character (double quote) to Hibernate's (back tick)
		if ( identifier.startsWith( "\"" ) && identifier.endsWith( "\"" ) ) {
			return '`' + identifier.substring( 1, identifier.length() - 1 ) + '`';
		}

		// If the user has requested "global" use of quoted identifiers, quote this identifier (using back ticks)
		// if not already
		if ( isUseQuotedIdentifiersGlobally() && ! ( identifier.startsWith( "`" ) && identifier.endsWith( "`" ) ) ) {
			return '`' + identifier + '`';
		}

		return identifier;
	}

	/**
	 * Retrieve whether the user requested that all database identifiers be quoted.
	 *
	 * @return True if the user requested that all database identifiers be quoted, false otherwise.
	 */
	protected abstract boolean isUseQuotedIdentifiersGlobally();

	/**
	 * Get the current {@link NamingStrategy}.
	 *
	 * @return The current {@link NamingStrategy}.
	 */
	protected abstract NamingStrategy getNamingStrategy();
}
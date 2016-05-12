package com.zorm.meta;

import com.zorm.dialect.Dialect;
import com.zorm.exception.IllegalIdentifierException;
import com.zorm.util.StringHelper;

public class Identifier {
	private final String name;
	private final boolean isQuoted;

	/**
	 * Means to generate an {@link Identifier} instance from its simple name
	 *
	 * @param name The name
	 *
	 * @return The identifier form of the name.
	 */
	public static Identifier toIdentifier(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return null;
		}
		final String trimmedName = name.trim();
		if ( isQuoted( trimmedName ) ) {
			final String bareName = trimmedName.substring( 1, trimmedName.length() - 1 );
			return new Identifier( bareName, true );
		}
		else {
			return new Identifier( trimmedName, false );
		}
	}

	public static boolean isQuoted(String name) {
		return name.startsWith( "`" ) && name.endsWith( "`" );
	}

	/**
	 * Constructs an identifier instance.
	 *
	 * @param name The identifier text.
	 * @param quoted Is this a quoted identifier?
	 */
	public Identifier(String name, boolean quoted) {
		if ( StringHelper.isEmpty( name ) ) {
			throw new IllegalIdentifierException( "Identifier text cannot be null" );
		}
		if ( isQuoted( name ) ) {
			throw new IllegalIdentifierException( "Identifier text should not contain quote markers (`)" );
		}
		this.name = name;
		this.isQuoted = quoted;
	}

	/**
	 * Get the identifiers name (text)
	 *
	 * @return The name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Is this a quoted identifier>
	 *
	 * @return True if this is a quote identifier; false otherwise.
	 */
	public boolean isQuoted() {
		return isQuoted;
	}

	/**
	 * If this is a quoted identifier, then return the identifier name
	 * enclosed in dialect-specific open- and end-quotes; otherwise,
	 * simply return the identifier name.
	 *
	 * @param dialect The dialect whose dialect-specific quoting should be used.
	 * @return if quoted, identifier name enclosed in dialect-specific open- and end-quotes; otherwise, the
	 * identifier name.
	 */
	public String encloseInQuotesIfQuoted(Dialect dialect) {
		return isQuoted ?
				new StringBuilder( name.length() + 2 )
						.append( dialect.openQuote() )
						.append( name )
						.append( dialect.closeQuote() )
						.toString() :
				name;
	}

	@Override
	public String toString() {
		return isQuoted
				? '`' + getName() + '`'
				: getName();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Identifier that = (Identifier) o;

		return isQuoted == that.isQuoted
				&& name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}

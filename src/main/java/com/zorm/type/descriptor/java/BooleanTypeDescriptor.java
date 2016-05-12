package com.zorm.type.descriptor.java;

import com.zorm.type.descriptor.WrapperOptions;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class BooleanTypeDescriptor extends AbstractTypeDescriptor<Boolean> {
	public static final BooleanTypeDescriptor INSTANCE = new BooleanTypeDescriptor();

	private final char characterValueTrue;
	private final char characterValueFalse;

	private final char characterValueTrueLC;

	private final String stringValueTrue;
	private final String stringValueFalse;

	public BooleanTypeDescriptor() {
		this( 'Y', 'N' );
	}

	public BooleanTypeDescriptor(char characterValueTrue, char characterValueFalse) {
		super( Boolean.class );
		this.characterValueTrue = Character.toUpperCase( characterValueTrue );
		this.characterValueFalse = Character.toUpperCase( characterValueFalse );

		characterValueTrueLC = Character.toLowerCase( characterValueTrue );

		stringValueTrue = String.valueOf( characterValueTrue );
		stringValueFalse = String.valueOf( characterValueFalse );
	}

	public String toString(Boolean value) {
		return value == null ? null : value.toString();
	}

	public Boolean fromString(String string) {
		return Boolean.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Boolean value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Boolean.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) toByte( value );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) toShort( value );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) toInteger( value );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) toInteger( value );
		}
		if ( Character.class.isAssignableFrom( type ) ) {
			return (X) Character.valueOf( value ? characterValueTrue : characterValueFalse );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) (value ? stringValueTrue : stringValueFalse);
		}
		throw unknownUnwrap( type );
	}

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	public <X> Boolean wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Boolean.class.isInstance( value ) ) {
			return (Boolean) value;
		}
		if ( Number.class.isInstance( value ) ) {
			final int intValue = ( (Number) value ).intValue();
			return intValue == 0 ? FALSE : TRUE;
		}
		if ( Character.class.isInstance( value ) ) {
			return isTrue( ( (Character) value ).charValue() ) ? TRUE : FALSE;
		}
		if ( String.class.isInstance( value ) ) {
			return isTrue( ( (String) value ).charAt( 0 ) ) ? TRUE : FALSE;
		}
		throw unknownWrap( value.getClass() );
	}

	private boolean isTrue(char charValue) {
		return charValue == characterValueTrue || charValue == characterValueTrueLC;
	}

	public int toInt(Boolean value) {
		return value ? 1 : 0;
	}

	@SuppressWarnings({ "UnnecessaryBoxing" })
	public Byte toByte(Boolean value) {
		return Byte.valueOf( (byte) toInt( value ) );
	}

	@SuppressWarnings({ "UnnecessaryBoxing" })
	public Short toShort(Boolean value) {
		return Short.valueOf( (short ) toInt( value ) );
	}

	@SuppressWarnings({ "UnnecessaryBoxing" })
	public Integer toInteger(Boolean value) {
		return Integer.valueOf( toInt( value ) );
	}

	@SuppressWarnings({ "UnnecessaryBoxing" })
	public Long toLong(Boolean value) {
		return Long.valueOf( toInt( value ) );
	}
}

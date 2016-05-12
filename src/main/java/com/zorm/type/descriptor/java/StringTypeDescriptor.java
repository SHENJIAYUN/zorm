package com.zorm.type.descriptor.java;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;

import com.zorm.type.descriptor.WrapperOptions;

public class StringTypeDescriptor extends AbstractTypeDescriptor<String>{
	public static final StringTypeDescriptor INSTANCE = new StringTypeDescriptor();
	
	public StringTypeDescriptor() {
		super( String.class );
	}
	
	public String toString(String value) {
		return value;
	}
	
	public String fromString(String string) {
		return string;
	}
	
	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( value );
		}
		throw unknownUnwrap( type );
	}
	
	public <X> String wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return (String) value;
		}

		throw unknownWrap( value.getClass() );
	}
}

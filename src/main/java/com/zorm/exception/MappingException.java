package com.zorm.exception;

public class MappingException extends ZormException{

	private static final long serialVersionUID = 7711870687929063742L;

	public MappingException(String msg, Throwable root) {
		super( msg, root );
	}

	public MappingException(Throwable root) {
		super(root);
	}

	public MappingException(String s) {
		super(s);
	}
}

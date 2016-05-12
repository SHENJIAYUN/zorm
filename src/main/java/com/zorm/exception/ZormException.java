package com.zorm.exception;

public class ZormException extends RuntimeException{
  
	private static final long serialVersionUID = 9015102462198089206L;

	public ZormException() {}
	
	public ZormException(String message){
		super(message);
	}
	
	public ZormException(Throwable root){
		super(root);
	}
	
	public ZormException(String message,Throwable root){
		super(message,root);
	}
}

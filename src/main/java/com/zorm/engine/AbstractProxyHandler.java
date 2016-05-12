package com.zorm.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.zorm.exception.ZormException;

public abstract class AbstractProxyHandler implements InvocationHandler {
	private boolean valid = true;
	private final int hashCode;
	
	protected abstract Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable;

	public AbstractProxyHandler(int hashCode) {
		this.hashCode = hashCode;
	}

	public String toString() {
		return super.toString() + "[valid=" + valid + "]";
	}

	public final int hashCode() {
		return hashCode;
	}

	protected final boolean isValid() {
		return valid;
	}

	protected final void invalidate() {
		valid = false;
	}

	protected final void errorIfInvalid() {
		if ( !isValid() ) {
			throw new ZormException( "proxy handle is no longer valid" );
		}
	}

	
	public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();

		// basic Object methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "toString".equals( methodName ) ) {
			return this.toString();
		}
		if ( "hashCode".equals( methodName ) ) {
			return this.hashCode();
		}
		if ( "equals".equals( methodName ) ) {
			return this.equals( args[0] );
		}

		return continueInvocation( proxy, method, args );
	}
}

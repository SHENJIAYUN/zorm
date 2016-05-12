package com.zorm.event;

public abstract class ProxyVisitor extends AbstractVisitor{
	public ProxyVisitor(EventSource session) {
		super(session);
	}
}

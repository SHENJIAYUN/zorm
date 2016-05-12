package com.zorm.event;

/*
 * 刷新session事件
 */
public class FlushEvent extends AbstractEvent{
	public FlushEvent(EventSource source) {
		super(source);
	}
}

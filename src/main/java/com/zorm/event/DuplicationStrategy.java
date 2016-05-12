package com.zorm.event;

public interface DuplicationStrategy {
	public static enum Action {
		ERROR,
		KEEP_ORIGINAL,
		REPLACE_ORIGINAL
	}
	
	public boolean areMatch(Object listener, Object original);
	public Action getAction();
}

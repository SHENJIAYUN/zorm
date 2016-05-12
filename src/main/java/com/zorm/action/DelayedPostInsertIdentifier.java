package com.zorm.action;

import java.io.Serializable;

public class DelayedPostInsertIdentifier implements Serializable{
	private static long SEQUENCE = 0;
	private final long sequence;

	public DelayedPostInsertIdentifier() {
		synchronized( DelayedPostInsertIdentifier.class ) {
			if ( SEQUENCE == Long.MAX_VALUE ) {
				SEQUENCE = 0;
			}
			this.sequence = SEQUENCE++;
		}
	}
}

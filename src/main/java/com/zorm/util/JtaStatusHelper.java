package com.zorm.util;

import javax.transaction.Status;

public class JtaStatusHelper {
	public static boolean isCommitted(int status) {
		return status == Status.STATUS_COMMITTED;
	}
}

package com.zorm.engine;

import java.sql.Connection;

public interface ConnectionObserver {

	public void physicalConnectionObtained(Connection physicalConnection);

	public void physicalConnectionReleased();

	public void logicalConnectionClosed();

}

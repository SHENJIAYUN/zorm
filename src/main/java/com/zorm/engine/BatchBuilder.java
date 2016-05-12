package com.zorm.engine;

import com.zorm.jdbc.Batch;
import com.zorm.jdbc.JdbcCoordinator;
import com.zorm.service.Service;

public interface BatchBuilder extends Service,Manageable{
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator);
}

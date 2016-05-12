package com.zorm.engine;

import java.util.Map;

import com.zorm.config.Environment;
import com.zorm.jdbc.Batch;
import com.zorm.jdbc.JdbcCoordinator;
import com.zorm.service.Configurable;
import com.zorm.util.ConfigurationHelper;

public class BatchBuilderImpl implements BatchBuilder, Configurable{
	private int size;

	public BatchBuilderImpl() {
	}

	@Override
	public void configure(Map configurationValues) {
		size = ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, size );
	}

	public BatchBuilderImpl(int size) {
		this.size = size;
	}

	public void setJdbcBatchSize(int size) {
		this.size = size;
	}

	@Override
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		return size > 1
				? new BatchingBatch( key, jdbcCoordinator, size )
				: new NonBatchingBatch( key, jdbcCoordinator );
	}

	@Override
	public String getManagementDomain() {
		return null; // use Hibernate default domain
	}

	@Override
	public String getManagementServiceType() {
		return null;  // use Hibernate default scheme
	}

	@Override
	public Object getManagementBean() {
		return this;
	}

}

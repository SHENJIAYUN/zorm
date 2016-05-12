package com.zorm.query;

import com.zorm.entity.ResultTransformer;

public interface AggregatedSelectExpression extends SelectExpression{

	public ResultTransformer getResultTransformer();

}

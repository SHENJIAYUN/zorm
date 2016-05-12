package com.zorm.stat;

import com.zorm.session.SessionFactoryImplementor;

public interface StatisticsFactory {
	public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory);
}

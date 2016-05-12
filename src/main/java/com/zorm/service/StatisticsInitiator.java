package com.zorm.service;

import com.zorm.config.Configuration;
import com.zorm.exception.ZormException;
import com.zorm.meta.MetadataImplementor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionFactoryServiceInitiator;
import com.zorm.stat.ConcurrentStatisticsImpl;
import com.zorm.stat.StatisticsFactory;
import com.zorm.stat.StatisticsImplementor;

public class StatisticsInitiator implements SessionFactoryServiceInitiator<StatisticsImplementor> {

	public static final StatisticsInitiator INSTANCE = new StatisticsInitiator();

	/**
	 * Names the {@link StatisticsFactory} to use.  Recognizes both a class name as well as an instance of
	 * {@link StatisticsFactory}.
	 */
	public static final String STATS_BUILDER = "zorm.stats.factory";

	@Override
	public Class<StatisticsImplementor> getServiceInitiated() {
		return StatisticsImplementor.class;
	}

	@Override
	public StatisticsImplementor initiateService(
			SessionFactoryImplementor sessionFactory,
			Configuration configuration,
			ServiceRegistryImplementor registry) {
		final Object configValue = configuration.getProperties().get( STATS_BUILDER );
		return initiateServiceInternal( sessionFactory, configValue, registry );
	}

	@Override
	public StatisticsImplementor initiateService(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			ServiceRegistryImplementor registry) {
		ConfigurationService configurationService =  registry.getService( ConfigurationService.class );
		final Object configValue = configurationService.getSetting( STATS_BUILDER, null );
		return initiateServiceInternal( sessionFactory, configValue, registry );
	}

	private StatisticsImplementor initiateServiceInternal(
			SessionFactoryImplementor sessionFactory,
			Object configValue,
			ServiceRegistryImplementor registry) {

		StatisticsFactory statisticsFactory;
		if ( configValue == null ) {
			statisticsFactory = DEFAULT_STATS_BUILDER;
		}
		else if ( StatisticsFactory.class.isInstance( configValue ) ) {
			statisticsFactory = (StatisticsFactory) configValue;
		}
		else {
			// assume it names the factory class
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			try {
				statisticsFactory = (StatisticsFactory) classLoaderService.classForName( configValue.toString() ).newInstance();
			}
			catch (ZormException e) {
				throw e;
			}
			catch (Exception e) {
				throw new ZormException(
						"Unable to instantiate specified StatisticsFactory implementation [" + configValue.toString() + "]",
						e
				);
			}
		}

		StatisticsImplementor statistics = statisticsFactory.buildStatistics( sessionFactory );
		final boolean enabled = sessionFactory.getSettings().isStatisticsEnabled();
		statistics.setStatisticsEnabled( enabled );
		return statistics;
	}

	private static StatisticsFactory DEFAULT_STATS_BUILDER = new StatisticsFactory() {
		@Override
		public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory) {
			return new ConcurrentStatisticsImpl( sessionFactory );
		}
	};
}

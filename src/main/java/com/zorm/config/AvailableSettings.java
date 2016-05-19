package com.zorm.config;

public interface AvailableSettings {
	public static final String SESSION_FACTORY_NAME = "zorm.session_factory_name";
	
	//数据库连接驱动类
	public static final String DRIVER ="zorm.connection.driver_class";
	
	//数据库连接URL
	public static final String URL ="zorm.connection.url";

	//数据库连接用户名
	public static final String USER ="zorm.connection.username";

	//数据库连接密码
	public static final String PASS ="zorm.connection.password";

	//数据库隔离级别
	public static final String ISOLATION ="zorm.connection.isolation";

	//数据库是否自动提交
	public static final String AUTOCOMMIT ="zorm.connection.autocommit";

	//数据库连接池容量
	public static final String POOL_SIZE ="zorm.connection.pool_size";

	public static final String CONNECTION_PREFIX = "zorm.connection";

	//数据库方言
	public static final String DIALECT ="zorm.dialect";
	public static final String DIALECT_RESOLVERS = "zorm.dialect_resolvers";
	public static final String DEFAULT_SCHEMA = "zorm.default_schema";
	public static final String DEFAULT_CATALOG = "zorm.default_catalog";
	
	public static final String MAX_FETCH_DEPTH = "zorm.max_fetch_depth";
	
	public static final String DEFAULT_BATCH_FETCH_SIZE = "zorm.default_batch_fetch_size";
	
	public static final String USE_SCROLLABLE_RESULTSET = "zorm.jdbc.use_scrollable_resultset";
	
	public static final String USE_GET_GENERATED_KEYS = "zorm.jdbc.use_get_generated_keys";
	
	public static final String STATEMENT_FETCH_SIZE = "zorm.jdbc.fetch_size";
	
	public static final String STATEMENT_BATCH_SIZE = "zorm.jdbc.batch_size";
	
	public static final String BATCH_STRATEGY = "zorm.jdbc.factory_class";
	
	public static final String BATCH_VERSIONED_DATA = "zorm.jdbc.batch_versioned_data";
	
	public static final String OUTPUT_STYLESHEET ="zorm.xml.output_stylesheet";

	public static final String AUTO_CLOSE_SESSION = "zorm.transaction.auto_close_session";
	
	public static final String FLUSH_BEFORE_COMPLETION = "zorm.transaction.flush_before_completion";
	
	public static final String RELEASE_CONNECTIONS = "zorm.connection.release_mode";
	
	public static final String CURRENT_SESSION_CONTEXT_CLASS = "zorm.current_session_context_class";

	public static final String TRANSACTION_STRATEGY = "zorm.transaction.factory_class";

	public static final String JTA_PLATFORM = "zorm.transaction.jta.platform";

	public static final String TRANSACTION_MANAGER_STRATEGY = "zorm.transaction.manager_lookup_class";

	public static final String USER_TRANSACTION = "jta.UserTransaction";

	public static final String USE_QUERY_CACHE = "zorm.cache.use_query_cache";

	public static final String QUERY_CACHE_FACTORY = "zorm.cache.query_cache_factory";

	public static final String USE_SECOND_LEVEL_CACHE = "zorm.cache.use_second_level_cache";

	public static final String USE_MINIMAL_PUTS = "zorm.cache.use_minimal_puts";
	
	public static final String CACHE_REGION_PREFIX = "zorm.cache.region_prefix";

	public static final String USE_STRUCTURED_CACHE = "zorm.cache.use_structured_entries";

	public static final String GENERATE_STATISTICS = "zorm.generate_statistics";

	public static final String USE_IDENTIFIER_ROLLBACK = "zorm.use_identifier_rollback";

	public static final String USE_REFLECTION_OPTIMIZER = "zorm.bytecode.use_reflection_optimizer";

	public static final String QUERY_TRANSLATOR = "zorm.query.factory_class";

	public static final String QUERY_SUBSTITUTIONS = "zorm.query.substitutions";

	public static final String QUERY_STARTUP_CHECKING = "zorm.query.startup_check";


	public static final String SQL_EXCEPTION_CONVERTER = "zorm.jdbc.sql_exception_converter";

	public static final String WRAP_RESULT_SETS = "zorm.jdbc.wrap_result_sets";

	public static final String ORDER_UPDATES = "zorm.order_updates";

	public static final String ORDER_INSERTS = "zorm.order_inserts";

    public static final String DEFAULT_ENTITY_MODE = "zorm.default_entity_mode";

    public static final String JACC_CONTEXTID = "zorm.jacc_context_id";

	public static final String GLOBALLY_QUOTED_IDENTIFIERS = "zorm.globally_quoted_identifiers";

	public static final String CHECK_NULLABILITY = "zorm.check_nullability";


	public static final String BYTECODE_PROVIDER = "zorm.bytecode.provider";

	public static final String JPAQL_STRICT_COMPLIANCE= "zorm.query.jpaql_strict_compliance";

	public static final String PREFER_POOLED_VALUES_LO = "zorm.id.optimizer.pooled.prefer_lo";

	public static final String QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES = "zorm.query.plan_cache_max_strong_references";

	public static final String QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES = "zorm.query.plan_cache_max_soft_references";

	public static final String QUERY_PLAN_CACHE_MAX_SIZE = "zorm.query.plan_cache_max_size";

	public static final String QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE = "zorm.query.plan_parameter_metadata_max_size";

	public static final String NON_CONTEXTUAL_LOB_CREATION = "zorm.jdbc.lob.non_contextual_creation";

	public static final String APP_CLASSLOADER = "zorm.classLoader.application";

	public static final String RESOURCES_CLASSLOADER = "zorm.classLoader.resources";

	public static final String ZORM_CLASSLOADER = "zorm.classLoader.zorm";

	public static final String ENVIRONMENT_CLASSLOADER = "zorm.classLoader.environment";


	public static final String C3P0_CONFIG_PREFIX = "zorm.c3p0";

	public static final String PROXOOL_CONFIG_PREFIX = "zorm.proxool";


	public static final String JMX_DEFAULT_OBJ_NAME_DOMAIN = "org.zorm.core";

	public static final String JTA_CACHE_TM = "zorm.jta.cacheTransactionManager";

	public static final String JTA_CACHE_UT = "zorm.jta.cacheUserTransaction";

	public static final String DEFAULT_CACHE_CONCURRENCY_STRATEGY = "zorm.cache.default_cache_concurrency_strategy";

	public static final String USE_NEW_ID_GENERATOR_MAPPINGS = "zorm.id.new_generator_mappings";

	public static final String CUSTOM_ENTITY_DIRTINESS_STRATEGY = "zorm.entity_dirtiness_strategy";

	public static final String MULTI_TENANT = "zorm.multiTenancy";

	public static final String MULTI_TENANT_CONNECTION_PROVIDER = "zorm.multi_tenant_connection_provider";

	public static final String MULTI_TENANT_IDENTIFIER_RESOLVER = "zorm.tenant_identifier_resolver";

	public static final String FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT = "zorm.discriminator.force_in_select";

    public static final String ENABLE_LAZY_LOAD_NO_TRANS = "zorm.enable_lazy_load_no_trans";

	public static final String HQL_BULK_ID_STRATEGY = "zorm.hql.bulk_id_strategy";
}


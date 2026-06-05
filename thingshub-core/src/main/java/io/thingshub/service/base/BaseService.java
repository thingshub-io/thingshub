package io.thingshub.service.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cache.store.jdbc.JdbcTypeField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;

import cn.hutool.db.sql.Condition;
import cn.hutool.db.sql.SqlBuilder;
import io.thingshub.commons.Page;
import io.thingshub.commons.SysException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Base Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class BaseService<K, E> {

	public static final DataRegion DEFAULT_DATA_REGION = new DataRegion() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return DataRegion.class;
		}

		@Override
		public String name() {
			return DataStorageConfiguration.DFLT_DATA_REG_DEFAULT_NAME;
		}

		@Override
		public long initSize() {
			return DataStorageConfiguration.DFLT_DATA_REGION_INITIAL_SIZE;
		}

		@Override
		public long maxSize() {
			return DataStorageConfiguration.DFLT_DATA_REGION_MAX_SIZE;
		}

		@Override
		public boolean persistent() {
			return true;
		}

		@Override
		public boolean local() {
			return false;
		}

	};

	public static enum DeletedStatus {
		NOT_DELETED(0), DELETED(1);

		@Accessors(fluent = true)
		@Getter
		private int value;

		DeletedStatus(int value) {
			this.value = value;
		}

	};

	public static enum AvailableStatus {
		NORMAL(0), DISABLED(1);

		@Accessors(fluent = true)
		@Getter
		private int value;

		AvailableStatus(int value) {
			this.value = value;
		}

	};

	private final Map<String, Field> colFieldMapping = new HashMap<>();

	private final List<String> cols = new ArrayList<>();

	private final List<JdbcTypeField> jdbcFields = new ArrayList<>();

	@Inject
	private Ignite ignite;

	protected IgniteCache<K, E> entityCache;

	private Class<K> kClazz;

	private Class<E> eClazz;

	private String cacheName;

	public String SELECT_BY;

	public String SELECT_COUNT;

	public String DELETE_BY;

	@SuppressWarnings("unchecked")
	protected BaseService() {
		Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
		this.kClazz = (Class<K>) types[0];
		this.eClazz = (Class<E>) types[1];
		this.cacheName = this.eClazz.getSimpleName();

		Field[] _fields = this.eClazz.getDeclaredFields();
		for (Field field : _fields) {
			if (field.isAnnotationPresent(QuerySqlField.class)) {
				QuerySqlField theAnnotation = field.getAnnotation(QuerySqlField.class);
				String colName = theAnnotation.name().isBlank() ? field.getName() : theAnnotation.name();
				this.colFieldMapping.put(colName, field);
				this.cols.add(colName);

				if (field.getType() == Long.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.BIGINT, colName, Long.class, field.getName()));
				} else if (field.getType() == String.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.VARCHAR, colName, String.class, field.getName()));
				} else if (field.getType() == Date.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.TIMESTAMP, colName, Date.class, field.getName()));
				} else if (field.getType() == BigDecimal.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.DECIMAL, colName, BigDecimal.class, field.getName()));
				} else if (field.getType() == Byte.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.TINYINT, colName, Byte.class, field.getName()));
				} else if (field.getType() == Short.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.SMALLINT, colName, Short.class, field.getName()));
				} else if (field.getType() == Integer.class) {
					jdbcFields.add(new JdbcTypeField(java.sql.Types.INTEGER, colName, Integer.class, field.getName()));
				}
			}
		}

		this.SELECT_COUNT = "SELECT count(*) FROM " + this.cacheName + " WHERE ";
		this.SELECT_BY = "SELECT " + String.join(",", this.cols) + " FROM " + this.cacheName;
		this.DELETE_BY = "DELETE FROM " + this.cacheName + " WHERE ";
	}

	@PostConstruct
	public void initIgniteCache() {
		DataRegion dataRegion = this.eClazz.getAnnotation(DataRegion.class);
		if (dataRegion == null) {
			dataRegion = DEFAULT_DATA_REGION;
		}

		CacheConfiguration<K, E> cacheConfiguration = new CacheConfiguration<K, E>() //
				.setSqlSchema("THINGSHUB") //
				.setName(cacheName) //
				.setCacheMode(dataRegion.local() ? CacheMode.REPLICATED : CacheMode.PARTITIONED) //
				.setDataRegionName(dataRegion.name()) //
				.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL) //
				.setWriteSynchronizationMode(getWriteSyncModel()) //
				.setBackups(getBackups()) //
//				.setGroupName("meta_data_group")//节点数量较多时使用
				.setEagerTtl(true) //
				.setPartitionLossPolicy(PartitionLossPolicy.READ_ONLY_SAFE) //
				.setIndexedTypes(new Class[] { this.kClazz, this.eClazz }) //
				.setRebalanceMode(CacheRebalanceMode.ASYNC);

		this.entityCache = this.ignite.getOrCreateCache(cacheConfiguration);
	}

	protected CacheWriteSynchronizationMode getWriteSyncModel() {
		return CacheWriteSynchronizationMode.PRIMARY_SYNC;
	}

	protected int getBackups() {
		return 1;
	}

	public void listen(BiConsumer<EventType, E> callback) {
		ContinuousQuery<K, E> cqry = new ContinuousQuery<>();
		cqry.setIncludeExpired(true);
		cqry.setLocalListener(new CacheEntryUpdatedListener<K, E>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends E>> events) throws CacheEntryListenerException {
				for (CacheEntryEvent<? extends K, ? extends E> e : events) {
					callback.accept(e.getEventType(), e.getValue());
				}
			}

		});

		this.entityCache.query(cqry);
	}

	public void listenInLocal(BiConsumer<EventType, E> callback) {
		ContinuousQuery<K, E> cqry = new ContinuousQuery<>();
		cqry.setIncludeExpired(true);
		cqry.setLocal(true);
		cqry.setLocalListener(new CacheEntryUpdatedListener<K, E>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends E>> events) throws CacheEntryListenerException {
				for (CacheEntryEvent<? extends K, ? extends E> e : events) {
					callback.accept(e.getEventType(), e.getValue());
				}
			}

		});

		this.entityCache.query(cqry);
	}

	public E getAndRemove(K key) {
		return this.entityCache.getAndRemove(key);
	}

	public void save(K key, E entity) {
		try {
			this.entityCache.put(key, entity);
		} catch (Exception e) {
			throw new SysException("system error", e);
		}
	}

	public void save(K key, E entity, long duration, TimeUnit unit) {
		try {
			ExpiryPolicy expiryPolicy = null;

			if (!this.entityCache.containsKey(key)) {
				expiryPolicy = new CreatedExpiryPolicy(new Duration(unit, duration));
			} else {
				expiryPolicy = new ModifiedExpiryPolicy(new Duration(unit, duration));
			}

			this.entityCache.withExpiryPolicy(expiryPolicy).put(key, entity);
		} catch (Exception e) {
			throw new SysException("system error", e);
		}
	}

	public boolean saveIfAbsent(K key, E entity, long duration, TimeUnit unit) {
		try {
			return this.entityCache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(unit, duration))).putIfAbsent(key, entity);
		} catch (Exception e) {
			throw new SysException("system error", e);
		}
	}

	public void saveBatch(Map<K, E> entities) {
		try {
			for (Entry<K, E> entry : entities.entrySet()) {
				this.entityCache.put(entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
			throw new SysException("system error", e);
		}
	}

	public boolean exists(K key) {
		return this.entityCache.containsKey(key);
	}

	public E getByKey(K key) {
		return this.entityCache.get(key);
	}

	public E getOne(List<Condition> conditions) {
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName)
				.where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.build()).setArgs(selectSqlBuilder.getParamValueArray());

		E entity = null;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			int i = 0;
			for (List<?> row : cursor) {
				if (i > 1) {
					throw new SysException("One record is expected, but query result has multiple records");
				}

				entity = buildEntity(row);
				i++;
			}
		}

		return entity;
	}

	public E getOneInLocal(List<Condition> conditions) {
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName)
				.where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.build()).setArgs(selectSqlBuilder.getParamValueArray()).setLocal(true);

		E entity = null;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			int i = 0;
			for (List<?> row : cursor) {
				if (i > 1) {
					throw new SysException("One record is expected, but query result has multiple records");
				}

				entity = buildEntity(row);
				i++;
			}
		}

		return entity;
	}

	private E buildEntity(List<?> row) {
		try {
			E entity = eClazz.getDeclaredConstructor().newInstance();

			for (int i = 0; i < this.cols.size(); i++) {
				Field field = this.colFieldMapping.get(this.cols.get(i));

				field.setAccessible(true);
				field.set(entity, row.get(i));
			}

			return entity;
		} catch (Exception e) {
			throw new SysException("system error", e);
		}
	}

	public List<E> list() {
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName);
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.build());

		List<E> entities = Collections.emptyList();
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			entities = new ArrayList<>();
			for (List<?> row : cursor) {
				E entity = buildEntity(row);

				entities.add(entity);
			}
		}

		return entities;
	}

	public List<E> listInLocal() {
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName);
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.build()).setLocal(true);

		List<E> entities = Collections.emptyList();
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			entities = new ArrayList<>();
			for (List<?> row : cursor) {
				E entity = buildEntity(row);

				entities.add(entity);
			}
		}

		return entities;
	}

	public List<E> query(List<Condition> conditions) {
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName)
				.where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.build()).setArgs(selectSqlBuilder.getParamValueArray());

		List<E> entities = Collections.emptyList();
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			entities = new ArrayList<>();
			for (List<?> row : cursor) {
				E entity = buildEntity(row);

				entities.add(entity);
			}
		}

		return entities;
	}

	public List<E> queryInLocal(List<Condition> conditions) {
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName)
				.where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.build()).setArgs(selectSqlBuilder.getParamValueArray()).setLocal(true);

		List<E> entities = Collections.emptyList();
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			entities = new ArrayList<>();
			for (List<?> row : cursor) {
				E entity = buildEntity(row);

				entities.add(entity);
			}
		}

		return entities;
	}

//	public static void main(String[] args) {
//		Condition equalCond = new Condition("sn", "c1");
//		Condition grpCond = new Condition("group", null);
//		Condition neCond = new Condition("status", "!=", 1);
//		Condition likeCond = new Condition("title", "test", LikeType.StartWith);
//		Condition inCond = new Condition("type", "in", new int[] { 1, 2 });
//		Condition geCond = new Condition("create_time", ">=", DateUtil.beginOfDay(DateUtil.date()));
//		Condition leCond = new Condition("create_time", "<=", DateUtil.endOfDay(DateUtil.date()));
//		Condition btwCond = new Condition("lng", "BETWEEN", new double[] { 96.09, 119.80 });
//		List<Condition> conds = new ArrayList<>();
//		conds.add(equalCond);
//		conds.add(grpCond);
//		conds.add(neCond);
//		conds.add(likeCond);
//		conds.add(inCond);
//		conds.add(geCond);
//		conds.add(leCond);
//		conds.add(btwCond);
//
//		String sql = SqlBuilder.create().select().from("device").where(conds.stream().toArray(Condition[]::new)).groupBy("type").build();
//
//		System.out.println("sql=========================" + sql);

//		Device device = new Device();
//		device.setActiveState(1);
//		device.setActivateTime(DateUtil.date());
//		device.setCreateBy("admin");
//
//		List<Condition> conditions = Lists.newArrayList(new Condition("sn", "123"));
//
//		Entity dataEntity = Entity.create().setTableName("device");
//		Field[] _fields = Device.class.getDeclaredFields();
//		for (int i = 0; i < _fields.length; i++) {
//			Field field = _fields[i];
//
//			field.setAccessible(true);
//			try {
//				if (field.get(device) != null)
//					dataEntity.set(field.getName(), field.get(device));
//			} catch (IllegalArgumentException | IllegalAccessException e) {
//				throw new ServcieException(e);
//			}
//		}
//
//		SqlBuilder updateSqlBuilder = SqlBuilder.create().update(dataEntity).where(conditions.toArray(new Condition[0]));
//
//		System.out.println("update sql==================" + updateSqlBuilder.build());
//	}

	public int count(List<Condition> conditions) {
		SqlBuilder countSqlBuilder = SqlBuilder.create().select("count(*)").from(cacheName).where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery countQuery = new SqlFieldsQuery(countSqlBuilder.build()).setArgs(countSqlBuilder.getParamValueArray());

		int total = 0;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(countQuery)) {
			for (List<?> row : cursor) {
				if (row.get(0) != null) {
					Long sum = (Long) row.get(0);
					total = sum.intValue();
				}
			}
		}

		return total;
	}

	public Page<E> query(List<Condition> conditions, Long startId, int size) {
		SqlBuilder countSqlBuilder = SqlBuilder.create().select("count(*)").from(cacheName).where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery countQuery = new SqlFieldsQuery(countSqlBuilder.build()).setArgs(countSqlBuilder.getParamValueArray());

		int total = 0;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(countQuery)) {
			for (List<?> row : cursor) {
				if (row.get(0) != null) {
					Long sum = (Long) row.get(0);
					total = sum.intValue();
				}
			}
		}

//		conditions.add(new Condition(this.keyCol, ">", startId));
		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName)
				.where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.append(" LIMIT ").append(size).toString())
				.setArgs(selectSqlBuilder.getParamValueArray());

		List<E> entities = Collections.emptyList();
		int curPageSize = 0;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			entities = new ArrayList<>();
			for (List<?> row : cursor) {
				E entity = buildEntity(row);

				entities.add(entity);
				curPageSize++;
			}
		}

		return Page.of(-1, curPageSize, total, entities);// TODO current page????
	}

	public Page<E> query(List<Condition> conditions, int page, int size) {
		SqlBuilder countSqlBuilder = SqlBuilder.create().select("count(*)").from(cacheName).where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery countQuery = new SqlFieldsQuery(countSqlBuilder.build()).setArgs(countSqlBuilder.getParamValueArray());

		int total = 0;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(countQuery)) {
			for (List<?> row : cursor) {
				if (row.get(0) != null) {
					Long sum = (Long) row.get(0);
					total = sum.intValue();
				}
			}
		}

		SqlBuilder selectSqlBuilder = SqlBuilder.create().select(this.cols.stream().toArray(String[]::new)).from(cacheName)
				.where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery selectQuery = new SqlFieldsQuery(selectSqlBuilder.append(" LIMIT ").append(size).append(" OFFSET ").append((page - 1) * size).build())
				.setArgs(selectSqlBuilder.getParamValueArray());

		List<E> entities = Collections.emptyList();
		int curPageSize = 0;
		try (QueryCursor<List<?>> cursor = this.entityCache.query(selectQuery)) {
			entities = new ArrayList<>();
			for (List<?> row : cursor) {
				E entity = buildEntity(row);

				entities.add(entity);
				curPageSize++;
			}
		}

		return Page.of(page, curPageSize, total, entities);
	}

	public void removeByKey(K key) {
		this.entityCache.remove(key);
	}

	public void remove(List<Condition> conditions) {
		SqlBuilder deleteSqlBuilder = SqlBuilder.create().delete(cacheName).where(conditions.stream().toArray(Condition[]::new));
		SqlFieldsQuery deleteQuery = new SqlFieldsQuery(deleteSqlBuilder.build()).setArgs(deleteSqlBuilder.getParamValueArray());

		this.entityCache.query(deleteQuery).getAll();
	}

}

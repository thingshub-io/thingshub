package io.thingshub;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteEvents;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.net.NetUtil;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.SysException;
import io.thingshub.config.BrokerConfig;
import io.thingshub.ioc.ApplicationRunner;
import io.thingshub.ioc.Config;
import io.thingshub.logging.LogIgniteService;
import io.thingshub.service.TransportServerService;
import io.thingshub.service.base.DataRegion;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.transport.Server;
import io.thingshub.transport.utils.TaskTracker;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Thingshub Broker
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Builder
@Slf4j
public class Broker {

	private static final List<Consumer<String>> nodeFailureHandlers = new ArrayList<>();

	public static String BROKER_ADDR = NetUtil.getLocalhost().getHostAddress();

	private static String consistentId;

	private static Injector injector;

	public static <T> T getBean(Class<T> clazz) {
		return injector.getInstance(clazz);
	}

	public static String getConsistentId() {
		return consistentId;
	}

	public static void registerNodeFailureHandler(Consumer<String> handler) {
		nodeFailureHandlers.add(handler);
	}

	private Map<String, Object> configs;

	@SuppressWarnings("rawtypes")
	public Mono<Broker> startup() throws Exception {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.getLogger("org.reflections").setLevel(Level.ERROR);

		System.getProperties().put("java.net.preferIPv4Stack", "true"); // using IPv4
		System.getProperties().put("IGNITE_NO_ASCII", "true");
		System.getProperties().put("IGNITE_QUIET", "false");
		System.getProperties().put("IGNITE_TO_STRING_INCLUDE_SENSITIVE", "false");
		System.getProperties().put("IGNITE_UPDATE_NOTIFIER", "false");

		Configuration reflectionsCfg = new ConfigurationBuilder().forPackages(Broker.class.getPackageName()).setParallel(true);
		Reflections reflections = new Reflections(reflectionsCfg);
		Set<Class<?>> clazzsWithConfig = reflections.get(Scanners.TypesAnnotated.with(Config.class).asClass());
		Map<Class, Object> configClazzBeanMap = new HashMap<>();
		if (clazzsWithConfig != null) {
			try {
				for (Class<?> configClazz : clazzsWithConfig) {
					Object configBean = configClazz.getDeclaredConstructor().newInstance();
					ConfigHelper.injectValue(configs, configBean);
					configClazzBeanMap.put(configClazz, configBean);
				}
			} catch (Exception e) {
				log.error("Failed to inject config value to bean. Error: ", e);
				throw new SysException("", e);
			}
		}

		BrokerConfig brokerConfig = (BrokerConfig) configClazzBeanMap.get(BrokerConfig.class);
		brokerConfig.getLoggers().forEach((logger, level) -> {
			loggerContext.getLogger(logger).setLevel(Level.toLevel(level));
		});

		/**
		 * Configure data storage
		 */
		DataStorageConfiguration storageConfiguration = new DataStorageConfiguration();
		storageConfiguration.setWalSegmentSize(128 * 1024 * 1024);
		storageConfiguration.setWriteThrottlingEnabled(true);
		storageConfiguration.setDefaultWarmUpConfiguration(null);// warm up strategy for all data region

		DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
		defaultRegion.setName(DataStorageConfiguration.DFLT_DATA_REG_DEFAULT_NAME);
		defaultRegion.setInitialSize(DataStorageConfiguration.DFLT_DATA_REGION_INITIAL_SIZE);
		defaultRegion.setMaxSize(DataStorageConfiguration.DFLT_DATA_REGION_MAX_SIZE);// 20% of total mem
		defaultRegion.setPersistenceEnabled(true);
		defaultRegion.setWarmUpConfiguration(null);// warm up strategy
		storageConfiguration.setDefaultDataRegionConfiguration(defaultRegion);
		storageConfiguration.getDefaultDataRegionConfiguration().setCheckpointPageBufferSize(1024L * 1024 * 1024);

		Set<Class<?>> clazzsWithCustomDataRegion = reflections.get(Scanners.TypesAnnotated.with(DataRegion.class).asClass());
		if (clazzsWithCustomDataRegion != null) {
			Map<String, DataRegion> customeDataRegions = Maps.newHashMap();
			clazzsWithCustomDataRegion.stream().map(c -> c.getAnnotation(DataRegion.class)).forEach(region -> customeDataRegions.putIfAbsent(region.name(), region));
			DataRegionConfiguration[] customDataRegionConfigurations = new DataRegionConfiguration[customeDataRegions.size()];
			int i = 0;
			for (Map.Entry<String, DataRegion> dataRegionEntry : customeDataRegions.entrySet()) {
				DataRegion dataRegion = dataRegionEntry.getValue();
				customDataRegionConfigurations[i] = new DataRegionConfiguration().setName(dataRegion.name()).setInitialSize(dataRegion.initSize()).setMaxSize(dataRegion.maxSize())
						.setPersistenceEnabled(dataRegion.persistent());

				i++;
			}

			storageConfiguration.setDataRegionConfigurations(customDataRegionConfigurations);
		}

		String home = System.getProperty("thingshub.home");
		int nodeId = (int) Optional.ofNullable(configs.get("thingshub.node-id")).orElse(1);
		String instanceName = "thingshub_instance_" + nodeId;
		consistentId = "thingshub_node_" + nodeId;

		String bindAddress = Optional.ofNullable(brokerConfig.getBind()).orElse(BROKER_ADDR);
		Integer clientPort = Optional.ofNullable(brokerConfig.getPort()).orElse(10800);
		String workDir = Optional.ofNullable(brokerConfig.getWorkDir()).filter(dir -> !dir.trim().isEmpty()).orElse(home.concat(File.separator).concat("work"));

		Map<String, Object> userAttributes = new HashMap<>();
		userAttributes.put("thingshub", "thingshub");
		if (brokerConfig.getNodeAttributes() != null) {
			userAttributes.putAll(brokerConfig.getNodeAttributes().stream().collect(Collectors.toMap(String::toString, String::toString)));
		}

		IgniteConfiguration igniteCfg = new IgniteConfiguration();
		igniteCfg.setDataStorageConfiguration(storageConfiguration);
		igniteCfg.setGridLogger(new Slf4jLogger());
		igniteCfg.setClientMode(false);
		igniteCfg.setMetricsLogFrequency(0L);
		igniteCfg.setIgniteHome(home);
		igniteCfg.setWorkDirectory(Paths.get(workDir).toAbsolutePath().toString());
		igniteCfg.setIgniteInstanceName(instanceName);
		igniteCfg.setUserAttributes(userAttributes);
		igniteCfg.setConsistentId(consistentId);
		igniteCfg.setLocalHost(bindAddress);
		igniteCfg.setConnectorConfiguration(new ConnectorConfiguration().setHost(bindAddress));
		igniteCfg.setIncludeEventTypes(EventType.EVT_NODE_FAILED);

		/**
		 * Configure discovery SPI implementation
		 */
		TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
		discoverySpi.setLocalPort(brokerConfig.getDiscPort());
		discoverySpi.setLocalPortRange(10);
		if (brokerConfig.getAddresses() != null) {// static IP finder
			TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
			ipFinder.setAddresses(brokerConfig.getAddresses());
			discoverySpi.setIpFinder(ipFinder);
		}
		igniteCfg.setDiscoverySpi(discoverySpi);

		TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
		commSpi.setLocalPort(brokerConfig.getCommPort());
		commSpi.setLocalPortRange(10);
		igniteCfg.setCommunicationSpi(commSpi);

		ClientConnectorConfiguration clientConnectorCfg = new ClientConnectorConfiguration();
		clientConnectorCfg.setHost(bindAddress);
		clientConnectorCfg.setThinClientEnabled(true);
		clientConnectorCfg.setPort(clientPort);
		clientConnectorCfg.setPortRange(10);
		igniteCfg.setClientConnectorConfiguration(clientConnectorCfg);

		Ignite ignite = Ignition.start(igniteCfg);
		if (!ignite.cluster().state().active()) {
			ignite.cluster().state(ClusterState.ACTIVE);
		}

		/**
		 * Assemble IOC dependency
		 */
		final ImmutableList.Builder<AbstractModule> modules = ImmutableList.builder();
		modules.add(new AbstractModule() {

			@SuppressWarnings("unchecked")
			@Override
			protected void configure() {
				binder().requireExplicitBindings();
				bind(IdGenerator.class).toInstance(new IdGenerator(1L, Long.valueOf(nodeId + "")));// TODO
				bind(Ignite.class).toInstance(ignite);
				bind(IgniteMessaging.class).toInstance(ignite.message());
				configClazzBeanMap.forEach((clazz, config) -> bind(clazz).toInstance(config));
			}

		});

		Set<Class<? extends AbstractModule>> moduleClazzs = reflections.getSubTypesOf(AbstractModule.class);
		if (moduleClazzs != null) {
			moduleClazzs.forEach(moduleClazz -> {
				try {
					if (!moduleClazz.toString().contains("$")) {
						modules.add(moduleClazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					log.warn("Building guice module failed. Error: ", e);
				}
			});
		}

		injector = Guice.createInjector(Stage.PRODUCTION, modules.build());

		/**
		 * Deploy services
		 */
		ClusterGroup clusterGroup = ignite.cluster().forAttribute("thingshub", "thingshub");
		ignite.services(clusterGroup).deployNodeSingleton("logService", new LogIgniteService());

		/**
		 * Listen node failure event
		 */
		IgniteEvents events = ignite.events();
		events.remoteListen(new IgniteBiPredicate<UUID, DiscoveryEvent>() {

			private static final long serialVersionUID = -8633770795475075609L;

			@Override
			public boolean apply(UUID uuid, DiscoveryEvent e) {

				nodeFailureHandlers.forEach(handler -> handler.accept(e.node().consistentId().toString()));

				return true; // continue listening
			}

		}, evt -> true, EventType.EVT_NODE_FAILED);

		/**
		 * Complete initialization and start transport server
		 */
		List<Mono<Server>> serverMonos = injector.findBindingsByType(TypeLiteral.get(Server.class)).stream().map(binding -> {
			return injector.getInstance(binding.getKey()).start().doOnSuccess(s -> {
				try {
					injector.getInstance(TransportServerService.class).saveTransportServer(s.name(), s.transport().name());
				} catch (ServiceException se) {
					log.error("", se);
				}
			});
		}).toList();

		return Mono.when(serverMonos).doOnSuccess(v -> {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				TaskTracker.getInstance().stop();

				ignite.close();
			}));
		}).then(Mono.from(v -> {
			Set<Class<? extends ApplicationRunner>> runnerClazzs = reflections.getSubTypesOf(ApplicationRunner.class);
			if (runnerClazzs != null) {
				runnerClazzs.forEach(runnerClazz -> {
					try {
						injector.getInstance(runnerClazz).run();
					} catch (Exception e) {
						log.error("", e);
					}
				});
			}

			log.info("Thingshub started successfully");
		})).thenReturn(this);
	}

}

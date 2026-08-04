# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Full build (skip tests)
mvn clean package -DskipTests

# Build with GraalVM JS engine
mvn clean package -P build-with-graalvm -DskipTests

# Run from source
mvn exec:java -pl thingshub-starter -Dexec.mainClass="io.thingshub.Starter"

# Frontend dev server (port 3031, proxies API to localhost:18080)
cd thingshub-dashboard/thingshub-dashboard-ui
npm install
npm start
npm run mock       # Mock data server
```

Build outputs (in `build/assembly/target/`): fat JAR, Linux `.tar.gz`, Windows `.zip`.

## Architecture Overview

ThingsHub is an **IoT message broker platform** — devices connect via multiple protocols, the broker routes messages, and data exports to downstream systems (Kafka, TDengine, ClickHouse, etc.).

**Startup flow** (`Starter.main` → `Broker.startup()`):
1. Load `config.yaml`, flatten keys (kebab→camelCase via `YmlCamelCase`), set as system properties
2. Scan all `@Config`-annotated classes, inject `@Value` fields from config
3. Start Apache Ignite node (cluster discovery, data grid, messaging)
4. Build Guice injector with `requireExplicitBindings()` — scan for `AbstractModule` subclasses and auto-instantiate them
5. Deploy Ignite node-singleton services (e.g., `LogIgniteService`)
6. Start all `Server` implementations (MQTT, TCP, HTTP, GB28181, Dashboard)
7. Run `ApplicationRunner` beans

## Custom IoC (Guice + Custom Annotations)

The DI system is **Guice under the hood** with custom extensions in `thingshub-core/.../ioc/`:

- `@Config` — marks config POJOs; Broker scans these and injects `@Value("${...}")` from config.yaml
- `@Component` / `@Service` — semantic markers (not scanned by IoC directly; beans are bound explicitly in Guice modules)
- `@Controller` — meta-annotated `@Component`, used by the dashboard HTTP framework
- `GuiceExtentionModule` registers type listeners:
  - `Destroyable` beans → auto-registered with `DestroyableManager` for shutdown cleanup
  - `@PostConstruct` methods → called after injection
  - `@PreDestroy` methods → called on shutdown via `DestroyableManager`
- `Broker.getBean(Class)` provides static access to the injector

**Bean discovery pattern**: The `Broker` scans for `AbstractModule` subclasses via Reflections. Transport modules and dashboard modules each have their own Guice module that explicitly binds their `@Component`/`@Service` classes. This means adding a new `@Service` also requires adding it to a module's `configure()` method.

## Config System

`config.yaml` is flattened to dot-separated keys (e.g., `thingshub.transport.mqtt.port`). `@Config` classes use `@Value("${thingshub.transport.mqtt.port:1883}")` with `:defaultValue` fallback. For `Map` fields, all entries under a prefix are collected (used for log level overrides in `BrokerConfig.loggers`).

## Transport Layer

All protocol implementations live under `thingshub-transport/` and implement `io.thingshub.transport.Server`:

- `Server.bind(TransportConfig)` — default implementation creates a Reactor Netty `TcpServer` with SSL, rate limiting (Guava `RateLimiter`), memory pressure gating (`DirectMemPressureCondition` OR `HeapMemPressureCondition`), and traffic shaping
- `MessageRouter` — type-safe dispatch: scans `Processor<Ctx, Packet>` generics, builds a packet-type → processor map
- `MessageDistributor` — broadcasts `Publication` objects cluster-wide via `IgniteMessaging`
- `PublicationListener` — receives cluster publications, matches subscribers, load-balances across online subscribers, retries for offline ones. Uses Ignite reentrant locks for delivery dedup
- `DeliveryProcessor` — per-client pipelined queue: `Sinks.Many` unicast sink + 30ms `Flux.interval` pull loop with backpressure-aware subscriber
- `ConnectionManager` / `SessionManager` — react to Ignite `ContinuousQuery` events for connection/session lifecycle

**Throttling pipeline** (in `transport/throttler/`): `ConnectionLimitHandler` → `SlowdownInboundHandler` → `MessageDebounceHandler`, gated by `HeapMemPressureCondition` and `DirectMemPressureCondition`.

## Service Layer & Data (Ignite as Database)

All services extend `BaseService<K, E>` which provides an ORM-like abstraction directly on **Ignite partitioned caches**:

- Cache config: `PARTITIONED`, `TRANSACTIONAL` atomicity, 1 backup, `PRIMARY_SYNC` writes
- SQL queries via `SqlFieldsQuery` — `BaseService` introspects `@QuerySqlField` annotations to build column lists and type mappings
- `query(Condition, page, size)` — COUNT then SELECT with LIMIT/OFFSET
- `*InLocal()` variants set `setLocal(true)` for node-local queries
- `listen(Callback)` — `ContinuousQuery` listeners for CDC-style event handling
- **Soft delete**: `deletedStatus` field (0/1) filtered in every query; `remove()` sets it to 1
- **Snowflake IDs**: `IdGenerator.nextId()` across all services
- Entities are the domain model AND the persistence model — no separate DTO/ORM layer

## Dashboard HTTP Framework

The dashboard server (`thingshub-dashboard-server`) uses a **custom micro-framework** (not Spring MVC):

- `DashboardServer` implements `Server`, uses Reactor Netty `HttpServer` directly
- `@Controller` + `@RequestMapping(method, path)` annotations on handler methods
- **Javassist** reads method parameter names at startup (Java reflection gives `arg0`/`arg1`)
- Parameter binding: `@RequestBody` (JSON deserialized), `@RequestParam`, `@Validated` (Bean Validation)
- `InterceptorChain`: `UserInterceptor` extracts user from `X-User-Id` header (currently hardcoded to user "1"), caches `UserInfo` in Caffeine (24h TTL)
- `@HasAuthority(value)` + AspectJ `CheckAuthorityAspect` for method-level authorization
- Static files served from classpath `/static/` (the Vue build output)
- SPA fallback: any non-file-extension path returns `/static/index.html`

## Script Engine

Protocol adapters use runtime-loaded scripts referenced by `ProtoAdaptor.scriptId`:

- `ScriptEngineFactory` resolves engine by language string
- `GraalJsEngine`: persistent GraalVM `Engine`, compiled scripts cached in `ConcurrentHashMap`. Scripts are wrapped in a constructor closure providing `hexToBytes`/`bytesToHex` helpers and an `invoke(func, args)` dispatcher. Each invocation creates a fresh `Context` with `allowAllAccess(true)`
- `PythonEngine` (Jython): partially implemented, currently disabled
- `ScriptService` manages `ScriptInfo` entities in Ignite; `TransportServerService.setPrehandler()` binds compiled scripts to transport servers

## Cluster (Apache Ignite)

Ignite is used as: data grid, service grid, message bus, lock manager, and compute grid. Key points:

- Discovery: TCP static IP list or multicast; ports 47500 (discovery), 47100 (communication)
- Persistence: Native persistence with WAL + checkpointing; data regions configurable via `@DataRegion` annotation
- `ClusterManager`: wraps `IgniteCluster` for node info queries and task execution (`CallableJob`, `ClosureJob`, `RunnableTask`)
- Node failure handling: `EVT_NODE_FAILED` listeners trigger `SessionManager.handleSessionStateAfterNodeFailure()`

## Retry Mechanism (Time Wheel)

`thingshub-core/.../retry0/` — a two-level hierarchical time wheel for delayed task execution:
- Lower wheel: 100 slots × 10ms = 1s interval
- Overflow wheel: 5 slots × 1s = 5s interval
- Max 100,000 pending tasks; worker pool = CPU cores × 2
- Cancellation check with 5-minute TTL window; each task executes at most once via `taskRegistry`

## Module Map

| Module | Purpose |
|--------|---------|
| `thingshub-core` | Broker, IoC, config, services, entities, ACL, script engine, transport abstractions, retry, plugins |
| `thingshub-transport/*` | MQTT, TCP, HTTP, GB28181, ONVIF protocol implementations |
| `thingshub-connector/*` | Kafka, RocketMQ, MQTT-bridge, TDengine, ClickHouse data sinks |
| `thingshub-dashboard/dashboard-server` | Reactor Netty HTTP API + custom MVC framework |
| `thingshub-dashboard/dashboard-ui` | Vue 3 + TinyPro + Vite SPA |
| `thingshub-starter` | Entry point, config.yaml, logback.xml, banner.txt |
| `thingshub-mcp` | MCP server module (scaffolded, not yet implemented) |
| `build` | Assembly descriptors, startup scripts (`bin/startup.sh`, `bin/startup.bat`) |

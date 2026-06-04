# Thingshub 物联网平台

## 1、系统简介
Thingshub基于Java 17、reactor-netty和Ignite进行开发，是一个比较完整的企业级物联网平台，能帮助企业快速将设备接入平台，并向上游业务系统提供标准化接口与设备进行互操作。

Thingshub提供多种标准协议支持，完全自有实现，不依赖第三方消息中间件。

对于厂家设备私有协议，可通过脚本可对私有协议进行转换，理论上可支持任何私有协议设备的接入。


## 2、系统架构

### 2.1、整体架构
物联网平台是物联网企业技术平台不可或缺的组成部分，作为设备管理的技术底座，上游对接各类业务系统，下游连接各种业务场景的设备。它在整体个企业技术平台中的具体位置如下图红色部分所示：

![平台总体架构](./overall.jpg)

### 2.2、Thingshub架构
	
Thingshub物联网平台的架构如下图所示：

![Thingshub架构](./thingshub-architecture.jpg)


## 3、核心特性

#### 3.1、统一接入
支持MQTT、TCP、HTTP、GB28181等协议，通过插件机制可实现企业私有协议。通过协议转换脚本可将不同厂商、不同设备的消息以统一数据格式提供给上游业务系统。
	
#### 3.2、设备管理
在产品类别、产品定义、设备和设备分组、物模型、消息模型、协议适配等维度对设备进行统一管理。
	
#### 3.3、权限控制
对上游业务系统按产品、消息名称等维度进行操作控制，对业务系统用户按产品、设备、消息等维度进行访问控制。
	
#### 3.4、规则引擎
自定义规则配置，实现设备告警、场景联动等功能
	
#### 3.5、数据桥接
可通过连接器将数据路由到现有消息中间件或时序数据库，比如Kafka、MQTT、RocketMQ、ClickHouse、Tdengine等
	
#### 3.6、系统监控

	
#### 3.7、系统集成
通过插件与企业现有平台进行无缝集成


## 4、快速开始

### 4.1、源码构建

#### 4.1.1、前提条件

* JDK 17+
* Maven 3.8.0+

#### 4.1.2、获取源码

克隆仓库到本地工作空间:

```
cd <YOUR_WORKSPACE>
git clone https://github.com/thingshub-io/thingshub thingshub
```

配置maven toolchains：
```
<toolchain>
  <type>jdk</type>
  <provides>
    <version>1.8</version>
    <vendor>oracle</vendor>
  </provides>
  <configuration>
    <jdkHome>D:/java8</jdkHome>
  </configuration>
</toolchain>
  
<toolchain>
  <type>jdk</type>
  <provides>
    <version>17</version>
    <vendor>graalvm</vendor>
  </provides>
  <configuration>
    <jdkHome>D:/graalvm-jdk-17</jdkHome>
  </configuration>
</toolchain>
```

#### 4.1.3、构建
进入项目根目录，执行以下命令:

```
cd thingshub
mvn clean install
```

构建输出文件位于 `/build/target`
 
* `thingshub-<VERSION>-standalone.tar.gz`
* `thingshub-<VERSION>-standalone-windows.zip`

### 4.2、启动运行

解压`thingshub-<VERSION>-standalone.tar.gz`文件，你将看到以下目录结构：

```
|- bin
|- doc
|- etc
|- lib
|- log
|- work
```

想要启动或停止Thingshub，可在在bin目录分别执行以下命令：

- **启动Thingshub**，运行：
  ```
  ./startup.sh
  ```

- **停止Thingshub**，运行：
  ```
  ./shutdown.sh
  ```


## 5、项目结构

```lua
thingshub
├── benchmark -- 基准测试脚本
├── build -- 构建输出
├── thingshub-commons -- 与thingshub-client共用的组件
├── thingshub-connector -- 连接器
│   ├── thingshub-connector-clickhouse -- clickhouse连接器
│   ├── thingshub-connector-kafka -- Kafka连接器
│   ├── thingshub-connector-mqtt -- MQTT Broker连接器
│   ├── thingshub-connector-rocketmq -- RocketMQ连接器
│   ├── thingshub-connector-tdengine -- Tdegine连接器
├── thingshub-core -- Thingshub核心模块
├── thingshub-dashboard -- Thingshub控制台管理模块
├── thingshub-mcp -- Thingshub MCP服务器
├── thingshub-starter -- Thingshub启动代码
├── thingshub-transport -- Thingshub标准协议服务器实现
│   ├── thingshub-transport-gb28181 -- 视频监控国标实现
│   ├── thingshub-transport-http -- HTTP通信协议实现
│   ├── thingshub-transport-mqtt -- MQTT通信协议实现
│   ├── thingshub-transport-onvif -- 视频监控国际标准实现
│   ├── thingshub-transport-tcp -- TCP通信协议实现
```

	
## 6、技术栈
- [reactor-netty](https://projectreactor.io/)
- [Ignite](https://ignite.apache.org/)
- [Guice](https://github.com/google/guice)
- [TinyPro of Vue](https://opentiny.design/vue-pro/docs/start)
- ......

	
## 7、参考资料
- [MQTT-3.1.1规范](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html)
- [MQTT-5.0规范](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html)
- [GB/T 28181-2022规范](http://c.gb688.cn/bzgk/gb/showGb?type=online&hcno=8BBC2475624A6C31DC34A28052B3923D&request_locale=zh)
- [BifroMQ](https://bifromq.apache.org/)


## 8、联系方式
vx: albert394005

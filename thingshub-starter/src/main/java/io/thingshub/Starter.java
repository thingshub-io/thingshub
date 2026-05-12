package io.thingshub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import com.google.common.collect.Maps;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;

/**
 * <p>
 * Broker Starter
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class Starter {

	public static void main(String[] args) throws Exception {
		Map<String, Object> configs = Maps.newHashMap();

		try (InputStream ins = Starter.class.getResourceAsStream("/config.yaml")) {
			if (ins == null) {
				throw new BootException("config.yaml not exists");
			}

			Constructor c = new Constructor(new LoaderOptions());
			c.setPropertyUtils(new PropertyUtils() {
				@Override
				public Property getProperty(Class<? extends Object> type, String name) {
					if (name.indexOf('-') > -1) {
						name = YmlCamelCase.camelize(name);
					}
					return super.getProperty(type, name);
				}
			});
			Yaml yaml = new Yaml(c);
			Map<String, Object> items = yaml.load(ins);
			String rootKey = "";
			flatConfigs(items, rootKey, configs);
		} catch (Exception e) {
			Logger logger = LoggerFactory.getLogger(Starter.class);
			logger.error("", e);
			throw new BootException("Reading config.yaml failed. Error: ", e);
		}

		try (InputStream ins = Starter.class.getResourceAsStream("/banner.txt")) {
			if (ins != null) {
				printBanner(ins);
			}
		} catch (Exception e) {
			Logger logger = LoggerFactory.getLogger(Starter.class);
			logger.error("", e);
			throw new BootException("Reading banner.txt failed. Error: ", e);
		}

		Broker.builder().configs(configs).build().startup().block();
	}

	@SuppressWarnings("unchecked")
	private static void flatConfigs(Map<String, Object> items, String parentKey, Map<String, Object> configs) {
		items.entrySet().forEach(entry -> {
			if (entry.getValue() instanceof LinkedHashMap child) {
				flatConfigs(child, parentKey + (parentKey.equals("") ? "" : ".") + entry.getKey(), configs);
			} else {
				// visitor
				configs.put(parentKey + (parentKey.equals("") ? "" : ".") + entry.getKey(), entry.getValue());
				System.getProperties().put(parentKey + (parentKey.equals("") ? "" : ".") + entry.getKey(), entry.getValue());
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void printBanner(InputStream bannerIns) throws IOException {
		Logger logger = LoggerFactory.getLogger(Starter.class);

		try (BufferedReader br = new BufferedReader(new InputStreamReader(bannerIns));) {
			String banner = "\n", line = "";
			while ((line = br.readLine()) != null) {
				banner += (line + "\n");
			}

			if (logger instanceof ch.qos.logback.classic.Logger theLogger) {
				LoggerContext lc = theLogger.getLoggerContext();
				ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender) lc.getLogger("io.thingshub").getAppender("CONSOLE");
				PatternLayoutEncoder encoderOfConsoleAppender = (PatternLayoutEncoder) consoleAppender.getEncoder();
				encoderOfConsoleAppender.stop();
				String pattern1 = encoderOfConsoleAppender.getPattern();
				encoderOfConsoleAppender.setPattern("%msg%n");
				encoderOfConsoleAppender.start();

				RollingFileAppender<ILoggingEvent> fileAppender = (RollingFileAppender) lc.getLogger("io.thingshub").getAppender("FILE");
				PatternLayoutEncoder encoderOfFileAppender = (PatternLayoutEncoder) fileAppender.getEncoder();
				encoderOfFileAppender.stop();
				String pattern2 = encoderOfFileAppender.getPattern();
				encoderOfFileAppender.setPattern("%msg%n");
				encoderOfFileAppender.start();

				logger.info(banner);

				encoderOfConsoleAppender.stop();
				encoderOfConsoleAppender.setPattern(pattern1);
				encoderOfConsoleAppender.start();

				encoderOfFileAppender.stop();
				encoderOfFileAppender.setPattern(pattern2);
				encoderOfFileAppender.start();
			}
		} catch (Exception e) {
			logger.error("Reading banner failed", e);
			throw new BootException("Reading banner failed. Error: ", e);
		}
	}

}
package com.simon.springmvc.http.netty.util.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.*;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;

/**
 * @author xiele
 * @date 2018/04/07
 */
public class ServerLoggerFactory {

    private static final LoggerContext lc = (LoggerContext) LogManager.getContext(false);
    private static final Configuration loggerConf = lc.getConfiguration();

    private static final String ACCESS_LOGGER_KEY = "accessLogger";


    /**
     * 创建Logger
     * @param logger
     * @param loggerPath
     * @param loggerName
     */
    private static void createLogger(String logger, String loggerPath, String loggerName) {
        if (loggerConf.getLoggers().containsKey(loggerName)) {
            return ;
        }
        //创建一个展示的样式：PatternLayout
        PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(loggerConf)
                .withPattern("%d{yyyy-MM-dd HH:mm:ss:SSS} %-5level %class{36}[%L] %M - %msg%xEx%n")
                .build();

        //单个日志文件大小
        TimeBasedTriggeringPolicy tbtp = TimeBasedTriggeringPolicy.createPolicy(null, null);
        TriggeringPolicy tp = SizeBasedTriggeringPolicy.createPolicy("10M");
        CompositeTriggeringPolicy policyComposite = CompositeTriggeringPolicy.createPolicy(tbtp, tp);

        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.createStrategy(
                "7", "1", null, null, null, false, loggerConf);
        //日志路径
        final String loggerPathPrefix = loggerPath  + loggerName;
        RollingFileAppender appender = RollingFileAppender.newBuilder()
                .withFileName(loggerPathPrefix)
                .withFilePattern(loggerPathPrefix + ".%d{yyyy-MM-dd}.%i")
                .withAppend(true)
                .withStrategy(strategy)
                .withName(logger)
                .withPolicy(policyComposite)
                .withLayout(layout)
                .setConfiguration(loggerConf)
                .build();
        appender.start();

        loggerConf.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef(logger, null, null);
        AppenderRef[] refs = new AppenderRef[]{ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false,
                Level.ALL, logger, "true", refs, null, loggerConf, null);
        loggerConfig.addAppender(appender, null, null);

        loggerConf.addLogger(logger, loggerConfig);
        lc.updateLoggers();

    }

    /**使用完之后记得调用此方法关闭动态创建的logger，避免内存不够用或者文件打开太多*/
    public static void stop(String loggerName) {
        synchronized (loggerConf){
            loggerConf.getAppender(loggerName).stop();
            loggerConf.getLoggerConfig(loggerName).removeAppender(loggerName);
            loggerConf.removeLogger(loggerName);
            lc.updateLoggers();
        }
    }


    /**获取Logger*/
    public static Logger getLogger(String logger) {
        synchronized (loggerConf) {
            if (!loggerConf.getLoggers().containsKey(logger)) {
                throw new IllegalArgumentException("please create logger file");
            }
        }
        return LogManager.getLogger(logger);
    }

    public static void createAccessLoggerIfNotExist(String fullLoggerFile) {
        createAccessLoggerIfNotExist(getLogPath(fullLoggerFile), getLoggerName(fullLoggerFile));
    }


    public static Logger getLogger() {
        return getLogger(ACCESS_LOGGER_KEY);
    }

    private static void createAccessLoggerIfNotExist(String loggerPath, String loggerName) {
        createLogger(ACCESS_LOGGER_KEY, loggerPath, loggerName);
    }

    private static String getLogPath(String loggerFile) {
        LoggerUtil.invalidFormat(loggerFile);
        return loggerFile.substring(0, loggerFile.lastIndexOf(File.separator) + 1);
    }

    private static String getLoggerName(String loggerFile) {
        LoggerUtil.invalidFormat(loggerFile);
        return loggerFile.substring(loggerFile.lastIndexOf(File.separator) + 1);
    }


    static class LoggerUtil {

        public static void invalidFormat(String loggerFile) {
            if (loggerFile == null || File.separator.equals(loggerFile)) {
                throw new IllegalArgumentException("illegal logger file which can not be null or root path");
            }
            if (!loggerFile.contains(File.separator)) {
                throw new IllegalArgumentException("illegal logger file which should be contains a direction");
            }

            if (!loggerFile.contains(".log")) {
                throw new IllegalArgumentException("illegal logger file which should be end with .log");
            }
        }
    }


    public static void main(String[] args) {

        String file = "/Users/simon/logs/log4j2/access_log.log";
        String logPath = getLogPath(file);
        String loggerName = getLoggerName(file);
        createAccessLoggerIfNotExist(logPath, loggerName);
        Logger logger = getLogger();

        for(int i=0; i<1000;i++) {
            logger.info("Hello,this is a test-{}", i);
        }

        stop(ACCESS_LOGGER_KEY);

    }


}

# app
# set ${APP_NAME}, if empty $(basename "${APP_HOME}") will be used.


export CATALINA_HOME=/opt/tomcat
export CATALINA_BASE=$APP_HOME/.default
export CATALINA_LOGS=$APP_HOME/logs/catalina
export CATALINA_OUT=$APP_HOME/logs/tomcat_stdout.log
export CATALINA_PID=$CATALINA_BASE/catalina.pid
# 7001 is recommended here, you can also configure Tomcat port if you've hard coded in your app.
export TOMCAT_PORT="7001"
# symbolic link jboss_stdout.log to tomcat_stdout.log
export JBOSS_OUT=$APP_HOME/logs/jboss_stdout.log

# tomcat jvm options
CATALINA_OPTS="-server"
CATALINA_OPTS="${CATALINA_OPTS} -Xms4g -Xmx4g"
CATALINA_OPTS="${CATALINA_OPTS} -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
CATALINA_OPTS="${CATALINA_OPTS} -Xmn2g"
CATALINA_OPTS="${CATALINA_OPTS} -XX:MaxDirectMemorySize=1g"
CATALINA_OPTS="${CATALINA_OPTS} -XX:SurvivorRatio=10"
CATALINA_OPTS="${CATALINA_OPTS} -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSMaxAbortablePrecleanTime=5000"
CATALINA_OPTS="${CATALINA_OPTS} -XX:+CMSClassUnloadingEnabled -XX:CMSInitiatingOccupancyFraction=80 -XX:+UseCMSInitiatingOccupancyOnly"
CATALINA_OPTS="${CATALINA_OPTS} -XX:+ExplicitGCInvokesConcurrent -Dsun.rmi.dgc.server.gcInterval=2592000000 -Dsun.rmi.dgc.client.gcInterval=2592000000"
CATALINA_OPTS="${CATALINA_OPTS} -XX:ParallelGCThreads=${CPU_COUNT}"
CATALINA_OPTS="${CATALINA_OPTS} -Xloggc:${MIDDLEWARE_LOGS}/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
CATALINA_OPTS="${CATALINA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${MIDDLEWARE_LOGS}/java.hprof"
CATALINA_OPTS="${CATALINA_OPTS} -Djava.awt.headless=true"
CATALINA_OPTS="${CATALINA_OPTS} -Dsun.net.client.defaultConnectTimeout=10000"
CATALINA_OPTS="${CATALINA_OPTS} -Dsun.net.client.defaultReadTimeout=30000"
CATALINA_OPTS="${CATALINA_OPTS} -DJM.LOG.PATH=${MIDDLEWARE_LOGS}"
CATALINA_OPTS="${CATALINA_OPTS} -DJM.SNAPSHOT.PATH=${MIDDLEWARE_SNAPSHOTS}"
CATALINA_OPTS="${CATALINA_OPTS} -Dfile.encoding=${JAVA_FILE_ENCODING}"
CATALINA_OPTS="${CATALINA_OPTS} -Dhsf.publish.delayed=true"

export CATALINA_OPTS

# project.name for hsf
CATALINA_OPTS="$CATALINA_OPTS -Dproject.name=$APP_NAME"

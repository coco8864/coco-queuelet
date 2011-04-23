#!/bin/sh
JAVA_HOME=/usr/j2se
QUEUELET_HOME=/export/home/naru/myProxy
cd $QUEUELET_HOME
${JAVA_HOME}/bin/java -server -Xms128m -Xmx128m -verbose:gc -DQUEUELET_HOME=${QUEUELET_HOME} -jar ${QUEUELET_HOME}/bin/queuelet-boot-1.1.0.jar myProxy.xml


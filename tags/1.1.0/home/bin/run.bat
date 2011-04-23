echo on
cd %QUEUELET_HOME%
"%JAVA_HOME%\bin\java" -Xms128m -Xmx128m -verbose:gc -DQUEUELET_HOME=%QUEUELET_HOME% -jar %QUEUELET_HOME%\bin\queuelet-boot-1.1.0.jar %1 %2 %3 %4 %5


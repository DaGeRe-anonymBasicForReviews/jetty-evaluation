resultFolder=../tree-results/
if [ -d $resultFolder/traces ]
then
	mv $resultFolder/traces $resultFolder/traces_2
fi
mkdir -p $resultFolder/traces

java -jar tests/jetty-jmh/target/benchmarks.jar -bm ss -i 100 -f 1 \
	-jvmArgsAppend "-Dkieker.monitoring.core.controller.WriterController.RecordQueueFQN=java.util.concurrent.ArrayBlockingQueue -Djava.io.tmpdir=$resultFolder/traces" 

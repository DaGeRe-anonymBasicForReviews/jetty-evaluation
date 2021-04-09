if [ -d tree-results/traces ]
then
	mv tree-results/traces tree-results/traces_2
fi
mkdir -p tree-results/traces

java -jar tests/jetty-jmh/target/benchmarks.jar -bm ss -i 100 -f 1 \
	-jvmArgsAppend "-javaagent:/home/$USER/.m2/repository/net/kieker-monitoring/kieker/1.15-SNAPSHOT/kieker-1.15-SNAPSHOT-aspectj.jar -Dkieker.monitoring.core.controller.WriterController.RecordQueueFQN=java.util.concurrent.ArrayBlockingQueue -Djava.io.tmpdir=tree-results/traces" \
       	"EWYKBenchmark.testStrategy"

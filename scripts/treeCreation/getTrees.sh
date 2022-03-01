start=$(pwd)

PROJECTFOLDER=$1

if [ ! -d $PROJECTFOLDER ]
then
	echo "Projectfolder needs to be passed as first argument!"
fi

treefolder=$PROJECTFOLDER/../tree-results

if [ -d $treefolder/traces ]
then
	        mv $treefolder/traces $treefolder/traces_2
fi
mkdir -p $treefolder/traces

cd ../../

mvn clean package &> $start/build.txt
java -cp target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peassEvaluation.ExecutionPreparer $PROJECTFOLDER &> $start/preparation.txt

(cd $PROJECTFOLDER && mvn -V -B clean install -DskipTests -Dlicense.skip -Denforcer.skip -Dcheckstyle.skip -T6 -e -pl :jetty-jmh -am &> $start/compilation.txt)

(cd $PROJECTFOLDER && java -jar tests/jetty-jmh/target/benchmarks.jar -bm ss -i 100 -f 1 \
	-jvmArgsAppend "-Dkieker.monitoring.core.controller.WriterController.RecordQueueFQN=java.util.concurrent.ArrayBlockingQueue -Djava.io.tmpdir=$treefolder/traces" &> $start/execution.txt)

java -cp target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peassEvaluation.GetTrees -projectFolder $PROJECTFOLDER -dataFolder $treefolder &> $start/treeReading.txt

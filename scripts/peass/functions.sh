function executeRTS {
	i=$1
	echo "Analyzing $version"
	java -cp $PEASS_PROJECT/starter/target/peass-starter-*-SNAPSHOT.jar de.dagere.peass.debugtools.DependencyReadingContinueStarter \
		-dependencyfile deps_jetty.project.json \
		-folder jetty.project/ \
		-skipProcessSuccessRuns \
		-pl ":jetty-jmh" \
		-doNotUpdateDependencies &> regression-$i/dependencylog.txt
	mv results/deps_jetty.project_out.json regression-$i

	java -cp $PEASS_PROJECT/starter/target/peass-starter-*-SNAPSHOT.jar de.dagere.peass.dependency.traces.TraceGeneratorStarter \
	        -dependencyfile regression-$i/deps_jetty.project_out.json \
	        -pl ":jetty-jmh" \
		-folder jetty.project &> regression-$i/tracelog.txt
	mkdir -p regression-$i/results
	mv results/views_jetty.project regression-$i/results
}

function executeRCA {
	i=$1
	testName=$2
	version=$3
	vms=$4
	repetitions=$5
	mkdir regression-$i/properties_jetty.project
	java -jar $PEASS_PROJECT/starter/target/peass-starter-*-SNAPSHOT.jar readproperties \
		-dependencyfile regression-$i/deps_jetty.project_out.json -folder jetty.project/ \
		-changefile regression-$i/results/changes_*.json \
		-viewfolder regression-$i/results/views_jetty.project/ \
		-out regression-$i/properties_jetty.project/properties.json &> regression-$i/readproperties.txt

	java -jar $PEASS_PROJECT/starter/target/peass-starter-*-SNAPSHOT.jar searchcause \
		-dependencyfile regression-$i/deps_jetty.project_out.json -folder jetty.project/ \
		-iterations 10 \
		-warmup 0 \
		-repetitions $repetitions \
		-vms $vms \
		-timeout 5 \
		-measurementStrategy PARALLEL \
		--rcaStrategy UNTIL_SOURCE_CHANGE \
		-propertyFolder regression-$i/properties_jetty.project/ \
		-version $version \
		-pl ":jetty-jmh" \
		-test $testName &> regression-$i/rca.txt
}

function getRepetitions {
	i=$1
	methodName=$(cat  regression-$i/test.txt | awk -F '#' '{print $2}')
	clazzName=$(cat regression-$i/test.txt | awk -F '[§#]' '{print $2}')
	calls=$(cat regression-$i/randomselection.txt | grep "Test: " | uniq | grep $clazzName | grep $methodName | awk '{print $(NF-1)}' | uniq)
	
	if [ $calls -gt 10000 ]
	then
		repetitions=100
	else
		if [ $calls -gt 1000 ]
		then
			repetitions=1000
		else
			repetitions=100000
		fi
	fi
	echo $repetitions
}

function measure {
	i=$1
	testName=$2
	version=$3
	vms=$4
	repetitions=$5

	echo "Measuring $testName Repetitions: $repetitions"
	java -cp $PEASS_PROJECT/starter/target/peass-starter-*-SNAPSHOT.jar de.dagere.peass.DependencyTestStarter \
		-dependencyfile regression-$i/deps_jetty.project_out.json -folder jetty.project/ \
		-iterations 10 \
		-warmup 0 \
		-repetitions $repetitions \
		-vms $vms \
		-timeout 5 \
		-measurementStrategy PARALLEL \
		-version $version \
		-pl ":jetty-jmh" \
		-test $testName	&> regression-$i/measurelog.txt
	
	java -jar $PEASS_PROJECT/starter/target/peass-starter-*-SNAPSHOT.jar getchanges \
		-data jetty.project_peass/measurementsFull/*.xml &> regression-$i/getchanges.txt
	mv results/* regression-$i/results/
}

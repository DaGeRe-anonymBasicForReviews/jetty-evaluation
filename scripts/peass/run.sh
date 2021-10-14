#!/bin/bash

# For ubuntu usage, insert to /etc/sysctl.conf
#net.ipv4.tcp_tw_reuse = 1
#net.ipv4.tcp_fin_timeout = 10
#net.ipv4.tcp_keepalive_time = 10
# and call sudo sysctl -p /etc/sysctl.conf 

# This script needs gawk to work (mawk does not work!)
hasgawk=$(awk 2>&1 | grep gawk)
if [ -z "$hasgawk" ]
then
	echo "No or wrong awk - please install gawk!"
	exit 1
fi

if [ ! -d jetty.project ]
then
	git clone git@github.com:DaGeRe/jetty-experiments.git jetty.project
else
	cd jetty.project && git reset --hard && cd ..
fi

#if [ ! -d jetty-traces ]
#then
#	git clone git@github.com:DaGeRe/jetty-traces.git
#else
#	cd jetty-traces && git pull && cd ..
#fi

vms=100

for i in 1
do
	cd jetty.project/ && git checkout regression-$i &> ../checkout.txt

	version=$(git rev-parse HEAD)
	cd ..

	mkdir regression-$i
	mv checkout.txt regression-$i
	
	echo "Analyzing $version"
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.dagere.peass.debugtools.DependencyReadingContinueStarter \
		-dependencyfile deps_jetty.project.json \
		-folder jetty.project/ \
		-skipProcessSuccessRuns \
		-pl ":jetty-jmh" \
		-doNotUpdateDependencies &> regression-$i/dependencylog.txt

#	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.dagere.peass.dependency.traces.TraceGeneratorStarter \
#	        -dependencyfile results/deps_jetty.project.json -pl ":jetty-jmh" \
#		-folder jetty.project &> regression-$i/tracelog.txt

	method=$(cat ../regressions.csv | grep "regression-$i;" | awk -F';' '{print $3}')

	java -cp ../../target/jetty-evaluation-0.1-SNAPSHOT.jar \
		de.dagere.peassEvaluation.SelectTest \
		-dependencyfile results/deps_jetty.project_out.json \
		-tracesFolder jetty-traces/regression-$i/results/ \
		-method $method \
		-folder jetty.project/ &> regression-$i/randomselection.txt
        
	mv results/deps_jetty.project_out.json regression-$i
	mv test.txt regression-$i
	if [ -f regression-$i/test.txt ]
	then
		
		
		methodName=$(cat  regression-$i/test.txt | awk -F '#' '{print $2}')
		clazzName=$(cat regression-$i/test.txt | awk -F '[§#]' '{print $2}')
		calls=$(cat regression-$i/randomselection.txt | grep "Test: " | uniq | grep $clazzName | grep $methodName | awk '{print $(NF-1)}')
		
		echo "Calls: $calls"
		if [ $calls -gt 1000 ]
		then
			repetitions=1000
		else
			repetitions=100000
		fi
		testName=$(cat regression-$i/test.txt)
		echo "Measuring $testName Calls: $calls Repetitions: $repetitions"
		java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.dagere.peass.DependencyTestStarter \
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
		
		java -jar $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar getchanges \
			-data jetty.project_peass/measurementsFull/*.xml
		mv results/* regression-$i/results/
		
		changes=$(cat regression-$i/results/changes_*.json | jq ".versionChanges.\"$version\".testcaseChanges | keys[0]")
		
		echo "Changes: $changes"
		
		if [ ! -z "$changes" ]
		then
			mkdir regression-$i/properties_jetty.project
			java -jar $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar readproperties \
				-dependencyfile regression-$i/deps_jetty.project_out.json -folder jetty.project/ \
				-changefile regression-$i/results/changes_*.json \
				-viewfolder jetty-traces/regression-$i/results/views_jetty.project/ \
				-out regression-$i/properties_jetty.project/properties.json
		
			java -jar $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar searchcause \
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
		fi
	else
		echo "No test was selected"
	fi
	
	mv jetty.project_peass regression-$i
		
done

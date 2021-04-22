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

if [ ! -d jetty-traces ]
then
	git clone git@github.com:DaGeRe/jetty-traces.git
else
	cd jetty-traces && git pull && cd ..
fi

for i in {1..10}
do
	cd jetty.project/ && git checkout regression-$i && cd ..

	cd jetty.project
	version=$(git rev-parse HEAD)
	cd ..

	echo "Analyzing $version"
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.debugtools.DependencyReadingContinueStarter \
		-dependencyfile deps_jetty.project.json -folder jetty.project/ -doNotUpdateDependencies &> dependencylog.txt

	method=$(cat ../regressions.csv | grep "regression-$i;" | awk -F';' '{print $3}')

	java -cp ../../../../target/jetty-evaluation-0.1-SNAPSHOT.jar \
		de.dagere.peassEvaluation.SelectTest \
		-dependencyfile results/deps_jetty.project.json \
		-tracesFolder jetty-traces/regression-$i/results/ \
		-method $method \
		-folder jetty.project/ &> randomselection.txt

        methodName=$(cat test.txt | awk -F '#' '{print $2}')
        clazzName=$(cat test.txt | awk -F '[§#]' '{print $2}')
	calls=$(cat randomselection.txt | grep "Test: TestCase " | uniq | grep $clazzName | grep $methodName | awk '{print $(NF-1)}')

	if [ $calls -gt 1000 ]
	then
		repetitions=10000
	else
		repetitions=1000000
	fi
	testName=$(cat test.txt)
	echo "Measuring $testName Calls: $calls Repetitions: $repetitions"
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.DependencyTestStarter \
		-dependencyfile results/deps_jetty.project.json -folder jetty.project/ \
		-iterations 10 \
		-warmup 0 \
		-repetitions $repetitions \
		-vms 100 \
		-timeout 5 \
		-measurementStrategy PARALLEL \
		-version $version -pl ":jetty-jmh" \
		-test $testName	&> measurelog.txt
	
	mv jetty.project_peass regression-$i
	mv measurelog.txt regression-$i
	mv dependencylog.txt regression-$i
done

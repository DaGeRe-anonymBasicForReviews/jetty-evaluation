#!/bin/bash

# For ubuntu usage, insert to /etc/sysctl.conf
#net.ipv4.tcp_tw_reuse = 1
#net.ipv4.tcp_fin_timeout = 10
#net.ipv4.tcp_keepalive_time = 10
# and call sudo sysctl -p /etc/sysctl.conf 


if [ ! -d jetty.project ]
then
	git clone git@github.com:DaGeRe/jetty-experiments.git jetty.project
else
	cd jetty.project && git reset --hard && cd ..
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

	java -cp ../../../../target/jetty-evaluation-0.1-SNAPSHOT.jar \
		de.dagere.peassEvaluation.SelectTest \
		-dependencyfile results/deps_jetty.project.json \
		-folder jetty.project/ &> randomselection.txt

	testName=$(cat test.txt)
	echo "Measuring $testName"
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.DependencyTestStarter \
		-dependencyfile results/deps_jetty.project.json -folder jetty.project/ \
		-iterations 10 \
		-warmup 0 \
		-repetitions 10000 \
		-vms 100 \
		-timeout 5 \
		-measurementStrategy PARALLEL \
		-version $version -pl ":jetty-jmh" \
		-test $testName	&> measurelog.txt
	
	mv jetty.project_peass regression-$i
	mv measurelog.txt regression-$i
	mv dependencylog.txt regression-$i
done

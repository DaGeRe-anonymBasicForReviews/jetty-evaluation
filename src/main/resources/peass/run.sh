#!/bin/bash

# For ubuntu usage, insert to /etc/sysctl.conf
#net.ipv4.tcp_tw_reuse = 1
#net.ipv4.tcp_fin_timeout = 10
#net.ipv4.tcp_keepalive_time = 10
# and call sudo sysctl -p /etc/sysctl.conf 


git clone https://github.com/DaGeRe/jetty-experiments.git jetty.project

for i in {1..10}
do
	cd jetty.project/ && git checkout regression-$i && cd ..

	cd jetty.project
	version=$(git rev-parse HEAD)
	cd ..

	echo "Analyzing $version"
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.debugtools.DependencyReadingContinueStarter \
		-dependencyfile deps_jetty.project.json -folder jetty.project/ -doNotUpdateDependencies &> dependencylog.txt

	echo "Measuring"
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.DependencyTestStarter \
		-dependencyfile results/deps_jetty.project.json -folder jetty.project/ \
		-iterations 5 \
		-repetitions 1000 \
		-vms 200 \
		-timeout 3 \
		-measurementStrategy PARALLEL \
		-version $version -pl ":jetty-jmh" &> measurelog.txt
	
	mv jetty.project_fullPeass regression-$i
	mv measurelog.txt regression-$i
	mv dependencylog.txt regression-$i
done
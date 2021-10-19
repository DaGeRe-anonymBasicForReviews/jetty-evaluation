#!/bin/bash

# For ubuntu usage, insert to /etc/sysctl.conf
#net.ipv4.tcp_tw_reuse = 1
#net.ipv4.tcp_fin_timeout = 10
#net.ipv4.tcp_keepalive_time = 10
# and call sudo sysctl -p /etc/sysctl.conf 

source functions.sh

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

vms=30

for i in 1
do
	cd jetty.project/ && git checkout regression-$i &> ../checkout.txt
	
	version=$(git rev-parse HEAD)
	cd ..
	
	if [ -d results/ ]
	then
		rm results/* -rf
	fi

	mkdir regression-$i
	mv checkout.txt regression-$i
	
	executeRTS $i

	method=$(cat ../regressions.csv | grep "regression-$i;" | awk -F';' '{print $3}')

	cd jetty.project && git reset --hard && cd ..
	java -cp ../../target/jetty-evaluation-0.1-SNAPSHOT.jar \
		de.dagere.peassEvaluation.SelectTest \
		-dependencyfile regression-$i/deps_jetty.project_out.json \
		-tracesFolder regression-$i/results/ \
		-method $method \
		-folder jetty.project/ &> regression-$i/randomselection.txt
	
	mv test.txt regression-$i
	if [ -f regression-$i/test.txt ]
	then
		repetitions=$(getRepetitions $i)
		
		testName=$(cat regression-$i/test.txt)
		
		measure $i $testName $version $vms $repetitions
		
		foundVersion=$(cat regression-$i/results/changes_*.json | jq ".versionChanges | keys[0]" | tr -d "\"")
		changes=$(cat regression-$i/results/changes_*.json | jq ".versionChanges.\"$foundVersion\".testcaseChanges | keys[0]")
		
		echo "Changes: $changes"
		
		if [ ! -z "$changes" ]
		then
			executeRCA $i $testName $version $vms $repetitions
		fi
	else
		echo "No test was selected"
	fi
	
	mv jetty.project_peass regression-$i
		
done

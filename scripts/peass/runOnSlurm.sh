#!/bin/bash

#SBATCH --cpu-freq=high-high
#SBATCH --cpus-per-task=24
#SBATCH --ntasks=1

source functions.sh

export PATH=/home/sc.uni-leipzig.de/do820mize/maven/apache-maven-3.8.3/bin:/usr/lib/jvm/java-11-openjdk-11.0.12.0.7-0.el7_9.x86_64/bin/java:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/nfs/user/do820mize/pxz:/nfs/user/do820mize/tar-1.29/bin/bin:/nfs/user/do820mize/git/git-2.9.5/bin-wrappers:/home/sc.uni-leipzig.de/do820mize/bin/jq

echo "Executing regression-$i"

startFolder=$(pwd)
resultFolder=$startFolder/peass-jetty-results
mkdir -p $resultFolder
echo "Final results go to $resultFolder"

experimentFolder=/tmp/peass-evaluation-rts/
if [ -d $experimentFolder ]
then
        rm -rf $experimentFolder
fi
if [ -d $experimentFolder/scripts/peass/ ]
then
	echo "Cleaning old data $experimentFolder/scripts/peass"
	rm -rf $experimentFolder/scripts/peass
else
	echo "Should not exist: $experimentFolder/scripts/peass/"
	ls $experimentFolder/scripts/peass/
fi

mkdir -p $experimentFolder/scripts/peass/regression-$i
cd $experimentFolder/scripts/peass/

echo "Executing in "$(pwd)

vms=100

git clone git@github.com:DaGeRe/jetty-experiments.git jetty.project
cd jetty.project/ && git checkout regression-$i &> ../checkout.txt

version=$(git rev-parse HEAD)
cd ..

if [ -d results/ ]
then
	rm results/* -rf
fi

mv checkout.txt regression-$i

cp $startFolder/deps_jetty.project.json .
cp -R $startFolder/../../target $experimentFolder
cp $startFolder/../regressions.csv $experimentFolder/scripts

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

mv regression-$i $resultFolder

echo "Finally deleting temporary folder"
ls
rm ../../* -rf
		

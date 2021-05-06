#!/bin/bash

#SBATCH --cpu-freq=high-high
#SBATCH --cpus-per-task=24
#SBATCH --ntasks=1

export PATH=/nfs/user/do820mize/maven/apache-maven-3.6.3/bin:/usr/lib/jvm/java-11-openjdk-11.0.10.0.9-0.el7_9.x86_64/bin/:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/nfs/user/do820mize/pxz:/nfs/user/do820mize/tar-1.29/bin/bin:/nfs/user/do820mize/git/git-2.9.5/bin-wrappers

echo "Executing $version"

startFolder=$(pwd)
resultFolder=$startFolder/peass-evaluation-jetty-results
mkdir -p $resultFolder
echo "Final results go to $resultFolder"

experimentFolder=/tmp/peass-evaluation-rts/$version
if [ -d $experimentFolder ]
then
        rm -rf $experimentFolder
fi
mkdir -p $experimentFolder
cd $experimentFolder

git clone /home/sc.uni-leipzig.de/do820mize/jetty-experiments
cd jetty-experiments
mkdir -p jmh_results

git checkout $version
mvn -V -B clean package \
        -DskipTests -Dlicense.skip -Denforcer.skip -Dcheckstyle.skip -Djacoco.skip \
         -T6 -e -pl :jetty-jmh -am
java -jar tests/jetty-jmh/target/benchmarks.jar \
        -rff jmh_results/$version.json -rf json \
         -i 3 -t 3 -wi 3
         
mv $experimentFolder $resultFolder


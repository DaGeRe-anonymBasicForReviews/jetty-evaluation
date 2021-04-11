#SBATCH --cpu-freq=high-high
#SBATCH --cpus-per-task=24
#SBATCH --ntasks=1

export PATH=/nfs/user/do820mize/maven/apache-maven-3.5.4/bin:/usr/jdk64/jdk1.8.0_112/bin/:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/nfs/user/do820mize/pxz:/nfs/user/do820mize/tar-1.29/bin/bin:/nfs/user/do820mize/git/git-2.9.5/bin-wrappers

echo "Executing $benchmark on $regression"

startFolder=$(pwd)
resultFolder=$startFolder/peass-evaluation-jetty-results
mkdir -p $resultFolder

experimentFolder=/tmp/peass-evaluation-jetty/$regression
mkdir -p $experimentFolder
cd $experimentFolder

git clone https://github.com/DaGeRe/jetty-experiments
cd jetty-experiments
mkdir -p jmh_results

git checkout b56edf511ab4399122ea2c6162a4a5988870f479
mvn -V -B clean install \
	-DskipTests -Dlicense.skip -Denforcer.skip -Dcheckstyle.skip \
	 -T6 -e -pl :jetty-jmh -am
java -jar tests/jetty-jmh/target/benchmarks.jar \
	-rff jmh_results/basic.json -rf json \
	 -i 3 -t 3 -wi 3 $benchmark

git checkout $regression
mvn -V -B clean install \
	-DskipTests -Dlicense.skip -Denforcer.skip -Dcheckstyle.skip \
	 -T6 -e -pl :jetty-jmh -am
java -jar tests/jetty-jmh/target/benchmarks.jar \
	-rff jmh_results/$regression.json -rf json \
	 -i 3 -t 3 -wi 3 $benchmark
	 
mv $experimentFolder $resultFolder
start=$(pwd)

mkdir treeLogs
mkdir treeResults

folder=$1
peassResultsFolder=$2


for i in {0..999}
do
	echo "Starting $i"
	(cd $folder && git reset --hard && git checkout regression-$i)
	cd $peassResultsFolder/regression-$i

	tar -xf deps.tar.xz
	tar -xf logs.tar.xz randomselection.txt
        testcase=$(tail -n 1 randomselection.txt | awk '{print $NF}')
	version=$(cd $folder && git rev-parse HEAD)
	rm randomselection.txt

	echo "Test: $testcase Version: $version"
	$PEASS_PROJECT/peass searchcause \
	       	-vms 0 -pl ":jetty-jmh" \
		-folder $folder \
		-dependencyfile $peassResultsFolder/regression-$i/deps_jetty.project_out.json \
		-test $testcase \
		-version $version &> $start/treeLogs/regression-$i
	mv "$folder"_peass $start/treeResults/regression-$i

	rm randomselection.txt
	rm deps_jetty.project_out.json
	echo
done

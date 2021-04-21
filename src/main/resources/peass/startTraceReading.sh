start=$(pwd)

if [ ! -d jetty.project ]
then
	git clone git@github.com:DaGeRe/jetty-experiments.git jetty.project
else
	cd jetty.project && git reset --hard && cd ..
fi

for i in {1..10}
do
	cd $start

	echo "Starting regression-$i"
	mkdir regression-$i
	cd regression-$i
	git clone $start/jetty.project jetty.project
        cd jetty.project
        git checkout regression-$i
        cd ..

	cp $start/dependencies.json deps_jetty.project.json

	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.debugtools.DependencyReadingContinueStarter \
                -dependencyfile deps_jetty.project.json -folder jetty.project/ -doNotUpdateDependencies &> dependencylog.txt

	# Using the Peass ViewGenerator at this point is not fast, since it
	# - always reads the traces of both versions, 
	# - reads all called methods and 
	# - compacts the traces,
	# which is all not required here. 
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.dependency.traces.ViewGenerator \
		-dependencyfile results/deps_jetty.project.json -pl ":jetty-jmh" \
		-folder jetty.project &> log.txt &
done

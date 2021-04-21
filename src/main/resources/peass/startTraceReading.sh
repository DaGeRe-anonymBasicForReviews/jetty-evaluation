start=$(pwd)

if [ ! -d jetty.project ]
then
	git clone git@github.com:DaGeRe/jetty-experiments.git jetty.project
	cd jetty.project
	for i in {0..999}; do git checkout regression-$i; done
	cd ..
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

	cp $start/deps_jetty.project.json deps_jetty.project.json

	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.debugtools.DependencyReadingContinueStarter \
                -dependencyfile deps_jetty.project.json -folder jetty.project/ -doNotUpdateDependencies &> dependencylog.txt

	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.dependency.traces.TraceGeneratorStarter \
		-dependencyfile results/deps_jetty.project.json -pl ":jetty-jmh" \
		-folder jetty.project &> log.txt &
done

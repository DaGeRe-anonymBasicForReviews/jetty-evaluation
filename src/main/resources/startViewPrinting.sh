# This starts generating all dependency files and all views

start=$(pwd)

for i in {0..999}
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

	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.dagere.peass.debugtools.DependencyReadingContinueStarter \
                -dependencyfile deps_jetty.project.json -folder jetty.project/ -doNotUpdateDependencies -doNotGenerateViews -doNotGenerateCoverageSelection &> dependencylog.txt

	mv results/deps_jetty.project_out.json results/deps_jetty.project.json
	java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.dagere.peass.dependency.traces.TraceGeneratorStarter \
		-dependencyfile results/deps_jetty.project.json -pl ":jetty-jmh" \
		-folder jetty.project &> log.txt &

	echo "Sleeping 2 minutes"
	date
	sleep 2m
done

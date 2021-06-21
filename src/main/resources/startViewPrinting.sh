# This starts generating all dependency files and all views

function startViewPrinting() {
	i=$1
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
}

start=$(pwd)

startViewPrinting 0

for i in {1..999}
do
	cd $start

	startViewPrinting $i	

	echo "Sleeping 15 minutes"
	date
	sleep 15m
done


cd jetty.project
version=$(git rev-parse HEAD)
cd ..

echo "Analyzing $version"
#java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.debugtools.DependencyReadingContinueStarter \
#	-dependencyfile deps_jetty.project.json -folder jetty.project/ -doNotUpdateDependencies &> dependencylog.txt

echo "Measuring"
java -cp $PEASS_PROJECT/distribution/target/peass-distribution-0.1-SNAPSHOT.jar de.peass.DependencyTestStarter \
	-dependencyfile results/deps_jetty.project.json -folder jetty.project/ \
	-iterations 10 \
	-repetitions 1000000 \
	-vms 200 \
	-version $version &> measurelog.txt

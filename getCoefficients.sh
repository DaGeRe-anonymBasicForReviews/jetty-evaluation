# Gets the count of occurences of the method with regression, the length of the test case and the relation between both
# This supports the analysis of measurement results: If a method with a regression if often called by a test, it is likely
# that the regression can be measured easily

# Please replace these for local execution
folder=/home/reichelt/daten4/diss/repos/jetty-evaluation/peass/traces
jettyFolder=/home/reichelt/nvme/workspaces/dissworkspace/projects/jetty.project
for file in $(ls $folder | grep regression)
do
	echo -n "$file "
	method=$(cat src/main/resources/regressions.csv | grep "$file;" | awk -F';' '{print $3}')
	echo -n "$method "
	java -cp target/jetty-evaluation-0.1-SNAPSHOT.jar \
                de.dagere.peassEvaluation.SelectTest \
                -dependencyfile $folder/$file/results/deps_jetty.project.json \
                -tracesFolder $folder/$file/results \
                -method $method \
                -folder $jettyFolder &> randomselection.txt
	methodName=$(cat test.txt | awk -F '#' '{print $2}')
	clazzName=$(cat test.txt | awk -F '[§#]' '{print $2}')
	cat randomselection.txt | grep "Test: TestCase " | uniq | grep $clazzName | grep $methodName | awk '{print $NF" "$(NF-1)" "$(NF-2)}'
	if [ ! -f test.txt ]
	then
		echo "Result file of java process could not be found"
		exit 1
	fi
	rm test.txt
done

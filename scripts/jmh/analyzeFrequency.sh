treeFolder=$2
if [ ! -d $treeFolder ]
then
	echo "TreeFolder was no folder: $treeFolder"
	exit 1
fi

#java -cp ../../target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peass.rtsEvaluation.ReadJMHMeasuredRegressions $1 \
#	| grep "regression" | awk '{print $1}' | uniq > identified.txt

# Not a very nice solution, that java requires regressions.csv in a fixed path, move to resources sometime or make a parameter
#(cd ../../ && java -cp target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peassEvaluation.treeReading.GetMinimalTreeDepth $treeFolder > scripts/jmh/treeDepth.csv)

simpleTraceFolder=$treeFolder/simpleTraces
if [ ! -d $simpleTraceFolder ]
then
	echo "SimpleTraceFolder was not existing: $simpleTraceFolder"
	exit 1
fi

function getSum {
  awk '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)" "sum/NR" "NR}'
}

function getRegressionCalls {
	regression=$1
	selectedBenchmark=$(cat ../regressions.csv | grep $regression";" | awk -F';' '{print $2}')
	changedMethod=$(cat ../regressions.csv | grep $regression";" | awk -F';' '{print $3}')
	changedMethodCalls=$(cat $simpleTraceFolder/root-"$selectedBenchmark"* | grep  $changedMethod | wc -l)
	calls=$(cat $simpleTraceFolder/root-"$selectedBenchmark"* | wc -l)
	echo "$regression ChangedCalls: $changedMethodCalls OverallCalls: $calls"
}

mkdir -p results

for regression in $(cat identified.txt)
do
	getRegressionCalls $regression
done > results/correctValues.csv

for regression in $(cat identified.txt)
do
	cat treeDepth.csv | grep $regression" " | awk '{print $4}'
done > results/correctDepth.csv


echo -n "Share of changed method on correct measurements: "
cat results/correctValues.csv | awk '{if ($5 != 0) {print $3/$5}}' | getSum

echo -n "Method call count on correct measurement: "
cat results/correctValues.csv | awk '{if ($5 != 0) {print $5}}' | getSum

echo -n "Average tree depth on correct measurements: "
cat results/correctDepth.csv | getSum

for regression in $(cat ../regressions.csv | awk -F';' '{print $1}' | grep -vxFf identified.txt)
do
	getRegressionCalls $regression
done > results/wrongMeasurementValues.csv

for regression in $(cat ../regressions.csv | awk -F';' '{print $1}' | grep -vxFf identified.txt)
do
	cat treeDepth.csv | grep $regression" " | awk '{print $4}'
done > results/wrongDepth.csv

echo -n "Share of changed method on wrong measurements: "
cat results/wrongMeasurementValues.csv | awk '{if ($5 != 0) {print $3/$5}}' | getSum

echo -n "Method call count on wrong measurement: "
cat results/wrongMeasurementValues.csv | awk '{if ($5 != 0) {print $5}}' | getSum

echo -n "Average tree depth on wrong measurements: "
cat results/wrongDepth.csv | getSum

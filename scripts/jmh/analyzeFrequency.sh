#java -cp ../../target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peass.rtsEvaluation.ReadJMHMeasuredRegressions $1 \
#	| grep "regression" | awk '{print $1}' | uniq > identified.txt

treeFolder=$2
if [ ! -d $treeFolder ]
then
	echo "TreeFolder was no folder: $treeFolder"
	exit 1
fi
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

for regression in $(cat identified.txt)
do
	getRegressionCalls $regression
done > correctValues.csv

for regression in $(cat ../regressions.csv | awk -F';' '{print $1}' | grep -vxFf identified.txt)
do
	getRegressionCalls $regression
done > wrongMeasurementValues.csv

echo -n "Share of changed method on correct measurements: "
cat correctValues.csv | awk '{if ($5 != 0) {print $3/$5}}' | getSum

echo -n "Share of changed method on wrong measurements: "
cat wrongMeasurementValues.csv | awk '{if ($5 != 0) {print $3/$5}}' | getSum


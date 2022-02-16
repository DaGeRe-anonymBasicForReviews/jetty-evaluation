#java -cp ../../target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peass.rtsEvaluation.ReadJMHMeasuredRegressions $1 \
#	| grep "regression" | awk '{print $1}' | uniq > identified.txt

function getSum {
  awk '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)" "sum/NR" "NR}'
}

for regression in $(cat identified.txt)
do
	selectedBenchmark=$(cat ../regressions.csv | grep $regression";" | awk -F';' '{print $2}')
	changedMethod=$(cat ../regressions.csv | grep $regression";" | awk -F';' '{print $3}')
	changedMethodCalls=$(cat $2/simpleTraces/root-"$selectedBenchmark"* | grep  $changedMethod | wc -l)
	calls=$(cat $2/simpleTraces/root-$selectedBenchmark_* | wc -l)
	echo "ChangedCalls: $changedMethodCalls OverallCalls: $calls"
done > correctValues.csv

for regression in $(cat ../regressions.csv | awk -F';' '{print $1}' | grep -vxFf identified.txt)
do
	selectedBenchmark=$(cat ../regressions.csv | grep $regression";" | awk -F';' '{print $2}')
	changedMethod=$(cat ../regressions.csv | grep $regression";" | awk -F';' '{print $3}')
	changedMethodCalls=$(cat $2/simpleTraces/root-"$selectedBenchmark"* | grep  $changedMethod | wc -l)
	calls=$(cat $2/simpleTraces/root-$selectedBenchmark_* | wc -l)
	echo "ChangedCalls: $changedMethodCalls OverallCalls: $calls"
done > wrongMeasurementValues.csv

echo -n "Share of changed method on correct measurements: "
cat correctValues.csv | awk '{print $2/$4}' | getSum

echo -n "Share of changed method on wrong measurements: "
cat wrongMeasurementValues.csv | awk '{print $2/$4}' | getSum


function getSum {
  awk '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)" "sum/NR" "NR}'
}

rm invocations.txt

for line in $(cat ../regressions.csv)
do
	benchmark=$(echo $line | awk -F';' '{print $2}')
	changedMethod=$(echo $line | awk -F';' '{print $3}')
	changedMethodCalls=$(cat $1/simpleTraces/root-$selectedBenchmark_* | grep  $changedMethod | wc -l)
	calls=$(cat $1/simpleTraces/root-$selectedBenchmark_* | wc -l)
	echo "ChangedCalls: $changedMethodCalls OverallCalls: $calls"
done > invocations.txt

echo -n "Overall share of changed method: "
cat invocations.txt | awk '{print $2/$4}' | getSum

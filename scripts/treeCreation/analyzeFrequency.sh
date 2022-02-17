function getSum {
  awk '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)" "sum/NR" "NR}'
}

treeFolder=$1
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

rm invocations.txt

for line in $(cat ../regressions.csv)
do
	regression=$(echo $line | awk -F';' '{print $1}')
	selectedBenchmark=$(echo $line | awk -F';' '{print $2}')
	changedMethod=$(echo $line | awk -F';' '{print $3}')
	changedMethodCalls=$(cat $simpleTraceFolder/root-"$selectedBenchmark"* | grep  $changedMethod | wc -l)
	calls=$(cat $1/simpleTraces/root-"$selectedBenchmark"* | wc -l)
	echo "$regression ChangedCalls: $changedMethodCalls OverallCalls: $calls"
done > invocations.txt

echo -n "Overall share of changed method: "
cat invocations.txt | awk '{if ($5 != 0) {print $3/$5}}' | getSum

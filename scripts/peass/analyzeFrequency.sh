function printValues {
        regressions=$1
        resultfile=$2

        echo -n > $resultfile
        for regression in $(cat $regressions)
        do
                tar -xf $regression/logs.tar.xz randomselection.txt
                testcase=$(tail -n 1 randomselection.txt | awk '{print $NF}')
                #echo $testcase
                cat randomselection.txt | grep "Test: $testcase" | tail -n 1 | awk '{print $(NF-3)" "$(NF-2)" "$(NF-1)}' >> $resultfile
        done
}

function getSum {
  awk '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)" "sum/NR" "NR}'
}


start=$(pwd)

cd $1

printValues $start/results/correct.txt $start/results/correctValues.csv
printValues $start/results/wrongMeasurementResult.txt $start/results/wrongMeasurementValues.csv

cd $start

echo -n "Share of changed method on correct measurements: "
cat results/correctValues.csv | awk '{print $2/$3}' | getSum

echo -n "Method call count on correct measurement: "
cat results/correctValues.csv | awk '{if ($3 != 0) {print $3}}' | getSum

echo -n "Share of changed method on wrong measurements: "
cat results/wrongMeasurementValues.csv | awk '{print $2/$3}' | getSum

echo -n "Method call count on wrong measurement: "
cat results/wrongMeasurementValues.csv | awk '{if ($3 != 0) {print $3}}' | getSum



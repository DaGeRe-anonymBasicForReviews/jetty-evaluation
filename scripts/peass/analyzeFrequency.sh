function printValues {
        regressions=$1
        resultfile=$2

        echo -n > $resultfile
        for regression in $(cat $regressions)
        do
                tar -xvf $regression/logs.tar.xz randomselection.txt &> /dev/null
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

printValues results/correct.txt results/correctValues.csv
printValues results/wrongMeasurementResult.txt results/wrongMeasurementValues.csv

echo -n "Share of changed method on correct measurements: "
cat results/correctValues.csv | awk '{print $2/$3}' | getSum

echo -n "Share of changed method on wrong measurements: "
cat results/wrongMeasurementValues.csv | awk '{print $2/$3}' | getSum

cd $start

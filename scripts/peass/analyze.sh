function printCorrect {
	echo -n > results/notSelectedChanges.txt
	echo -n > results/wrongMeasurementResult.txt
	echo -n "Correct Measurement: "
	for regression in regression-*
	do 
		if [ -f $regression/results/changes*json ]
		then
			length=$(cat $regression/results/changes*json | jq ".versionChanges | length")
			echo $length
			if (( $length < 1 ))
			then
				echo $regression >> results/wrongMeasurementResult.txt
			fi
		else
			echo $regression >> results/notSelectedChanges.txt
		fi
	done | awk '{sum+=$1} END {print sum}'
}

function printRCACorrect {
	echo -n > results/rcaIncorrect.txt
	echo -n "Correct RCA in root node: "
	for regression in regression-*
	do 
		if [ -f $regression/jetty.project_peass/rca/tree/*/*/*.json ]
		then
			tvalue=$(cat $regression/jetty.project_peass/rca/tree/*/*/*.json | jq ".nodes.statistic.tvalue")
			absoluteTvalue=${tvalue#-}
			# 2,750 is the critical t-value for 30 measurements and 99% significance level
			greater=$(echo "$absoluteTvalue > 2.750" | bc -l)
			if [ $greater ]
			then
				echo "1"
			else
				echo $regression >> results/rcaIncorrect.txt
			fi
		fi
	done | awk '{sum+=$1} END {print sum}'
}

function printRCACorrectLeaf {
	echo -n > results/rcaIncorrectLeaf.txt
	echo -n "Correct RCA in leaf node: "
	for regression in regression-*
	do 
		if [ -f $regression/jetty.project_peass/rca/tree/*/*/*.json ]
		then
			tvalue=$(tac $regression/jetty.project_peass/rca/tree/*/*/*.json | grep tvalue | head -n 1 | tr -d "\"tvalue:-")
			# 2,750 is the critical t-value for 30 measurements and 99% significance level
			greater=$(echo "$tvalue > 2.750" | bc -l)
			if [ $greater ]
			then
				echo "1"
			else
				echo $regression >> results/rcaIncorrectLeaf.txt
			fi
		fi
	done | awk '{sum+=$1} END {print sum}'
}

mkdir -p results

printCorrect
printRCACorrect
printRCACorrectLeaf

echo -n "Not selected changes: "
cat results/notSelectedChanges.txt | wc -l

echo -n "Overall: "
ls | grep regression | wc -l

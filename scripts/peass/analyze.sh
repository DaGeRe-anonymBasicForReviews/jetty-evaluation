function isSelected {
	regression=$1
	tar -xvf $regression/deps.tar.xz -C $regression &> /dev/null
	ls $regression >> results/regression
	versions=$(cat $regression/deps_jetty.project_out.json | jq ".versions")
	version=$(echo $versions | jq "keys[1]")
	vals=$(echo $versions | jq ".$version.changedClazzes | .[]")
	rm $regression/deps_jetty.project_out.json
	if [ "$vals" == "{}" ]
	then
		echo "0"
	else
		echo "1"
	fi
}

function printCorrect {
	echo -n > results/notSelectedChanges.txt
	echo -n > results/wrongMeasurementResult.txt
	echo -n > results/wrongAnalysis.txt
	echo -n > results/correct.txt
	for regression in regression-*
	do 
		if [ -f $regression/results/changes*json ]
		then
			length=$(cat $regression/results/changes*json | jq ".versionChanges | length")
			if (( $length < 1 ))
			then
				echo $regression >> results/wrongMeasurementResult.txt
			else
				echo $regression >> results/correct.txt
			fi
			statisticsLength=$(cat $regression/results/statistics/*json | jq ".statistics | length")
			if (( $statisticsLength < 1 ))
			then
				echo $regression >> results/wrongAnalysis.txt
			fi
		else
			selected=$(isSelected $regression)
			if [ $selected ]
			then
				echo $regression >> results/notSelectedChanges.txt
			fi
		fi
	done
}

function printRCACorrect {
	echo -n > results/rcaCorrect.txt
	echo -n > results/rcaIncorrect.txt
	echo -n > results/rcaMissing.txt
	for regression in regression-*
	do 
		if [ -f $regression/jetty.project_peass/rca/tree/*/*/*.json ]
		then
			tvalue=$(cat $regression/jetty.project_peass/rca/tree/*/*/*.json | jq ".nodes.statistic.tvalue")
			absoluteTvalue=${tvalue#-}
			# 2,750 is the critical t-value for 30 measurements and 99% significance level
			greater=$(echo "$absoluteTvalue > 2.750" | bc -l)
			if [ "$greater" -eq "1" ]
			then
				echo $regression >> results/rcaCorrect.txt
			else
				echo $regression >> results/rcaIncorrect.txt
			fi
		else
			selected=$(isSelected $regression)
			if [ -f $regression/results/changes*json ] && [ ! $selected ]
			then
				echo $regression >> results/rcaMissing.txt
			fi
		fi
	done
}

function printRCACorrectLeaf {
	echo -n > results/rcaIncorrectLeaf.txt
	echo -n > results/rcaCorrectLeaf.txt
	for regression in regression-*
	do 
		if [ -f $regression/jetty.project_peass/rca/tree/*/*/*.json ]
		then
			tvalue=$(tac $regression/jetty.project_peass/rca/tree/*/*/*.json | grep tvalue | head -n 1 | tr -d "\"tvalue:-")
			# 2,750 is the critical t-value for 30 measurements and 99% significance level
			greater=$(echo "$tvalue > 2.750" | bc -l)
			if [ $greater ]
			then
				echo $regression >> results/rcaCorrectLeaf.txt
			else
				echo $regression >> results/rcaIncorrectLeaf.txt
			fi
		fi
	done
}

startfolder=$(pwd)

cd $1

mkdir -p results

printCorrect
echo -n "Correct Measurement: "
cat results/correct.txt | wc -l

printRCACorrect
echo -n "Correct RCA in root node: "
cat results/rcaCorrect.txt | wc -l

printRCACorrectLeaf
echo -n "Correct RCA in leaf node: "
cat results/rcaCorrectLeaf.txt | wc -l

echo -n "Not selected changes: "
cat results/notSelectedChanges.txt | wc -l

echo -n "Wrong analysis (should be 0): "
cat results/wrongAnalysis.txt | wc -l

echo -n "RCA missing (should be 0): "
cat results/rcaMissing.txt | wc -l

echo -n "Overall: "
ls | grep regression | wc -l

cd $startfolder

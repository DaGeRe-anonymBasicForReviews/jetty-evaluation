function isSelected {
	regression=$1
	tar -xvf $regression/deps.tar.xz -C $regression &> /dev/null

	# This is a debug output
	# ls $regression >> results/regression_ls_result

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

startfolder=$(pwd)

cd $1

mkdir -p results

printCorrect
echo -n "Correct Measurement: "
cat results/correct.txt | wc -l

echo -n "Not selected changes: "
cat results/notSelectedChanges.txt | wc -l

echo -n "Wrong measurement result: "
cat results/wrongMeasurementResult.txt | wc -l

echo -n "Wrong analysis (should be 0): "
cat results/wrongAnalysis.txt | wc -l

echo -n "Overall: "
ls | grep regression | wc -l

cd $startfolder

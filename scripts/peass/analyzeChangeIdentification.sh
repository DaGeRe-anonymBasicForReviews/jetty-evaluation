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
	resultfolder=$1
	echo -n > $resultfolder/results/notSelectedChanges.txt
	echo -n > $resultfolder/results/wrongMeasurementResult.txt
	echo -n > $resultfolder/results/wrongAnalysis.txt
	echo -n > $resultfolder/results/correct.txt
	for regression in $(ls | grep regression- | grep -v ".tar")
	do 
		echo "Analyzing $regression"
		if [ -f $regression/results/changes*json ]
		then
			length=$(cat $regression/results/changes*json | jq ".versionChanges | length")
			if (( $length < 1 ))
			then
				echo $regression >> $resultfolder/results/wrongMeasurementResult.txt
			else
				echo $regression >> $resultfolder/results/correct.txt
			fi
			statisticsLength=$(cat $regression/results/statistics/*json | jq ".statistics | length")
			if (( $statisticsLength < 1 ))
			then
				echo $regression >> $resultfolder/results/wrongAnalysis.txt
			fi
		else
			selected=$(isSelected $regression)
			if [ $selected ]
			then
				echo $regression >> $resultfolder/results/notSelectedChanges.txt
			fi
		fi
	done
}

resultfolder=$(pwd)

cd $1

if [ ! -d $1 ]
then
	echo "Please provide a folder with Peass Jetty measurement data!"
fi

mkdir -p $resultfolder/results

printCorrect $resultfolder
echo -n "Correct Measurement: "
cat $resultfolder/results/correct.txt | wc -l

echo -n "Not selected changes: "
cat $resultfolder/results/notSelectedChanges.txt | wc -l

echo -n "Wrong measurement result: "
cat $resultfolder/results/wrongMeasurementResult.txt | wc -l

echo -n "Wrong analysis (should be 0): "
cat $resultfolder/results/wrongAnalysis.txt | wc -l

echo -n "Overall: "
ls | grep regression | grep -v ".tar" | wc -l

cd $resultfolder

mkdir -p /nfs/user/do820mize/processlogs/jetty-evaluation-jmh/

for line in $(cat regressions.csv | tail -n 95)
#for i in {0..100}
do
	benchmark=$(echo $line | awk -F';' '{print $2}' | awk -F'#' '{print $1}')	
	regression=$(echo $line | awk -F';' '{print $1}')
	echo "Starting benchmark $benchmark on $regression"
	sbatch \
                --nice=1 \
                --time=10-0 \
                --output=/nfs/user/do820mize/processlogs/jetty-evaluation-jmh/"%j".out \
		--export=benchmark=$benchmark,regression=$regression \
		runEvaluation.sh
done

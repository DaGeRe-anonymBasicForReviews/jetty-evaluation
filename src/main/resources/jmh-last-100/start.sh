mkdir -p /nfs/user/do820mize/processlogs/jetty-evaluation-jmh/

for version in $(cat commits.txt)
#for i in {0..100}
do
	echo "Starting version $version"
	sbatch \
                --nice=1 \
                --time=5-0 \
                --output=/nfs/user/do820mize/processlogs/jetty-evaluation-rts/"%j".out \
		--export=version=$version \
		runEvaluation.sh
done

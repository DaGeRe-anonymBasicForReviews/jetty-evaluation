mkdir -p /nfs/user/do820mize/processlogs/jetty-evaluation-rts/

#for version in $(cat commits.txt | tail -n 90)
for version in b56edf511a 16241d7fcb 
do
	echo "Starting version $version"
	sbatch \
                --nice=1 \
                --time=5-0 \
                --output=/nfs/user/do820mize/processlogs/jetty-evaluation-rts/"%j".out \
		--export=version=$version \
		runBenchmark.sh
done

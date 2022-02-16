mkdir -p /nfs/user/do820mize/processlogs/jetty-evaluation/peass

for i in {0..10}
do
        echo "Starting regression-$i"
        sbatch \
                --nice=1 \
                --time=5-0 \
                --output=/nfs/user/do820mize/processlogs/jetty-evaluation/peass/"%j".out \
                --export=i=$i,PEASS_PROJECT=$PEASS_PROJECT \
                runOnSlurm.sh
done


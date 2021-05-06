To get the commits, execute

git log --oneline | grep b56edf511a -A 101 | awk '{print $1}' > commits.txt

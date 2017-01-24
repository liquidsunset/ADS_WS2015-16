#/bin/bash

echo "This script prints the nr of lines with true in them"
echo "Don't forget to put this script into a results folder"

for f in *;
do
	echo "$f: $(cat $f | grep true | wc -l)"
done

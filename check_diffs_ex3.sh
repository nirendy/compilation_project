#!/bin/bash

mkdir -p temp_out
mkdir -p temp_expected
mkdir -p temp_actual

echo "Running semantic logic..."

printf "\n\nChecking semantic passing results against expected:\n"
echo "---------------------------------"

echo "OK" > temp_out/passing.txt
for path in $(ls examples/ex3/test_cases/passing/*.xml); do
	echo "Checking $(basename $path)..."
	java -jar mjavac.jar unmarshal semantic examples/ex3/test_cases/passing/$(basename $path) temp_out/output.txt
	diff temp_out/output.txt temp_out/passing.txt
	echo "================================="
done

printf "\n\nChecking semantic failing results against expected:\n"
echo "---------------------------------"

echo "ERROR" > temp_out/failing.txt
for path in $(ls examples/ex3/test_cases/failing/*.xml); do
	echo "Checking $(basename $path)..."
  java -jar mjavac.jar unmarshal semantic examples/ex3/test_cases/failing/$(basename $path) temp_out/output.txt
  diff temp_out/output.txt temp_out/failing.txt
	echo "================================="
done

printf "\n\nFinishing up...  "
rm -rf temp_out
rm -rf temp_expected
rm -rf temp_actual
echo "Done!"
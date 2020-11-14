#!/bin/bash

mkdir -p temp_out

echo "Running renaming logic..."

java -jar mjavac.jar unmarshal rename method theMethod 11 renamedMethod  examples/ex1/test_cases/method_renaming/method_related_sibling_class.xml temp_out/method_related_sibling_class_renamed.xml
java -jar mjavac.jar unmarshal rename method theMethod 19 renamedMethod  examples/ex1/test_cases/method_renaming/method_unrelated_sibling_class.xml temp_out/method_unrelated_sibling_class_renamed.xml
java -jar mjavac.jar unmarshal rename var theVar 10 renamedVar examples/ex1/test_cases/method_renaming/field_related_sibling_class.xml temp_out/field_related_sibling_class_renamed.xml

echo "Checking results against expected:"
echo "---------------------------------"
for path in $(ls examples/ex1/test_cases/method_renaming/*_renamed*.xml); do
	echo "Checking $(basename $path)..."
	diff temp_out/$(basename $path) $path
	echo "================================="
done

echo "Finishing up..."
rm -rf temp_out
echo "Done!"


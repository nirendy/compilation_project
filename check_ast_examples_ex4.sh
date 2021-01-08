#!/bin/bash


java -jar mjavac.jar parse marshal   examples/ast/BinarySearch.java   BinarySearch_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/BinaryTree.java     BinaryTree_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/BubbleSort.java     BubbleSort_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/Factorial.java      Factorial_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/LinearSearch.java   LinearSearch_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/LinkedList.java     LinkedList_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/QuickSort.java      QuickSort_out.java.xml
java -jar mjavac.jar parse marshal   examples/ast/TreeVisitor.java    TreeVisitor_out.java.xml


diff  examples/ast/BinarySearch.java.xml   BinarySearch_out.java.xml
diff  examples/ast/BinaryTree.java.xml     BinaryTree_out.java.xml
diff  examples/ast/BubbleSort.java.xml     BubbleSort_out.java.xml
diff  examples/ast/Factorial.java.xml      Factorial_out.java.xml
diff  examples/ast/LinearSearch.java.xml   LinearSearch_out.java.xml
diff  examples/ast/LinkedList.java.xml     LinkedList_out.java.xml
diff  examples/ast/QuickSort.java.xml      QuickSort_out.java.xml
diff  examples/ast/TreeVisitor.java.xml    TreeVisitor_out.java.xml

rm BinarySearch_out.java.xml
rm BinaryTree_out.java.xml
rm BubbleSort_out.java.xml
rm Factorial_out.java.xml
rm LinearSearch_out.java.xml
rm LinkedList_out.java.xml
rm QuickSort_out.java.xml
rm TreeVisitor_out.java.xml
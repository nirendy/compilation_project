#!/bin/bash


java -jar mjavac.jar unmarshal compile  examples/ast/BinarySearch.java.xml   BinarySearch.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/BinaryTree.java.xml     BinaryTree.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/BubbleSort.java.xml     BubbleSort.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/Factorial.java.xml      Factorial.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/LinearSearch.java.xml   LinearSearch.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/LinkedList.java.xml     LinkedList.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/QuickSort.java.xml      QuickSort.java.ll
java -jar mjavac.jar unmarshal compile  examples/ast/TreeVisitor.java.xml    TreeVisitor.java.ll

package ast.llvm_formatting;

import ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class LLVMObjectOrientedUtils {

    Map<String, Map<String, MethodData>> classToMethodsMapping;
    Map<String, Map<String, VarDecl>> classToFieldsMapping;

    public static class MethodData {
        String declaringClass;
        final String methodName;
        final int index;
        final AstType returnType;
        final List<AstType> formalArgsTypes;

        private MethodData(String className, MethodDecl methodNode, int index) {
            this.declaringClass = className;
            this.methodName = methodNode.name();
            this.index = index;
            this.returnType = methodNode.returnType();

            List<AstType> formalArgsTypes = new ArrayList<>();
            for (FormalArg formalArg : methodNode.formals()) {
                formalArgsTypes.add(formalArg.type());
            }
            this.formalArgsTypes = formalArgsTypes;
        }

        private MethodData(String className, String methodName, int index, AstType returnType,
                           List<AstType> formalArgsTypes) {
            this.declaringClass = className;
            this.methodName = methodName;
            this.index = index;
            this.returnType = returnType;
            this.formalArgsTypes = formalArgsTypes;
        }

        private MethodData getRedeclaredCopy(String overridingClassName) {
            return new MethodData(overridingClassName, this.methodName,
                    this.index, this.returnType, this.formalArgsTypes);
        }
    }

    public LLVMObjectOrientedUtils(Program program) {
        classToMethodsMapping = createClassToMethodsMapping(program);
        classToFieldsMapping = createClassToFieldsMapping(program);
    }

    public int getFieldOffset(String className, String fieldName) {
        // The first field of each class starts at offset 8, since the first 8 bytes are the reference to the V-table
        int offset = 8;
        for (VarDecl field : classToFieldsMapping.get(className).values()) {
            if (field.name().equals(fieldName))
                return offset;

            offset += typeToSize(field.type());
        }

        return offset;
    }

    public AstType getFieldType(String className, String fieldName) {
        return classToFieldsMapping.get(className).get(fieldName).type();
    }

    public int getMethodIndex(String className, String methodName) {
        return classToMethodsMapping.get(className).get(methodName).index;
    }

    public AstType getMethodReturnType(String className, String methodName) {
        return classToMethodsMapping.get(className).get(methodName).returnType;
    }

    public List<AstType> getMethodFormalArgsTypes(String className, String methodName) {
        return classToMethodsMapping.get(className).get(methodName).formalArgsTypes;
    }

    public int getInstanceSize(String className) {
        return getFieldOffset(className, null);
    }

    public int getNumberOfMethods(String className) {
        return classToMethodsMapping.get(className).size();
    }

    private int typeToSize(AstType type) {
        if (type instanceof IntAstType)
            return 4;
        else if (type instanceof BoolAstType)
            return 1;

        // The type is of a reference
        return 8;
    }

    private Map<String, Map<String, MethodData>> createClassToMethodsMapping(Program program) {
        /* Creating a mapping between each class in the program to its methods, while taking inheritance into account.
        * Each method has a saved index, which is consistent throughout inheritance */
        Map<String, Map<String, MethodData>> classToMethodsMapping = new HashMap<>();

        Map<String, MethodData> classMethods;
        int index;
        for (ClassDecl classNode : program.classDecls()) {
            classMethods = new HashMap<>();

            if (classNode.superName() != null) {
                // The class has a super class, hence its inheriting its methods
                classMethods.putAll(classToMethodsMapping.get(classNode.superName()));
            }

            index = classMethods.size();
            for (MethodDecl methodNode : classNode.methoddecls()) {
                if (classMethods.containsKey(methodNode.name())) {
                    // A method can override an inherited class. In that case it shouldn't be re-added to the list,
                    // but we should update the name of the declaring class
                    MethodData baseMethodData = classMethods.remove(methodNode.name());
                    classMethods.put(methodNode.name(), baseMethodData.getRedeclaredCopy(classNode.name()));
                } else {
                    classMethods.put(methodNode.name(), new MethodData(classNode.name(), methodNode, index));
                    index++;
                }
            }
            classToMethodsMapping.put(classNode.name(), classMethods);
        }

        return classToMethodsMapping;
    }

    private Map<String, Map<String, VarDecl>> createClassToFieldsMapping(Program program) {
        /* Creating a mapping between each class in the program to its fields, while taking inheritance into account. */
        Map<String, Map<String, VarDecl>> classToFieldsMapping = new HashMap<>();

        Map<String, VarDecl> classFields;
        for (ClassDecl classNode : program.classDecls()) {
            classFields = new HashMap<>();

            if (classNode.superName() != null) {
                // The class has a super class, hence its inheriting its fields
                classFields.putAll(classToFieldsMapping.get(classNode.superName()));
            }

            for (VarDecl field : classNode.fields()) {
                classFields.put(field.name(), field);
            }

            classToFieldsMapping.put(classNode.name(), classFields);
        }

        return classToFieldsMapping;
    }

    public List<MethodData> getMethodsData(String className) {
        Map<String, MethodData> methodsMapping = classToMethodsMapping.get(className);
        return methodsMapping.values().stream()
                .sorted(Comparator.comparing((methodData) -> methodData.index))
                .collect(Collectors.toList());
    }

    public Formatter getVTablesFormatter() {
        Formatter formatter = new Formatter();

        Map<String, String> methodsFormatting;
        MethodData methodData;
        int tableSize;
        for (String className : classToMethodsMapping.keySet()) {
            tableSize = classToMethodsMapping.get(className).size();
            formatter.format("@.%s_vtable = global [%d x i8*] [", className, tableSize);

            methodsFormatting = new HashMap<>();
            for (String methodName : classToMethodsMapping.get(className).keySet()) {
                methodData = classToMethodsMapping.get(className).get(methodName);
                methodsFormatting.put(Integer.toString(methodData.index),
                        String.format("i8* bitcast (i32 (i8*, i32)* @%s.%s to i8*)", className, methodName));

            }
            for (int index = 0; index < tableSize; index++) {
                formatter.format(methodsFormatting.get(Integer.toString(index)));
                if (index < tableSize - 1)
                    formatter.format(", ");
            }

            formatter.format("]\n");
        }

        return formatter;
    }
}

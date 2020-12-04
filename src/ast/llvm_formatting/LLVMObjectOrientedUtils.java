package ast.llvm_formatting;

import ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLVMObjectOrientedUtils {

    Map<String, Map<String, MethodData>> classToMethodsMapping;
    Map<String, Map<String, VarDecl>> classesToFieldsMapping;

    private static class MethodData {
        final int index;
        final AstType returnType;
        final List<AstType> formalArgsTypes;

        private MethodData(int index, MethodDecl methodNode) {
            this.index = index;
            this.returnType = methodNode.returnType();

            List<AstType> formalArgsTypes = new ArrayList<>();
            for (FormalArg formalArg : methodNode.formals()) {
                formalArgsTypes.add(formalArg.type());
            }
            this.formalArgsTypes = formalArgsTypes;
        }
    }

    public LLVMObjectOrientedUtils(Program program) {
        classToMethodsMapping = createClassToMethodsMapping(program);
        classesToFieldsMapping = createClassToFieldsMapping(program);
    }

    public int getFieldOffset(String className, String fieldName) {
        // The first field of each class starts at offset 8, since the first 8 bytes are the reference to the V-table
        int offset = 8;
        for (VarDecl field : classesToFieldsMapping.get(className).values()) {
            if (field.name().equals(fieldName))
                return offset;

            offset += typeToSize(field.type());
        }

        return offset;
    }

    public AstType getFieldType(String className, String fieldName) {
        return classesToFieldsMapping.get(className).get(fieldName).type();
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
        // Each class instance is using the first 8 bytes for the V-table
        int size = 8;
        for (VarDecl field : classesToFieldsMapping.get(className).values()) {
            size += typeToSize(field.type());
        }

        return size;
    }

    public int getNumberOfMethods(String className) {
        return classToMethodsMapping.get(className).size();
    }

    private int typeToSize(AstType type) {
        if (type instanceof IntAstType)
            return 32;
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
                if (!classMethods.containsKey(methodNode.name())) {
                    // A method can override an inherited class. In that case it shouldn't be re-added to the list
                    classMethods.put(methodNode.name(), new MethodData(index, methodNode));
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

}

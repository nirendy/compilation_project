package ast.semantic_checks;

import ast.*;

import java.util.*;

public class ObjectOrientedUtils {

    Map<String, Map<String, MethodData>> classToMethodsMapping;
    Map<String, Map<String, VarDecl>> classToFieldsMapping;

    public static class MethodData {
        final AstType returnType;
        final List<AstType> formalArgsTypes;

        private MethodData(MethodDecl methodNode) {
            this.returnType = methodNode.returnType();

            List<AstType> formalArgsTypes = new ArrayList<>();
            for (FormalArg formalArg : methodNode.formals()) {
                formalArgsTypes.add(formalArg.type());
            }
            this.formalArgsTypes = formalArgsTypes;
        }
    }

    public ObjectOrientedUtils(Program program) {
        classToMethodsMapping = createClassToMethodsMapping(program);
        classToFieldsMapping = createClassToFieldsMapping(program);
    }

    public AstType getFieldType(String className, String fieldName) {
        return classToFieldsMapping.get(className).get(fieldName).type();
    }

    public boolean hasField(String className, String fieldName) {
        return classToFieldsMapping.get(className).containsKey(fieldName);
    }

    public boolean hasMethod(String className, String methodName) {
        return classToMethodsMapping.get(className).containsKey(methodName);
    }

    public AstType getMethodReturnType(String className, String methodName) {
        return classToMethodsMapping.get(className).get(methodName).returnType;
    }

    public List<AstType> getMethodFormalArgsTypes(String className, String methodName) {
        return classToMethodsMapping.get(className).get(methodName).formalArgsTypes;
    }

    public boolean classNotInProgram(String className) {
        return !classToFieldsMapping.containsKey(className);
    }

    private Map<String, Map<String, MethodData>> createClassToMethodsMapping(Program program) {
        /* Creating a mapping between each class in the program to its methods, while taking inheritance into account.
        * Each method has a saved index, which is consistent throughout inheritance */
        Map<String, Map<String, MethodData>> classToMethodsMapping = new HashMap<>();
        classToMethodsMapping.put(program.mainClass().name(), new HashMap<>());

        Map<String, MethodData> classMethods;
        for (ClassDecl classNode : program.classDecls()) {
            classMethods = new HashMap<>();

            if (classNode.superName() != null && classToMethodsMapping.containsKey(classNode.superName())) {
                // The class has a super class, hence its inheriting its methods
                classMethods.putAll(classToMethodsMapping.get(classNode.superName()));
            }

            for (MethodDecl methodNode : classNode.methoddecls()) {
                    classMethods.put(methodNode.name(), new MethodData(methodNode));
            }
            classToMethodsMapping.put(classNode.name(), classMethods);
        }

        return classToMethodsMapping;
    }

    private Map<String, Map<String, VarDecl>> createClassToFieldsMapping(Program program) {
        /* Creating a mapping between each class in the program to its fields, while taking inheritance into account. */
        Map<String, Map<String, VarDecl>> classToFieldsMapping = new HashMap<>();
        classToFieldsMapping.put(program.mainClass().name(), new HashMap<>());

        Map<String, VarDecl> classFields;
        for (ClassDecl classNode : program.classDecls()) {
            classFields = new HashMap<>();

            if (classNode.superName() != null && classToFieldsMapping.containsKey(classNode.superName())) {
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

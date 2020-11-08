import ast.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ProgramUtils {

    Program program;

    public ProgramUtils(Program program) {
        this.program = program;
    }

    public ClassDecl getClassNode(String className) {
        for (ClassDecl classdecl : program.classDecls()) {
            if (classdecl.name().equals(className))  {
                return classdecl;
            }
        }
        return null;
    }

    public MethodDecl getMethodNode(ClassDecl classNode, String methodName) {
        for (MethodDecl methoddecl : classNode.methoddecls()) {
            if (methoddecl.name().equals(methodName)) {
                return methoddecl;
            }
        }
        return null;
    }

    public MethodDecl getMethodNode(String className, String methodName) {
        ClassDecl classNode = getClassNode(className);
        return getMethodNode(classNode, methodName);
    }

    public VarDecl getFieldNode(String className, String fieldName) {
        ClassDecl classNode = getClassNode(className);
        for (VarDecl field : classNode.fields()) {
            if (field.name().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public boolean doesClassContainMethod(ClassDecl classNode, String methodName) {
        return getMethodNode(classNode, methodName) != null;
    }

    public boolean doesClassContainMethod(String className, String methodName) {
        return getMethodNode(className, methodName) != null;
    }

    public boolean doesClassContainField(String className, String fieldName) {
        return getFieldNode(className, fieldName) != null;
    }

    public ArrayList<String> getAffectedClassesOfMethodModifying(String originalClassName, String methodName) {
        /* Given a class and a method in it, this function returns the classes that would be affected by modifying it.
        This list contains:
        1. The given class' most ancient ancestor with a declaration of the given method name
        2. The descendants of the above class
         */
        ClassDecl firstAncestorWithMethod = getFirstAncestorWithMethod(originalClassName, methodName);
        assert (firstAncestorWithMethod != null);

        ArrayList<String> descendants = new ArrayList<>();
        descendants.add(firstAncestorWithMethod.name());
        getDescendantsNames(firstAncestorWithMethod.name(), descendants);
        return descendants;
    }

    private void getDescendantsNames(String baseClassName, ArrayList<String> descendants) {
        ArrayList<String> children = new ArrayList<>();

        for (ClassDecl classdecl : program.classDecls()) {
            if (classdecl.superName() != null && classdecl.superName().equals(baseClassName)) {
                children.add(classdecl.name());
            }
        }

        for (String child: children) {
            descendants.add(child);
            getDescendantsNames(child, descendants);
        }
    }

    private ClassDecl getFirstAncestorWithMethod(String className, String methodName) {
        /* Gathering a list of the class and its ancestors, ordered by hierarchy */
        ArrayList<ClassDecl> ancestors = new ArrayList<>();
        getAncestorsNodes(className, ancestors);

        for (ClassDecl classNode : ancestors) {
            if (doesClassContainMethod(classNode, methodName)) {
                return classNode;
            }
        }
        return null;
    }

    private void getAncestorsNodes(String baseClassName, ArrayList<ClassDecl> ancestors) {
        ClassDecl baseClassNode = getClassNode(baseClassName);
        assert (baseClassNode != null);

        ancestors.add(0, baseClassNode);
        if (baseClassNode.superName() != null) {
            getAncestorsNodes(baseClassNode.superName(), ancestors);
        }

    }

    public ArrayList<String> getAffectedClassesOfFieldModifying(String originalClassName, String fieldName) {
        /* Given a class and a field in it, this function returns the classes that would be affected by modifying it.
        This list contains:
        1. The given class
        2. The descendants of the above class, if they don't shadow the modified field with their own declaration
           (this is a recursive definition: If a certain descendant of the base class shadows the field, its children
            would not be added to the output list as well)
         */
        ArrayList<String> affectedClasses = new ArrayList<>();
        affectedClasses.add(originalClassName);
        getDescendantsWithoutFieldShadowing(originalClassName, fieldName, affectedClasses);

        return affectedClasses;
    }

    private void getDescendantsWithoutFieldShadowing(String baseClassName, String fieldName,
                                                     ArrayList<String> descendants) {
        /* Given a class name, this function extracts its descendants in the following way:
           1. Extracts the immediate children of the given base class
           2. For the children whose fields don't shadow the given field name, runs this function recursively with them
              as the base class
         */
        ArrayList<String> children = new ArrayList<>();

        for (ClassDecl classdecl : program.classDecls()) {
            if (!doesClassContainField(baseClassName, fieldName) && classdecl.superName() != null &&
                    classdecl.superName().equals(baseClassName)) {
                children.add(classdecl.name());
            }
        }

        for (String child: children) {
            descendants.add(child);
            getDescendantsWithoutFieldShadowing(child, fieldName, descendants);
        }
    }

    public HashMap<String, HashMap<String, String>> getTypesOfFieldsByClass() {
        HashMap<String, HashMap<String, String>> typesOfFieldsByClass = new HashMap<>();

        String className;
        String superClass;
        HashMap<String, String> classFields;
        HashMap<String, String> superClassFields;
        for (ClassDecl classNode : program.classDecls()) {
            className = classNode.name();
            classFields = new HashMap<>();
            superClass = classNode.superName();

            if (superClass != null) {
                // The class extends another class, so it inherits its field (since a super class precedes an inheriting
                // class in its declaration, the super class for sure has already been processed)
                superClassFields = typesOfFieldsByClass.get(superClass);
                for (String field : superClassFields.keySet()) {
                    classFields.put(field, superClassFields.get(field));
                }
            }

            // updating the field types according to its field declarations (while overriding existing fields)
            for (VarDecl fieldDecl : classNode.fields()) {
                classFields.put(fieldDecl.name(), getType(fieldDecl.type()));
            }

            typesOfFieldsByClass.put(className, classFields);
        }

        return typesOfFieldsByClass;
    }

    public String getType(AstType varType) {
        /* Given a variable type, this function returns its type according to this logic:
            - If its type is of a different class, it returns the class name
            - If its type is of a primitive type (int, String, etc), it returns null (as it's not relevant for renaming)
         */
        if (varType instanceof RefType) {
            return ((RefType) varType).id();
        }

        return null;
    }

}


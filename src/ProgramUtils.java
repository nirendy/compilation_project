import ast.ClassDecl;
import ast.MethodDecl;
import ast.Program;

import java.util.ArrayList;

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

    public boolean doesClassContainMethod(ClassDecl classNode, String methodName) {
        return getMethodNode(classNode, methodName) != null;
    }

    public boolean doesClassContainMethod(String className, String methodName) {
        return getMethodNode(className, methodName) != null;
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

}


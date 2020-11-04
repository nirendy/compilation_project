import ast.ClassDecl;
import ast.MethodDecl;
import ast.Program;

import java.util.ArrayList;

public class MethodRenameUtils {

    Program program;
    String methodName;
    int lineNumber;

    public MethodRenameUtils(Program program, String methodName, int lineNumber) {
        this.program = program;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public ArrayList<String> getAffectedClasses(String originalClassName) {
        ClassDecl firstAncestorWithMethod = getFirstAncestorWithMethod(originalClassName);
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

    private ClassDecl getFirstAncestorWithMethod(String className) {
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

    public void getAncestorsNodes(String baseClassName, ArrayList<ClassDecl> ancestors) {
        ClassDecl baseClassNode = getClassNode(baseClassName);
        assert (baseClassNode != null);

        ancestors.add(0, baseClassNode);
        if (baseClassNode.superName() != null) {
            getAncestorsNodes(baseClassNode.superName(), ancestors);
        }

    }

    public ClassDecl getClassNode(String className) {
        for (ClassDecl classdecl : program.classDecls()) {
            if (classdecl.name().equals(className))  {
                return classdecl;
            }
        }
        return null;
    }

    public boolean doesClassContainMethod(ClassDecl classNode, String methodName) {
        for (MethodDecl methoddecl : classNode.methoddecls()) {
            if (methoddecl.name().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

}

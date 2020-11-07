import ast.ClassDecl;
import ast.MethodDecl;
import ast.Program;

import java.util.ArrayList;

public class MethodRenaming extends Renaming {

    public MethodRenaming(Program program, String className, String methodName, String newMethodName) {
        super(program, className, null, methodName, newMethodName);
    }

    public void rename() {
        ArrayList<String> affectedClasses = utils.getAffectedClassesOfMethodModifying(classOfRenamedObject, objectName);
        renameMethodDeclarations(affectedClasses);
    }

    public void renameMethodDeclarations(ArrayList<String> classesNames) {
        ClassDecl classNode;
        MethodDecl methodNode;
        for (String className : classesNames) {
            classNode = utils.getClassNode(className);
            methodNode = utils.getMethodNode(classNode, objectName);
            if (methodNode != null) {
                methodNode.setName(newObjectName);
            }
        }
    }
}

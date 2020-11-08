import ast.ClassDecl;
import ast.MethodDecl;
import ast.Program;

import java.util.ArrayList;
import java.util.HashMap;

public class MethodRenaming extends Renaming {

    public MethodRenaming(Program program, String className, String methodName, String newMethodName) {
        super(program, className, null, methodName, newMethodName);
    }

    @Override
    public void rename() {
        ArrayList<String> affectedClasses = utils.getAffectedClassesOfMethodModifying(classOfRenamedObject, objectName);
        renameMethodDeclarations(affectedClasses);

        HashMap<String, HashMap<String, String>> typesOfFieldsByClass = utils.getTypesOfFieldsByClass();
        new AstMethodRenamingVisitor(typesOfFieldsByClass, affectedClasses, objectName, newObjectName).visit(program);
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

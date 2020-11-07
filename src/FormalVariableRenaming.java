import ast.AstVariableRenameVisitor;
import ast.FormalArg;
import ast.MethodDecl;
import ast.Program;

import java.util.ArrayList;

public class FormalVariableRenaming extends Renaming {

    public FormalVariableRenaming(Program program, String classOfRenamedObject, String methodOfRenamedObject,
                                 String variableName, String newVariableName) {
        super(program, classOfRenamedObject, methodOfRenamedObject, variableName, newVariableName);
    }

    public void rename() {
        ArrayList<String> classesToRename = utils.getAffectedClassesOfMethodModifying(classOfRenamedObject,
                methodOfRenamedObject);

        MethodDecl methodNode;
        for (String className : classesToRename) {
             methodNode = utils.getMethodNode(className, methodOfRenamedObject);

             /* An affected class might not have an explicit method declaration. In that case, there's nothing to do */
             if (utils.doesClassContainMethod(className, methodOfRenamedObject)) {
                 /* Renaming the formal variable of the method */
                 renameFormalVariableOfMethod(methodNode);

                 /* Renaming all the occurrences of the variable in the method */
                 new AstVariableRenameVisitor(objectName, newObjectName).visit(methodNode);
             }
        }
    }

    private void renameFormalVariableOfMethod(MethodDecl methodNode) {
        for (FormalArg formalArg : methodNode.formals()) {
            if (formalArg.name().equals(objectName)) {
                formalArg.setName(newObjectName);
                return;
            }
        }
    }
}

import ast.MethodDecl;
import ast.Program;
import ast.VarDecl;

public class LocalVariableRenaming extends Renaming {

    public LocalVariableRenaming(Program program, String classOfRenamedObject, String methodOfRenamedObject,
                                 String variableName, String newVariableName) {
        super(program, classOfRenamedObject, methodOfRenamedObject, variableName, newVariableName);
    }

    @Override
    public void rename() {
        MethodDecl methodNode = utils.getMethodNode(classOfRenamedObject, methodOfRenamedObject);

        /* Renaming the variable in its declaration */
        renameLocalVariableOfMethod(methodNode);

        /* Renaming all the occurrences of the variable in the method */
        AstVariableRenamingVisitor visitor = new AstVariableRenamingVisitor(objectName, newObjectName);
        visitor.visit(methodNode);
    }

    private void renameLocalVariableOfMethod(MethodDecl methodNode) {
        for (VarDecl varDecl : methodNode.vardecls()) {
            if (varDecl.name().equals(objectName)) {
                varDecl.setName(newObjectName);
                return;
            }
        }
    }
}

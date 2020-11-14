import ast.FormalArg;
import ast.MethodDecl;
import ast.Program;

public class FormalVariableRenaming extends Renaming {

    public FormalVariableRenaming(Program program, String classOfRenamedObject, String methodOfRenamedObject,
                                 String variableName, String newVariableName) {
        super(program, classOfRenamedObject, methodOfRenamedObject, variableName, newVariableName);
    }

    @Override
    public void rename() {
        MethodDecl methodNode = utils.getMethodNode(classOfRenamedObject, methodOfRenamedObject);

        /* Renaming the formal variable in its declaration */
        renameFormalVariableOfMethod(methodNode);

        /* Renaming all the occurrences of the variable in the method */
        AstVariableRenamingVisitor visitor = new AstVariableRenamingVisitor(objectName, newObjectName);
        visitor.visit(methodNode);
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

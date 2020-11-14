import ast.*;

import java.util.ArrayList;

public class FieldRenaming extends Renaming {

    public FieldRenaming(Program program, String classOfRenamedObject, String fieldName, String newFieldName) {
        super(program, classOfRenamedObject, null, fieldName, newFieldName);
    }

    @Override
    public void rename() {
        VarDecl fieldNode = utils.getFieldNode(classOfRenamedObject, objectName);

        // Renaming the field in its original class
        fieldNode.setName(newObjectName);

        ArrayList<String> affectedClasses = utils.getAffectedClassesOfFieldModifying(classOfRenamedObject, objectName);

        AstVariableRenamingVisitor variableVisitor = new AstVariableRenamingVisitor(objectName, newObjectName);

        // Going over all classes in the affected classes list, and for each method defined in it:
        // *) If the method has a declaration (formal or local vars) which shadows the field, continue
        // *) Otherwise, the method field usages are renamed (using the variable renaming visitor)
        ClassDecl classNode;
        for (String className : affectedClasses) {
            classNode = utils.getClassNode(className);

            for (MethodDecl methodNode : classNode.methoddecls()) {
                if (!isFieldDeclaredInMethodScope(methodNode)) {
                    variableVisitor.visit(methodNode);
                }
            }
        }
    }

    private boolean isFieldDeclaredInMethodScope(MethodDecl methodNode) {
        /* Checking if the given method has local declaration (local variable or formal arg) of the same name as the
        * renamed field (i.e. checking if the method shadows the field name) */
        for (FormalArg formalArg : methodNode.formals()) {
            if (formalArg.name().equals(objectName)) {
                return true;
            }
        }

        for (VarDecl localVar : methodNode.vardecls()) {
            if (localVar.name().equals(objectName)) {
                return true;
            }
        }

        return false;
    }
}

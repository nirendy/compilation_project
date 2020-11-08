import ast.Program;
import ast.VarDecl;

import java.util.ArrayList;
import java.util.HashMap;

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
        new AstFieldRenamingVisitor(affectedClasses, objectName, newObjectName).visit(program);
    }
}

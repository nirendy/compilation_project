import ast.Program;
import ast.RenamingType;

public class RenamingFactory {

    public static void rename(Program program, String originalName, String newName, int lineNumber) {
        AstLineNumberVisitor lineNumberVisitor = new AstLineNumberVisitor(originalName, lineNumber);
        lineNumberVisitor.visit(program);

        RenamingType renamedObject = lineNumberVisitor.renamedObject;
        String classOfRenamedObject = lineNumberVisitor.getClassOfRenamedObject();
        String methodOfRenamedObject = lineNumberVisitor.getMethodOfRenamedObject();

        Renaming renamingClass = null;

        if (renamedObject == RenamingType.METHOD) {
            renamingClass = new MethodRenaming(program, classOfRenamedObject, originalName, newName);
        }

        else if (renamedObject == RenamingType.LOCAL_VARIABLE) {
            renamingClass = new LocalVariableRenaming(program, classOfRenamedObject, methodOfRenamedObject,
                    originalName, newName);
        }

        else if (renamedObject == RenamingType.FORMAL_VARIABLE) {
            renamingClass = new FormalVariableRenaming(program, classOfRenamedObject, methodOfRenamedObject,
                    originalName, newName);
        }

        else if (renamedObject == RenamingType.FIELD) {
            renamingClass = new FieldRenaming(program, classOfRenamedObject, originalName, newName);
        }

        assert (renamingClass != null);
        renamingClass.rename();
    }
}

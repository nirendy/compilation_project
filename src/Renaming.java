import ast.AstLineNumberVisitor;
import ast.Program;
import ast.RenamingType;

public class Renaming {

    Program program;
    String originalName;
    String newName;
    int lineNumber;

    public Renaming(Program program, String originalName, String newName, int lineNumber) {
        this.program = program;
        this.originalName = originalName;
        this.newName = newName;
        this.lineNumber = lineNumber;
    }

    public void rename() {
        AstLineNumberVisitor lineNumberVisitor = new AstLineNumberVisitor(originalName, lineNumber);
        lineNumberVisitor.visit(program);

        if (lineNumberVisitor.renamedObject == RenamingType.METHOD) {
            MethodRenaming methodRenaming = new MethodRenaming(program, originalName, newName, lineNumber);
            methodRenaming.renameMethod(lineNumberVisitor.getClassOfRenamedObject());
        }
    }
}

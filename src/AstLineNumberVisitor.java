import ast.*;

public class AstLineNumberVisitor implements Visitor {

    String originalName;
    int lineNumber;
    boolean isMethod;

    public RenamingType renamedObject;

    private String currentClass;
    private String currentMethod;

    public AstLineNumberVisitor(String originalName, int lineNumber, boolean isMethod) {
        this.originalName = originalName;
        this.lineNumber = lineNumber;
        this.isMethod = isMethod;
    }

    public String getClassOfRenamedObject() {
        return currentClass;
    }

    public String getMethodOfRenamedObject() {
        if (renamedObject == RenamingType.METHOD || renamedObject == RenamingType.FIELD) {
            return null;
        }
        return currentMethod;
    }

    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            if (renamedObject != null) {
                // The renamed object has already been found; hence existing
                return;
            }

            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        currentClass = classDecl.name();

        for (var fieldDecl : classDecl.fields()) {
            if (!isMethod && fieldDecl.lineNumber == lineNumber && fieldDecl.name().equals(originalName)) {
                renamedObject = RenamingType.FIELD;
                return;
            }

        }
        for (var methodDecl : classDecl.methoddecls()) {
            if (isMethod && methodDecl.lineNumber == lineNumber && methodDecl.name().equals(originalName)) {
                renamedObject = RenamingType.METHOD;
                return;
            }

            methodDecl.accept(this);

            if (renamedObject != null) {
                // The renamed object has already been found; hence existing
                return;
            }
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        currentClass = mainClass.name();
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        currentMethod = methodDecl.name();

        for (var formal : methodDecl.formals()) {
            if (!isMethod && formal.lineNumber == lineNumber && formal.name().equals(originalName)) {
                renamedObject = RenamingType.FORMAL_VARIABLE;
                return;
            }
        }

        for (var varDecl : methodDecl.vardecls()) {
            if (!isMethod && varDecl.lineNumber == lineNumber && varDecl.name().equals(originalName)) {
                renamedObject = RenamingType.LOCAL_VARIABLE;
                return;
            }
        }
    }

    @Override
    public void visit(FormalArg formalArg) {}

    @Override
    public void visit(VarDecl varDecl) {}

    @Override
    public void visit(BlockStatement blockStatement) {}

    @Override
    public void visit(IfStatement ifStatement) {}

    @Override
    public void visit(WhileStatement whileStatement) {}

    @Override
    public void visit(SysoutStatement sysoutStatement) {}

    @Override
    public void visit(AssignStatement assignStatement) {}

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {}

    @Override
    public void visit(AndExpr e) {}

    @Override
    public void visit(LtExpr e) {}

    @Override
    public void visit(AddExpr e) {}

    @Override
    public void visit(SubtractExpr e) {}

    @Override
    public void visit(MultExpr e) {}

    @Override
    public void visit(ArrayAccessExpr e) {}

    @Override
    public void visit(ArrayLengthExpr e) {}

    @Override
    public void visit(MethodCallExpr e) {}

    @Override
    public void visit(IntegerLiteralExpr e) {}

    @Override
    public void visit(TrueExpr e) {}

    @Override
    public void visit(FalseExpr e) {}

    @Override
    public void visit(IdentifierExpr e) {}

    public void visit(ThisExpr e) {}

    @Override
    public void visit(NewIntArrayExpr e) {}

    @Override
    public void visit(NewObjectExpr e) {}

    @Override
    public void visit(NotExpr e) {}

    @Override
    public void visit(IntAstType t) {}

    @Override
    public void visit(BoolAstType t) {}

    @Override
    public void visit(IntArrayAstType t) {}

    @Override
    public void visit(RefType t) {}
}

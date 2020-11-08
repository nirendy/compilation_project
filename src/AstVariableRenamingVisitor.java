import ast.*;

public class AstVariableRenamingVisitor implements Visitor {

    String variableName;
    String newVariableName;

    public AstVariableRenamingVisitor(String variableName, String newVariableName) {
        this.variableName = variableName;
        this.newVariableName = newVariableName;
    }

    @Override
    public void visit(Program program) {}

    @Override
    public void visit(ClassDecl classDecl) {}

    @Override
    public void visit(MainClass mainClass) {}

    @Override
    public void visit(MethodDecl methodDecl) {
        /* We assume that the original variable declaration has already been renamed. Thus, if we encounter another
        * declaration of the same name, it means it has been redeclared and the process should terminate. */
        for (var varDecl : methodDecl.vardecls()) {
            if (varDecl.name().equals(variableName)) {
                return;
            }
        }

        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        methodDecl.ret().accept(this);
    }

    @Override
    public void visit(FormalArg formalArg) {}

    @Override
    public void visit(VarDecl varDecl) {}

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ifStatement.elsecase().accept(this);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        if (assignStatement.lv().equals(variableName)) {
            // Renaming the left variable of the assignment
            assignStatement.setLv(newVariableName);
        }
        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        if (assignArrayStatement.lv().equals(variableName)) {
            // Renaming the left variable of the assignment
            assignArrayStatement.setLv(newVariableName);
        }

        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
    }

    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e);
    }

    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e);
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e);
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e);
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e);
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        e.indexExpr().accept(this);
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);
    }

    @Override
    public void visit(MethodCallExpr e) {
        e.ownerExpr().accept(this);

        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(IntegerLiteralExpr e) {}

    @Override
    public void visit(TrueExpr e) {}

    @Override
    public void visit(FalseExpr e) {}

    @Override
    public void visit(IdentifierExpr e) {
        if (e.id().equals(variableName)) {
            // Renaming a reference of the variable
            e.setId(newVariableName);
        }
    }

    public void visit(ThisExpr e) {}

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {}

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {}

    @Override
    public void visit(BoolAstType t) {}

    @Override
    public void visit(IntArrayAstType t) {}

    @Override
    public void visit(RefType t) {}

    private void visitBinaryExpr(BinaryExpr e) {
        e.e1().accept(this);
        e.e2().accept(this);
    }

}

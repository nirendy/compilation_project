import ast.*;

import java.util.ArrayList;

public class AstFieldRenamingVisitor implements Visitor {

    ArrayList<String> relevantClasses;
    String fieldName;
    String newFieldName;
    ProgramUtils utils;

    // This parameters is used to store the formal args and declared variables of the context method
    private ArrayList<String> contextMethodVariables;

    public AstFieldRenamingVisitor(ArrayList<String> relevantClasses, String fieldName, String newFieldName) {
        this.relevantClasses = relevantClasses;
        this.fieldName = fieldName;
        this.newFieldName = newFieldName;
    }

    @Override
    public void visit(Program program) {
        utils = new ProgramUtils(program);

        program.mainClass().accept(this);

        for (ClassDecl classDecl : program.classDecls()) {
            if (relevantClasses.contains(classDecl.name())) {
                // If the class is not in the relevant classes (i.e. an in-class reference of the field should be
                // renamed), it shouldn't be processed, since either:
                // 1. The class inherits the renamed field's base class, but shadows it with another value - hence it
                //    shouldn't be renamed
                // 2. The class doesn't inherit from the field's base class, hence it isn't allowed to access its field
                //    (according to Mini-java limitations, all fields are of type 'protected')
                classDecl.accept(this);
            }
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        // Fields of inheriting class are shadowing and not overriding, hence they don't require renaming in any scenario

        for (MethodDecl methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {

    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // Creating a list of the variables which are used locally in the method (and might shadow class fields)
        contextMethodVariables = new ArrayList<>();

        for (FormalArg formalArg : methodDecl.formals()) {
            contextMethodVariables.add(formalArg.name());
        }

        for (VarDecl varDecl : methodDecl.vardecls()) {
            contextMethodVariables.add(varDecl.name());
        }

        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        methodDecl.ret().accept(this);
    }

    @Override
    public void visit(FormalArg formalArg) {

    }

    @Override
    public void visit(VarDecl varDecl) {

    }

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
        if (assignStatement.lv().equals(fieldName) && !contextMethodVariables.contains(assignStatement.lv())) {
            // The field name is set, and it's not in a local-method scope (i.e. it hasn't been shadowed by local
            // or formal variables), hence we rename the reference
            assignStatement.setLv(newFieldName);
        }

        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        if (assignArrayStatement.lv().equals(fieldName) && !contextMethodVariables.contains(assignArrayStatement.lv())) {
            // The field name is referenced, and it's not in a local-method scope (i.e. it hasn't been shadowed by local
            // or formal variables), hence we rename the reference
            assignArrayStatement.setLv(newFieldName);
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
    public void visit(IntegerLiteralExpr e) {

    }

    @Override
    public void visit(TrueExpr e) {

    }

    @Override
    public void visit(FalseExpr e) {

    }

    @Override
    public void visit(IdentifierExpr e) {
        if (e.id().equals(fieldName) && !contextMethodVariables.contains(e.id())) {
            // The field name is referenced, and it's not in a local-method scope (i.e. it hasn't been shadowed by local
            // or formal variables), hence we rename the reference
            e.setId(newFieldName);
        }
    }

    @Override
    public void visit(ThisExpr e) {

    }

    @Override
    public void visit(NewIntArrayExpr e) {

    }

    @Override
    public void visit(NewObjectExpr e) {

    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {

    }

    @Override
    public void visit(BoolAstType t) {

    }

    @Override
    public void visit(IntArrayAstType t) {

    }

    @Override
    public void visit(RefType t) {

    }

    private void visitBinaryExpr(BinaryExpr e) {
        e.e1().accept(this);
        e.e2().accept(this);
    }
}

import ast.*;

import java.util.ArrayList;
import java.util.HashMap;

public class AstMethodRenamingVisitor implements Visitor {

    HashMap<String, HashMap<String, String>> typesOfFields;
    ArrayList<String> relevantClasses;
    String methodName;
    String newMethodName;
    ProgramUtils utils;

    private String contextClassName;
    // These parameters are used to store the types of fields in the current visited class / method scope
    private HashMap<String, String> contextClassFields;
    private HashMap<String, String> contextMethodFields; // this might get overridden, hence needs to be copied

    public AstMethodRenamingVisitor(HashMap<String, HashMap<String, String>> typesOfFields,
                                    ArrayList<String> relevantClasses, String methodName, String newMethodName) {
        this.typesOfFields = typesOfFields;
        this.relevantClasses = relevantClasses;
        this.methodName = methodName;
        this.newMethodName = newMethodName;
    }

    @Override
    public void visit(Program program) {
        utils = new ProgramUtils(program);

        program.mainClass().accept(this);

        for (ClassDecl classDecl : program.classDecls()) {
            classDecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        // Fields of inheriting class are shadowing and not overriding, hence they don't require renaming in any scenario

        // Setting the context class fields to be the pre-calculated fields for this class
        contextClassName = classDecl.name();
        contextClassFields = typesOfFields.get(classDecl.name());
        for (MethodDecl methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // Creating a (shallow) copy of the context class fields, to be overridden as the method is processed
        contextMethodFields = new HashMap<>();
        contextMethodFields.putAll(contextClassFields);

        // Setting (or overriding) the type of the method formal variables
        for (FormalArg formalArg : methodDecl.formals()) {
            contextMethodFields.put(formalArg.name(), utils.getType(formalArg.type()));
        }

        // Setting (or overriding) the type of the declared method variables
        for (VarDecl varDecl : methodDecl.vardecls()) {
            contextMethodFields.put(varDecl.name(), utils.getType(varDecl.type()));
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
        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
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
        if (e.methodId().equals(methodName)) {
            String callerClass = null;

            if (e.ownerExpr() instanceof IdentifierExpr) {
                callerClass = contextMethodFields.get(((IdentifierExpr) e.ownerExpr()).id());
            } else if (e.ownerExpr() instanceof ThisExpr) {
                callerClass = contextClassName;
            } else if (e.ownerExpr() instanceof NewObjectExpr) {
                callerClass = ((NewObjectExpr) e.ownerExpr()).classId();
            }

            // Mini-java doesn't support any other objects calling for methods
            assert (callerClass != null);

            if (relevantClasses.contains(callerClass)) {
                // The caller class is one of the classes which require method renaming, thus the called method is renamed
                e.setMethodId(newMethodName);
            }
        }

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

package ast.semantic_checks;

import ast.*;

import java.util.*;

public class AstSemanticChecksVisitor implements Visitor {
    private boolean isValid = false; // Starts at false. Making sure that the only way it can turn true is it if started at program
    private String currentClass;
    private String currentMethod;
    private String classOfCalledMethod;
    private final ObjectOrientedUtils OOUtils;
    private HashMap<String, AstType> methodVariableTypes;
    private final Stack<String> exprResults = new Stack<>();

    public AstSemanticChecksVisitor(Program program) {
        OOUtils = new ObjectOrientedUtils(program);
    }

    public String getString() {
        if (isValid) {
            return "OK\n";
        } else {
            return "ERROR\n";
        }
    }

    private void setValidity(boolean val) {
        this.isValid = val;
    }

    private void formatVTables(Program program) {
        int tableSize;

        for (ClassDecl classdecl : program.classDecls()) {
            String className = classdecl.name();
            List<ObjectOrientedUtils.MethodData> methodsData = OOUtils.getMethodsData(className);
            tableSize = methodsData.size();
            if (tableSize == 0) {
                return;
            } else if (tableSize > 1) {
            }

            for (var methodData : methodsData) {
                methodData.returnType.accept(this);
                for (var formalArgType : methodData.formalArgsTypes) {
                    formalArgType.accept(this);
                }

            }

            if (tableSize > 1) {
            }

        }
    }


    private AstType formatAssignmentLv(String lv) {
        String destRegister;
        AstType lvType;

        if (!methodVariableTypes.containsKey(lv)) {
            destRegister = lv;
            lvType = OOUtils.getFieldType(currentClass, lv);
        } else {
            destRegister = "%" + lv;
            lvType = methodVariableTypes.get(lv);
        }

        exprResults.push(destRegister);
        return lvType;
    }

    private void visitArithmeticBinaryExpr(BinaryExpr e, String op) {
        // Note: we only use this to format arithmetic operations ("+", "-" and "*")
        // so the return type is always i32.
        e.e1().accept(this);
        String value1 = exprResults.pop();
        e.e2().accept(this);
        String value2 = exprResults.pop();
        // String resultReg = nextAnonymousReg();
        // exprResults.push(resultReg);
    }

    @Override
    public void visit(Program program) {
        formatVTables(program);

        // the only place that sets isValid to true
        this.setValidity(true);

        program.mainClass().accept(this);
        // TODO: 3: making sure the same name cannot be used for 2 classes (including main)
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }


    @Override
    public void visit(ClassDecl classDecl) {
        // TODO: 1: class do not extends itself (circulary)

        currentClass = classDecl.name();


        // TODO: 5: the same method can not be in the same class
        // TODO: 6: if a methid is overrided - same number of args, same satic types, a covariant static return type)
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
        // TODO: 4: same field do not used twice (including sub_classes)
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        // TODO: 2: make sure cannot be extended
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // TODO: 18: make sure that the return type is correct
        // TODO: 24: make sure no variable redeclaration (for formals and for locals)

        currentMethod = methodDecl.name();
        methodVariableTypes = new HashMap<>();

        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        methodDecl.ret();
        methodDecl.returnType();
    }

    @Override
    public void visit(FormalArg formalArg) {
        // TODO: 8: reference type must be declared in the file

        methodVariableTypes.put(formalArg.name(), formalArg.type());
        formalArg.type().accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        // TODO: 8: reference type must be declared in the file
        methodVariableTypes.put(varDecl.name(), varDecl.type());
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {

        // TODO: 15:: manage whether inializaion in every branch
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 17: make sure the expression is bool

        // calculate condition
        ifStatement.cond().accept(this);
        String condValue = exprResults.pop();

        // then case
        ifStatement.thencase().accept(this);

        // else case
        ifStatement.elsecase().accept(this);

    }

    @Override
    public void visit(WhileStatement whileStatement) {
        // TODO: 15:: manage whether inializaion in every branch
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 17: make sure the expression is bool

        // calculate condition
        whileStatement.cond().accept(this);
        String condValue = exprResults.pop();

        // while body
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        // TODO: 20: make sure that the arg is int
        sysoutStatement.arg().accept(this);
        exprResults.pop();
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        // TODO: 16: make sure that the assignmnet of rv is valid according to lv
        assignStatement.rv().accept(this);
        String rv = exprResults.pop();

        // TODO: 15: mark lv as assigned (unless in a branch)
        // TODO: 21: make sure that the args of the expression in the correct type

        AstType lvType = formatAssignmentLv(assignStatement.lv());
        if (!methodVariableTypes.containsKey(assignStatement.lv())) {
            lvType = OOUtils.getFieldType(currentClass, assignStatement.lv());
        } else {
            lvType = methodVariableTypes.get(assignStatement.lv());
        }

        String destRegister = exprResults.pop();

    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        // TODO: 15:: make sure the array is initialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 23: make sure that the array is int[] and the index is int and the value is int
        AstType lvType = formatAssignmentLv(assignArrayStatement.lv());
        String arrayPtrReg = exprResults.pop();

        assignArrayStatement.index().accept(this);
        String index = exprResults.pop();

        // load the address to the array

        // get pointer to array element
        String elementPtrReg = exprResults.pop();

        assignArrayStatement.rv().accept(this);
        String rv = exprResults.pop();

    }

    // private void visitArithmeticBinaryExpr(BinaryExpr e, String op) {
    //     // Note: we only use this to format arithmetic operations ("+", "-" and "*")
    //     // so the return type is always i32.
    //     e.e1().accept(this);
    //     String value1 = exprResults.pop();
    //     e.e2().accept(this);
    //     String value2 = exprResults.pop();
    //     String resultReg = nextAnonymousReg();
    //     formatIndented("%s = %s i32 %s, %s\n", resultReg, op, value1, value2);
    //     exprResults.push(resultReg);
    // }

    @Override
    public void visit(AndExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type

        // calculate first expression
        e.e1().accept(this);
        String value1 = exprResults.pop();

        // check first expression's result

        // calculate second expression's result
        e.e2().accept(this);
        String value2 = exprResults.pop();

        // jump to the result calculation

        // calculate result (based on the label that we got here from)
        // exprResults.push();
    }

    @Override
    public void visit(LtExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        e.e1().accept(this);
        String value1 = exprResults.pop();
        e.e2().accept(this);
        String value2 = exprResults.pop();
        // exprResults.push(resultReg);
    }

    @Override
    public void visit(NotExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        e.e().accept(this);
        exprResults.pop();
        // exprResults.push(resultReg);
    }

    @Override
    public void visit(AddExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitArithmeticBinaryExpr(e, "add");
    }

    @Override
    public void visit(SubtractExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitArithmeticBinaryExpr(e, "sub");
    }

    @Override
    public void visit(MultExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitArithmeticBinaryExpr(e, "mul");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 22: make sure that the array is int[] and the index is int
        e.arrayExpr().accept(this);
        String arrayPtrReg = exprResults.pop();

        e.indexExpr().accept(this);
        String index = exprResults.pop();

        String elementPtrReg = exprResults.pop();
        // load element to get the value

        // exprResults.push(elementValueReg);
    }


    @Override
    public void visit(ArrayLengthExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 13: static type of which it invokes is int[]

        e.arrayExpr().accept(this);
        String elementPtrReg = exprResults.pop();

        // load element pointer to get the value
        // String elementValueReg = nextAnonymousReg();
        // formatIndented("%s = load i32, i32* %s\n", elementValueReg, elementPtrReg);
        //
        // exprResults.push(elementValueReg);
    }


    @Override
    public void visit(MethodCallExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 10: must be called from ref type
        // TODO: 11: not fully understand make sure tat the
        // TODO: 12: must be called from this, new, local var, formal or field
        // TODO: 18: make sure that the return type is correct

        e.ownerExpr().accept(this);
        String ownerReg = exprResults.pop();

        // bitcast to pointer to the vtable
        // String vtablePtrReg = nextAnonymousReg();
        // formatIndented("%s = bitcast i8* %s to i8***\n", vtablePtrReg, ownerReg);

        // load the vtable
        // String vtableReg = nextAnonymousReg();
        // formatIndented("%s = load i8**, i8*** %s\n", vtableReg, vtablePtrReg);

        // get a pointer to the method's element in the vtable
        // String methodElementReg = nextAnonymousReg();
        int methodIndex = OOUtils.getMethodIndex(classOfCalledMethod, e.methodId());
        // formatIndented("%s = getelementptr i8*, i8** %s, i32 %d\n", methodElementReg, vtableReg, methodIndex);

        // load element to get the pointer to the method
        // String tempMethodPtrReg = nextAnonymousReg();
        // formatIndented("%s = load i8*, i8** %s\n", tempMethodPtrReg, methodElementReg);

        AstType returnType = OOUtils.getMethodReturnType(classOfCalledMethod, e.methodId());
        List<AstType> formalArgsTypes = OOUtils.getMethodFormalArgsTypes(classOfCalledMethod, e.methodId());

        // make method call

        for (var actual : e.actuals()) {
            actual.accept(this);
            // actualValues.add(exprResults.pop());
        }

        // e.ownerExpr();

        // e.methodId();

    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        exprResults.push(Integer.toString(e.num()));
    }

    @Override
    public void visit(TrueExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        exprResults.push("1");
    }

    @Override
    public void visit(FalseExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        exprResults.push("0");
    }

    @Override
    public void visit(IdentifierExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        String identifierReg;
        AstType type;
        if (!methodVariableTypes.containsKey(e.id())) {
            type = OOUtils.getFieldType(currentClass, e.id());
        } else {
            identifierReg = "%" + e.id();
            type = methodVariableTypes.get(e.id());
        }

        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        if (type instanceof RefType) {
            classOfCalledMethod = ((RefType) type).id();
        }

        // Load value referenced by the identifierReg
        // exprResults.push(valueReg);

    }

    public void visit(ThisExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        classOfCalledMethod = currentClass;

        exprResults.push("%this");
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        e.lengthExpr().accept(this);
        String arrayLength = exprResults.pop();

        // format array length validation
        e.lengthExpr();

        // calculate array size

        // allocate new array, and store arrayLen in the first cell
    }

    @Override
    public void visit(NewObjectExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 9: new must be declared in the file

        e.classId();

        OOUtils.getInstanceSize(e.classId());
        int numOfMethods = OOUtils.getNumberOfMethods(e.classId());
        // numOfMethods

        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        classOfCalledMethod = e.classId();

        // exprResults.push(instanceReg);
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
        // TODO: 8: reference type must be declared in the file
    }
}

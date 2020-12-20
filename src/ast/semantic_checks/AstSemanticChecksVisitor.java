package ast.semantic_checks;

import ast.*;


import java.util.*;

public class AstSemanticChecksVisitor implements Visitor {
    private boolean isValid = false; // Starts at false. Making sure that the only way it can turn true is it if started at program
    private String currentClass;
    private String mainClassName;
    private String currentMethod;
    private String classOfCalledMethod;
    private final ObjectOrientedUtils OOUtils;
    private final SemanticChecksUtils SCUtils;
    private HashMap<String, AstType> methodVariableTypes;
    private final Stack<AstType> typesStack = new Stack<>();
    Map<String, Set<String>> classToSuperClasses;

    public AstSemanticChecksVisitor(Program program) {
        OOUtils = new ObjectOrientedUtils(program);
        SCUtils = new SemanticChecksUtils();
    }

    public String getString() {
        if (isValid) {
            return "OK\n";
        } else {
            return "ERROR\n";
        }
    }

    private void setInvalid(String reason) {
        this.isValid = false;
        System.out.println(reason);
    }
    

    @Override
    public void visit(Program program) {

        // the only place that sets isValid to true
        this.isValid = true;

        program.mainClass().accept(this);


        // TODO: 3: making sure the same name cannot be used for 2 classes (including main)
        HashSet<String> classNames = new HashSet<>();
        this.mainClassName = program.mainClass().name();

        classNames.add(this.mainClassName);
        for (ClassDecl classDecl : program.classDecls()) {
            String className = classDecl.name();
            if (classNames.contains(className)) {
                setInvalid(String.format("Class Name %s declared more than once", className));
                return;
            } else {
                classNames.add(className);
            }

            classDecl.accept(this);
        }
    }

    private boolean isSubClass(String subClassName, String parentClassName) {
        return this.classToSuperClasses.get(subClassName).contains(parentClassName);
    }

    private void updateClassToSuperClasses(ClassDecl classDecl) {
        if (classDecl.superName() != null) {
            if (!this.classToSuperClasses.containsKey(classDecl.superName())) {
                setInvalid(String.format("Superclass '%s' must be declared before extending class '%s'",
                        classDecl.superName(), classDecl.name()));
                return;
            }

            HashSet<String> superClasses = new HashSet<>(this.classToSuperClasses.get(classDecl.superName()));
            if (superClasses.contains(classDecl.name())) {
                setInvalid(String.format("Class '%s' extends itself", classDecl.name()));
                return;
            }

            this.classToSuperClasses.put(classDecl.name(), superClasses);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        // TODO: 1: class do not extends itself (circularly)
        updateClassToSuperClasses(classDecl);
        if (!this.isValid) {
            return;
        }

        // TODO: 2: make sure main class cannot be extended
        if (classDecl.superName().equals(this.mainClassName)) {
            setInvalid(String.format("The main class (class '%s') cannot be extended", this.mainClassName));
            return;
        }

        currentClass = classDecl.name();

        // TODO: 6: if a method is overridden - same number of args, same static types, a covariant static return type)
        // TODO: 5: the same method can not be in the same class
        HashSet<String> methodNames = new HashSet<>();
        for (var methodDecl : classDecl.methoddecls()) {
            String methodName = methodDecl.name();
            if (classDecl.superName() != null && OOUtils.hasMethod(classDecl.superName(), methodName)) {
                // Check same number of args
                List<AstType> superFormalArgsTypes = OOUtils.getMethodFormalArgsTypes(classDecl.superName(), methodName);
                if (superFormalArgsTypes.size() != methodDecl.formals().size()) {
                    setInvalid(String.format("Overriding method %s in class %s with the wrong number of formal arguments",
                            methodName, classDecl.name()));
                    return;
                }

                // Check same static types
                for (int i = 0; i < superFormalArgsTypes.size(); i++) {
                    AstType superFormalArgType = superFormalArgsTypes.get(i);
                    AstType formalArgType = methodDecl.formals().get(i).type();
                    if (superFormalArgType.getClass() != formalArgType.getClass()) {
                        setInvalid(String.format("Overriding method %s in class %s with the wrong formal arguments types",
                                methodName, classDecl.name()));
                        return;
                    } else if (superFormalArgType instanceof RefType) {
                        if (!(((RefType) superFormalArgType).id().equals(((RefType) formalArgType).id()))) {
                            setInvalid(String.format("Overriding method %s in class %s with the wrong formal arguments types",
                                    methodName, classDecl.name()));
                            return;
                        }
                    }
                }

                // Check covariant static return type
                AstType superReturnType = OOUtils.getMethodReturnType(classDecl.superName(), methodName);
                if (methodDecl.returnType().getClass() != superReturnType.getClass()) {
                    setInvalid(String.format("Overriding method %s in class %s with the wrong return type",
                            methodName, classDecl.name()));
                    return;
                } else if (superReturnType instanceof RefType) {
                    if (!isSubClass(((RefType) methodDecl.returnType()).id(), ((RefType) superReturnType).id())) {
                        setInvalid(String.format("Overriding method %s in class %s with the wrong return type",
                                methodName, classDecl.name()));
                        return;
                    }
                }
            }

            if (methodNames.contains(methodName)) {
                setInvalid(String.format("Method name %s declared more than once in class %s", methodName, classDecl.name()));
                return;
            } else {
                methodNames.add(methodName);
            }

            methodDecl.accept(this);
        }

        // TODO: 4: same field is not used twice (including sub_classes)
        // TODO (including sub_classes)
        HashSet<String> fieldNames = new HashSet<>();
        for (var fieldDecl : classDecl.fields()) {
            String fieldName = fieldDecl.name();

            if (classDecl.superName() != null && OOUtils.hasField(classDecl.superName(), fieldName)) {
                setInvalid(String.format("Field name %s re-declared in class %s", fieldName, classDecl.name()));
                return;
            } else if (fieldNames.contains(fieldName)) {
                setInvalid(String.format("Field name %s declared more than once in class %s", fieldName, classDecl.name()));
                return;
            } else {
                fieldNames.add(fieldName);
            }

            fieldDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        currentMethod = methodDecl.name();
        methodVariableTypes = new HashMap<>();

        // TODO: 24: make sure no variable redeclaration (for formals and for locals)
        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        // TODO: 18: make sure that the return type is correct
        methodDecl.ret().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType retExpType = typesStack.pop();

        if (methodDecl.returnType() instanceof RefType) {
            if (!(retExpType instanceof RefType)) {
                setInvalid("The static type of e in 'return e' must match the method's return type");
                return;
            } else if (!isSubClass(((RefType) retExpType).id(), ((RefType) methodDecl.returnType()).id())) {
                setInvalid("The static type of e in 'return e' is not a subtype of the method's return type");
                return;
            }
        } else {
            if (retExpType.getClass() != methodDecl.returnType().getClass()) {
                setInvalid("The static type of e in 'return e' must match the method's return type");
                return;
            }
        }
    }

    @Override
    public void visit(FormalArg formalArg) {
        // TODO: 24: make sure no variable redeclaration (for formals and for locals)
        if (methodVariableTypes.containsKey(formalArg.name())) {
            setInvalid(String.format("Formal arg %s declared more than once in method %s of class %s", formalArg.name(),
                    this.currentMethod, this.currentClass));
            return;
        }

        // TODO: 8: reference type must be declared in the file
        formalArg.type().accept(this);
        if (!this.isValid) {
            return;
        }

        methodVariableTypes.put(formalArg.name(), typesStack.pop());
    }

    @Override
    public void visit(VarDecl varDecl) {
        // TODO: 24: make sure no variable redeclaration (for formals and for locals)
        if (methodVariableTypes.containsKey(varDecl.name())) {
            setInvalid(String.format("Local variable %s re-declared in method %s of class %s", varDecl.name(),
                    this.currentMethod, this.currentClass));
            return;
        }

        // TODO: 8: reference type must be declared in the file
        varDecl.type().accept(this);
        if (!this.isValid) {
            return;
        }

        methodVariableTypes.put(varDecl.name(), typesStack.pop());
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
            if (!this.isValid) {
                return;
            }
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {

        // TODO: 15: manage whether initialization in every branch
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 17: make sure the expression is bool

        ifStatement.cond().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type = typesStack.pop();
        if (!(type instanceof BoolAstType)) {
            setInvalid("If statement got non-boolean argument for the condition");
            return;
        }

        // then case
        ifStatement.thencase().accept(this);
        if (!this.isValid) {
            return;
        }

        // else case
        ifStatement.elsecase().accept(this);
        if (!this.isValid) {
            return;
        }
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        // TODO: 15: manage whether inializaion in every branch
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 17: make sure the expression is bool

        whileStatement.cond().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type = typesStack.pop();
        if (!(type instanceof BoolAstType)) {
            setInvalid("While statement got non-boolean argument for the condition");
            return;
        }

        // while body
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        // TODO: 20: make sure that the arg is int
        sysoutStatement.arg().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type = typesStack.pop();
        if (!(type instanceof IntAstType)) {
            setInvalid("Sysout statement got non-numeric argument");
            return;
        }
    }

    private void visitAssignmentLv(String lv) {
        if (methodVariableTypes.containsKey(lv)) {
            typesStack.push(methodVariableTypes.get(lv));
        } else if (OOUtils.hasField(currentClass, lv)) {  // is a field
            typesStack.push(OOUtils.getFieldType(currentClass, lv));
        } else {
            setInvalid(String.format("Reference to undefined name '%s'", lv));
            return;
        }
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        // TODO: 16: make sure that the assignment of rv is valid according to lv
        // TODO: 15: mark lv as assigned (unless in a branch)
        // TODO: 21: make sure that the args of the expression in the correct type
        visitAssignmentLv(assignStatement.lv());
        if (!this.isValid) {
            return;
        }

        AstType lvType = typesStack.pop();
        assignStatement.rv().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType rvType = typesStack.pop();

        if (lvType instanceof RefType) {
            if (!(rvType instanceof RefType)) {
                setInvalid("Assignment (x = a) statement got non-matching types (one is not a subtype of the other)");
                return;
            } else if (!isSubClass(((RefType) rvType).id(), ((RefType) lvType).id())) {
                setInvalid("Assignment (x = a) statement got non-matching types (one is not a subtype of the other)");
                return;
            }
        } else {
            if (rvType.getClass() != lvType.getClass()) {
                setInvalid("Assignment (x = a) statement got non-matching types");
                return;
            }
        }
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        // TODO: 15: make sure the array is initialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 23: make sure that the array is int[] and the index is int and the value is int
        visitAssignmentLv(assignArrayStatement.lv());
        if (!this.isValid) {
            return;
        }

        AstType lvType = typesStack.pop();
        if (!(lvType instanceof IntArrayAstType)) {
            setInvalid("Array assignment (x[e1] = e2) statement got a non-array argument (as x)");
            return;
        }

        assignArrayStatement.index().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType indexType = typesStack.pop();
        if (!(indexType instanceof IntAstType)) {
            setInvalid("Array assignment (x[e1] = e2) statement got a non-numeric argument (as e1)");
            return;
        }

        assignArrayStatement.rv().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType rvType = typesStack.pop();
        if (!(rvType instanceof IntAstType)) {
            setInvalid("Array assignment (x[e1] = e2) statement got a non-numeric argument (as e2)");
            return;
        }
    }

    private void visitBooleanBinaryExpr(BinaryExpr e, String op) {
        // Note: we only use this to check boolean operations ("&&" and ">")
        // so the argument types are always boolean.
        e.e1().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type1 = typesStack.pop();
        e.e2().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type2 = typesStack.pop();

        if (!(type1 instanceof BoolAstType) || !(type2 instanceof BoolAstType)) {
            setInvalid(String.format("%s op got non-boolean arguments", op));
            return;
        }
    }

    @Override
    public void visit(AndExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitBooleanBinaryExpr(e, "And (&&)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(LtExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitBooleanBinaryExpr(e, "Lt (<)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(NotExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        e.e().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type = typesStack.pop();
        if (!(type instanceof BoolAstType)) {
            setInvalid("Not (!) op got non-boolean argument");
            return;
        }

        typesStack.push(new BoolAstType());
    }

    private void visitArithmeticBinaryExpr(BinaryExpr e, String op) {
        // Note: we only use this to check arithmetic operations ("+", "-" and "*")
        // so the argument types are always int.
        e.e1().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type1 = typesStack.pop();
        e.e2().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType type2 = typesStack.pop();

        if (!(type1 instanceof IntAstType) || !(type2 instanceof IntAstType)) {
            setInvalid(String.format("%s op got non-numeric arguments", op));
            return;
        }
    }

    @Override
    public void visit(AddExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitArithmeticBinaryExpr(e, "Add (+)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(SubtractExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitArithmeticBinaryExpr(e, "Subtract (-)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(MultExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        visitArithmeticBinaryExpr(e, "Mult (*)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 22: make sure that the array is int[] and the index is int
        e.arrayExpr().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType arrayType = typesStack.pop();
        if (!(arrayType instanceof IntArrayAstType)) {
            setInvalid("Array access (x[e]) op got a non-array argument (for x in the x[e])");
            return;
        }

        e.indexExpr().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType indexType = typesStack.pop();
        if (!(indexType instanceof IntAstType)) {
            setInvalid("Array access (x[e]) op got a non-numeric argument (for e in the x[e])");
            return;
        }

        typesStack.push(new IntAstType());
    }


    @Override
    public void visit(ArrayLengthExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 13: static type of which it invokes is int[]
        e.arrayExpr().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType arrayType = typesStack.pop();
        if (!(arrayType instanceof IntArrayAstType)) {
            setInvalid("Array length (x.length) op got a non-array argument");
            return;
        }

        typesStack.push(new IntAstType());
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

        typesStack.push(OOUtils.getMethodReturnType(classOfCalledMethod, e.methodId()));
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(TrueExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(FalseExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(IdentifierExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current 
        //  method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        AstType type;
        if (methodVariableTypes.containsKey(e.id())) {
            type = methodVariableTypes.get(e.id());
        } else if (OOUtils.hasField(currentClass, e.id())) {  // is a field
            type = OOUtils.getFieldType(currentClass, e.id());
        } else {
            setInvalid(String.format("Reference to undefined name '%s'", e.id()));
            return;
        }

        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        if (type instanceof RefType) {
            classOfCalledMethod = ((RefType) type).id();
        }

        typesStack.push(type);
    }

    public void visit(ThisExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        classOfCalledMethod = currentClass;

        typesStack.push(new RefType(currentClass));
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        e.lengthExpr().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType lengthType = typesStack.pop();
        if (!(lengthType instanceof IntAstType)) {
            setInvalid("New array op got a non-numeric argument for the array length");
            return;
        }

        typesStack.push(new IntArrayAstType());
    }

    @Override
    public void visit(NewObjectExpr e) {
        // TODO: 14: make sure that all the varialbe access is to a local variable, formal param defined in the current method or to a field defined in the same class of supercalss
        // TODO: 15: make sure use of variables are already initlialized
        // TODO: 21: make sure that the args of the expression in the correct type
        // TODO: 9: new must be declared in the file
        if (!OOUtils.hasClass(e.classId())) {
            setInvalid((String.format("New object type '%s' was not declared in the program", e.classId())));
            return;
        }

        typesStack.push(new RefType(e.classId()));
        
        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        classOfCalledMethod = e.classId();
    }

    @Override
    public void visit(IntAstType t) {
        typesStack.push(t);
    }

    @Override
    public void visit(BoolAstType t) {
        typesStack.push(t);
    }

    @Override
    public void visit(IntArrayAstType t) {
        typesStack.push(t);
    }

    @Override
    public void visit(RefType t) {
        // TODO: 8: reference type must be declared in the file
        if (!OOUtils.hasClass(t.id())) {
            setInvalid((String.format("Reference type '%s' was not declared in the program", t.id())));
            return;
        }
        typesStack.push(t);
    }
}

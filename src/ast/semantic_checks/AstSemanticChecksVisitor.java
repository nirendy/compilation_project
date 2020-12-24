package ast.semantic_checks;

import ast.*;


import java.util.*;

public class AstSemanticChecksVisitor implements Visitor {
    private boolean isValid = true;
    private String currentClass;
    private String mainClassName;
    private String currentMethod;
    private String lastVisitedClassType;
    private final ObjectOrientedUtils OOUtils;
    private final SemanticChecksUtils SCUtils;
    private HashMap<String, AstType> methodVariableTypes = new HashMap<>();;
    private HashMap<String, InitializationState> methodVariablesInitializationStates = new HashMap<>();
    private final Stack<AstType> typesStack = new Stack<>();
    Map<String, Set<String>> classToSuperClasses = new HashMap<>();

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
    
    private boolean notSubClass(String subClassName, String parentClassName) {
        if (subClassName.equals(parentClassName)) {
            return false;
        }
        return !this.classToSuperClasses.get(subClassName).contains(parentClassName);
    }

    private void updateClassToSuperClasses(ClassDecl classDecl) {
        if (classDecl.superName() == null) {
            this.classToSuperClasses.put(classDecl.name(), new HashSet<>());
            return;
        }

        if (!this.classToSuperClasses.containsKey(classDecl.superName())) {
            if (classDecl.superName().equals(classDecl.name())) {  // 1: Class does not extend itself (directly)
                setInvalid(String.format("Class '%s' extends itself", classDecl.name()));
            } else { // 1: Class is declared after the class it's extending
                setInvalid(String.format("Superclass '%s' must be declared before extending class '%s'",
                        classDecl.superName(), classDecl.name()));
            }
            return;
        }

        // 1: Class does not extend itself (indirectly)
        if (this.classToSuperClasses.get(classDecl.superName()).contains(classDecl.name())) {
            setInvalid(String.format("Class '%s' (indirectly) extends itself", classDecl.name()));
            return;
        }

        HashSet<String> superClasses = new HashSet<>(this.classToSuperClasses.get(classDecl.superName()));
        superClasses.add(classDecl.superName());
        this.classToSuperClasses.put(classDecl.name(), superClasses);
    }

    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        if (!this.isValid) {
            return;
        }

        // 3: Making sure the same name cannot be used for 2 classes (including main)
        HashSet<String> classNames = new HashSet<>();
        this.mainClassName = program.mainClass().name();

        for (ClassDecl classDecl : program.classDecls()) {
            updateClassToSuperClasses(classDecl);
            if (!this.isValid) {
                return;
            }
        }

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
            if (!this.isValid) {
                return;
            }
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        // 2: Make sure the main class is not extended
        if (classDecl.superName() != null && classDecl.superName().equals(this.mainClassName)) {
            setInvalid(String.format("The main class (class '%s') cannot be extended", this.mainClassName));
            return;
        }

        currentClass = classDecl.name();

        // 6: Method overriding is done correctly
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
                    if (notSubClass(((RefType) methodDecl.returnType()).id(), ((RefType) superReturnType).id())) {
                        setInvalid(String.format("Overriding method %s in class %s with the wrong return type",
                                methodName, classDecl.name()));
                        return;
                    }
                }
            }

            // 5: Two methods with the same name can't be defined for the same class (no overloading)
            if (methodNames.contains(methodName)) {
                setInvalid(String.format("Method name %s declared more than once in class %s", methodName, classDecl.name()));
                return;
            } else {
                methodNames.add(methodName);
            }

            methodDecl.accept(this);
            if (!this.isValid) {
                return;
            }
        }

        // 4: The same name cannot be used for the same field in one class (including subclasses)
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

            // 8: Reference type must be declared in the file
            fieldDecl.type().accept(this);
            if (!this.isValid) {
                return;
            }
            typesStack.pop();
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        classToSuperClasses.put(mainClass.name(), new HashSet<>());
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        currentMethod = methodDecl.name();
        methodVariableTypes = new HashMap<>();
        methodVariablesInitializationStates = new HashMap<>();

        for (var formal : methodDecl.formals()) {
            formal.accept(this);
            if (!this.isValid) {
                return;
            }
        }
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
            if (!this.isValid) {
                return;
            }
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
            if (!this.isValid) {
                return;
            }
        }

        // 18: The static type of "e" in "return e" is valid according to the definition of the current method.
        methodDecl.ret().accept(this);
        if (!this.isValid) {
            return;
        }

        AstType retExpType = typesStack.pop();

        if (retExpType.getClass() != methodDecl.returnType().getClass()) {
            setInvalid(String.format("In method %s, the static type of e in 'return e' must match the method's return type", methodDecl.name()));
        } else if (methodDecl.returnType() instanceof RefType) {
            if (notSubClass(((RefType) retExpType).id(), ((RefType) methodDecl.returnType()).id())) {
                setInvalid(String.format("In method %s, the static type of e in 'return e' is not a subtype of the method's return type", methodDecl.name()));
            }
        }
    }

    @Override
    public void visit(FormalArg formalArg) {
        // 24: Variable redeclaration is forbidden - the same name cannot be
        // used for declarations of two formal parameters.
        if (methodVariableTypes.containsKey(formalArg.name())) {
            setInvalid(String.format("Formal arg %s declared more than once in method %s of class %s", formalArg.name(),
                    this.currentMethod, this.currentClass));
            return;
        }

        // 8: Reference type must be declared in the file
        formalArg.type().accept(this);
        if (!this.isValid) {
            return;
        }

        methodVariableTypes.put(formalArg.name(), typesStack.pop());
    }

    @Override
    public void visit(VarDecl varDecl) {
        // 24: Variable redeclaration is forbidden - the same name cannot be
        // used for declarations of two local variables.
        if (methodVariableTypes.containsKey(varDecl.name())) {
            setInvalid(String.format("Local variable %s re-declared in method %s of class %s", varDecl.name(),
                    this.currentMethod, this.currentClass));
            return;
        }

        // 8: Reference type must be declared in the file
        varDecl.type().accept(this);
        if (!this.isValid) {
            return;
        }

        methodVariableTypes.put(varDecl.name(), typesStack.pop());
        methodVariablesInitializationStates.put(varDecl.name(), InitializationState.UNINITIALIZED);
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

    private HashMap<String, InitializationState> getMethodVariablesInitializationStatesCopy() {
        return new HashMap<>(this.methodVariablesInitializationStates);
    }

    private HashMap<String, InitializationState> joinMethodVariablesInitializationStates(
            HashMap<String, InitializationState> states1, HashMap<String, InitializationState> states2) {
        HashMap<String, InitializationState> joined = new HashMap<>();
        for (var variableName: states1.keySet()) {
            joined.put(variableName,
                    SCUtils.joinInitializationStates(states1.get(variableName), states2.get(variableName)));
        }

        return joined;
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.cond().accept(this);
        if (!this.isValid) {
            return;
        }

        // 17: The condition expression results in a boolean
        AstType type = typesStack.pop();
        if (!(type instanceof BoolAstType)) {
            setInvalid("If statement got non-boolean argument for the condition");
            return;
        }

        HashMap<String, InitializationState> originalMethodVariablesInitializationStates =
                getMethodVariablesInitializationStatesCopy();

        // then case
        ifStatement.thencase().accept(this);
        if (!this.isValid) {
            return;
        }
        HashMap<String, InitializationState> thenMethodVariablesInitializationStates =
                getMethodVariablesInitializationStatesCopy();


        this.methodVariablesInitializationStates = originalMethodVariablesInitializationStates;
        // else case
        ifStatement.elsecase().accept(this);
        if (!this.isValid) {
            return;
        }

        // 15: Every local variable is definitely initialized (assigned to) before it is used (in every branch).
        // This is later check in the IdentifierExpr.
        this.methodVariablesInitializationStates = joinMethodVariablesInitializationStates(
                thenMethodVariablesInitializationStates, this.methodVariablesInitializationStates);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.cond().accept(this);
        if (!this.isValid) {
            return;
        }

        // 17: The condition expression results in a boolean
        AstType type = typesStack.pop();
        if (!(type instanceof BoolAstType)) {
            setInvalid("While statement got non-boolean argument for the condition");
            return;
        }

        HashMap<String, InitializationState> originalMethodVariablesInitializationStates =
                getMethodVariablesInitializationStatesCopy();

        // while body
        whileStatement.body().accept(this);
        if (!this.isValid) {
            return;
        }

        // 15: Every local variable is definitely initialized (assigned to) before it is used (in every branch).
        // This is later check in the IdentifierExpr.
        this.methodVariablesInitializationStates = joinMethodVariablesInitializationStates(
                originalMethodVariablesInitializationStates, this.methodVariablesInitializationStates);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
        if (!this.isValid) {
            return;
        }

        // 17: The arg expression results in an int
        AstType type = typesStack.pop();
        if (!(type instanceof IntAstType)) {
            setInvalid("Sysout statement got non-numeric argument");
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

        if (methodVariablesInitializationStates.containsKey(lv)) {
            methodVariablesInitializationStates.put(lv, InitializationState.INITIALIZED);
        }
    }

    @Override
    public void visit(AssignStatement assignStatement) {
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

        // 16: In an assignment "x = e", the static type of "e" is valid according to the declaration of x
        if (rvType.getClass() != lvType.getClass()) {
            setInvalid("Assignment (x = a) statement got non-matching types");
        } else if (rvType instanceof RefType) {
            if (notSubClass(((RefType) rvType).id(), ((RefType) lvType).id())) {
                setInvalid("Assignment (x = a) statement got non-matching types (one is not a subtype of the other)");
            }
        }
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        visitAssignmentLv(assignArrayStatement.lv());
        if (!this.isValid) {
            return;
        }

        // 23: n an assignment to an array "x[e1] = e2", x is int[], e1 is an int and also e2 is an int
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
        }
    }

    @Override
    public void visit(AndExpr e) {
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
            setInvalid("And (&&) op got non-boolean arguments");
            return;
        }

        typesStack.push(new BoolAstType());
    }

    private void visitNumericBinaryExpr(BinaryExpr e, String op) {
        // Note: we only use this to check numeric operations ("+", "-", "*" and "<")
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
        }
    }

    @Override
    public void visit(LtExpr e) {
        visitNumericBinaryExpr(e, "Lt (<)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(NotExpr e) {
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

    @Override
    public void visit(AddExpr e) {
        visitNumericBinaryExpr(e, "Add (+)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(SubtractExpr e) {
        visitNumericBinaryExpr(e, "Subtract (-)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(MultExpr e) {
        visitNumericBinaryExpr(e, "Mult (*)");
        if (!this.isValid) {
            return;
        }

        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        // 22: In an array access x[e], x is int[] and e is an int.
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
        // 13: The static type of the object on which length invoked is int[].
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
        // 12: Must be called from this, new, or ref-type
        if (!SCUtils.isValidOwnerExpressionType(e.ownerExpr())) {
            setInvalid("Method call owner expression must be this, ref-type, or new object expression");
            return;
        }

        e.ownerExpr().accept(this);
        if (!this.isValid) {
            return;
        }

        String methodOwnerClass = lastVisitedClassType;
        // 10: In method invocation, the static type of the object is a reference type (not int, bool, or int[])
        AstType ownerType = typesStack.pop();
        if (!(ownerType instanceof RefType)) {
            setInvalid("If method call owner expression is ref-type, it must be an object (not int, int[] or bool)");
            return;
        }

        // 11: Check method exists for class
        if (!OOUtils.hasMethod(methodOwnerClass, e.methodId())) {
            setInvalid(String.format("Method %s doesn't exist in class %s", e.methodId(), methodOwnerClass));
            return;
        }

        // 11: Check number of args match between call and declaration
        List<AstType> formalArgsTypes = OOUtils.getMethodFormalArgsTypes(methodOwnerClass, e.methodId());
        if (formalArgsTypes.size() != e.actuals().size()) {
            setInvalid(String.format("Method %s of class %s called with wrong number of arguments", e.methodId(), methodOwnerClass));
            return;
        }

        // 11: Check types of args match between call and declaration
        for (int i = 0; i < formalArgsTypes.size(); i++) {
            e.actuals().get(i).accept(this);
            if (!this.isValid) {
                return;
            }

            AstType actualType = typesStack.pop();
            AstType formalArgType = formalArgsTypes.get(i);
            if (actualType.getClass() != formalArgType.getClass()) {
                setInvalid(String.format("Method %s of class %s called with wrong type of argument",
                        e.methodId(), methodOwnerClass));
                return;
            } else if (actualType instanceof RefType) {
                if (notSubClass(((RefType) actualType).id(), ((RefType) formalArgType).id())) {
                    setInvalid(String.format("Method %s of class %s called with wrong type of argument",
                        e.methodId(), methodOwnerClass));
                    return;
                }
            }
        }

        typesStack.push(OOUtils.getMethodReturnType(methodOwnerClass, e.methodId()));
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        typesStack.push(new IntAstType());
    }

    @Override
    public void visit(TrueExpr e) {
        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(FalseExpr e) {
        typesStack.push(new BoolAstType());
    }

    @Override
    public void visit(IdentifierExpr e) {
        // 14: A reference in an expression to a variable is to a local variable or formal parameter defined in the
        // current method, or to a field defined in the current class or its superclasses
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
            lastVisitedClassType = ((RefType) type).id();
        }

        // Check that local vars are initialized before use
        if (methodVariablesInitializationStates.containsKey(e.id())) {
            if (methodVariablesInitializationStates.get(e.id()) != InitializationState.INITIALIZED) {
                setInvalid(String.format("Reference to uninitialized variable '%s'", e.id()));
                return;
            }
        }
        typesStack.push(type);
    }

    public void visit(ThisExpr e) {
        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        lastVisitedClassType = currentClass;

        typesStack.push(new RefType(currentClass));
    }

    @Override
    public void visit(NewIntArrayExpr e) {
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
        // 9: new A() is invoked for a class A that is defined somewhere in the file
        if (OOUtils.classNotInProgram(e.classId())) {
            setInvalid((String.format("New object type '%s' was not declared in the program", e.classId())));
            return;
        }

        typesStack.push(new RefType(e.classId()));
        
        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        lastVisitedClassType = e.classId();
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
        // 8: Reference type must be declared in the file
        if (OOUtils.classNotInProgram(t.id())) {
            setInvalid((String.format("Reference type '%s' was not declared in the program", t.id())));
            return;
        }
        typesStack.push(t);
    }
}

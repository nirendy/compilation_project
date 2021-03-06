package ast.llvm_formatting;

import ast.*;

import java.util.*;

public class AstLLVMFormatVisitor implements Visitor {
    private final Formatter formatter = new Formatter();
    private String currentClass;
    private String currentMethod;
    private String classOfCalledMethod;
    private final LLVMObjectOrientedUtils OOUtils;
    private HashMap<String, AstType> methodVariableTypes;
    private final Stack<String> exprResults = new Stack<>();
    private int regCounter = 0;
    private int labelCounter = 0;

    public AstLLVMFormatVisitor(Program program) {
        OOUtils = new LLVMObjectOrientedUtils(program);
    }

    public String getString() {
        return formatter.toString();
    }

    private void formatIndented(String str, Object ... args) {
        formatter.format("\t");
        formatter.format(str, args);
    }

    private String nextAnonymousReg() {
        return String.format("%%_%d", regCounter++);
    }

    private int nextLabelPostfix() {
        return labelCounter++;
    }

    @Override
    public void visit(Program program) {
        formatVTables(program);
        formatter.format("%s\n\n\n", LLVMConstants.getFunctionDeclarations());

        program.mainClass().accept(this);
        formatter.format("\n");
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
            formatter.format("\n");  // this is unnecessary, but the examples have it too.
        }
    }

    private void formatVTables(Program program) {
        int tableSize;

        for (ClassDecl classdecl : program.classDecls()) {
            String className = classdecl.name();
            List<LLVMObjectOrientedUtils.MethodData> methodsData = OOUtils.getMethodsData(className);
            tableSize = methodsData.size();
            formatter.format("@.%s_vtable = global [%d x i8*] [", className, tableSize);
            if (tableSize == 0) {
                formatter.format("]\n");
                continue;
            }

            else if (tableSize > 1) {
                formatter.format("\n\t");
            }

            String sep = "";
            for (var methodData : methodsData) {
                formatter.format("%si8* bitcast (", sep);
                formatMethodSignature(methodData.returnType, methodData.formalArgsTypes);
                formatter.format(" @%s.%s to i8*)", methodData.declaringClass, methodData.methodName);
                sep = ",\n\t";
            }

            if (tableSize > 1) {
                formatter.format("\n");
            }

            formatter.format("]\n\n");
        }
    }

    private void formatMethodSignature(AstType returnType, List<AstType> formalArgsTypes) {
        returnType.accept(this);
        formatter.format(" (i8*");
        for (var formalArgType : formalArgsTypes) {
            formatter.format(", ");
            formalArgType.accept(this);
        }
        formatter.format(")*");
    }

    @Override
    public void visit(ClassDecl classDecl) {
        currentClass = classDecl.name();
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
            formatter.format("\n");
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        formatter.format("define i32 @main() {\n");
        mainClass.mainStatement().accept(this);
        formatIndented("ret i32 0\n");
        formatter.format("}\n");
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        currentMethod = methodDecl.name();
        methodVariableTypes = new HashMap<>();
        regCounter = 0;

        formatDefineLine(methodDecl);
        for (var formal : methodDecl.formals()) {
            formatFormalArgRegRenaming(formal);
        }
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        formatReturnLine(methodDecl);

        formatter.format("}\n");
    }

    private void formatDefineLine(MethodDecl methodDecl) {
        formatter.format("define ");
        methodDecl.returnType().accept(this);
        formatter.format(" @%s.%s(i8* %%this", currentClass, currentMethod);
        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }
        formatter.format(") {\n");
    }

    @Override
    public void visit(FormalArg formalArg) {
        methodVariableTypes.put(formalArg.name(), formalArg.type());
        formatter.format(", ");
        formalArg.type().accept(this);
        formatter.format(" %%.%s", formalArg.name());
    }

    private void formatFormalArgRegRenaming(FormalArg formalArg) {
        // Allocate space on the stack for the variable
        formatVariableAllocation(formalArg);

        // Store the formal arg's value in the newly allocated space
        String sourceReg = "%." + formalArg.name();
        String destReg = "%" + formalArg.name();
        formatStore(sourceReg, destReg, formalArg.type());
    }

    private void formatVariableAllocation(VariableIntroduction variable) {
        formatIndented("%%%s = alloca ", variable.name());
        variable.type().accept(this);
        formatter.format("\n");
    }

    private void formatStore(String sourceRegOrValue, String destReg, AstType type) {
        formatIndented("store ");
        type.accept(this);
        formatter.format(" %s, ", sourceRegOrValue);
        type.accept(this);
        formatter.format("* %s\n", destReg);
    }

    private void formatLoad(String destReg, String sourceReg, AstType type) {
        formatIndented("%s = load ", destReg);
        type.accept(this);
        formatter.format(", ");
        type.accept(this);
        formatter.format("* %s\n", sourceReg);
    }

    @Override
    public void visit(VarDecl varDecl) {
        methodVariableTypes.put(varDecl.name(), varDecl.type());
        formatVariableAllocation(varDecl);
    }

    private void formatReturnLine(MethodDecl methodDecl) {
        methodDecl.ret().accept(this);
        formatIndented("ret ");
        methodDecl.returnType().accept(this);
        formatter.format(" %s\n", exprResults.pop());
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        int labelPostfix = nextLabelPostfix();

        // calculate condition
        ifStatement.cond().accept(this);
        String condValue = exprResults.pop();
        formatIndented("br i1 %s, label %%then_case_%d, label %%else_case_%d\n", condValue, labelPostfix, labelPostfix);

        // then case
        formatter.format("then_case_%d:\n", labelPostfix);
        ifStatement.thencase().accept(this);
        formatIndented("br label %%if_end_%d\n", labelPostfix);

        // else case
        formatter.format("else_case_%d:\n", labelPostfix);
        ifStatement.elsecase().accept(this);
        formatIndented("br label %%if_end_%d\n", labelPostfix);

        // end
        formatter.format("if_end_%d:\n", labelPostfix);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        int labelPostfix = nextLabelPostfix();

        // calculate condition
        formatIndented("br label %%while_check_cond_%d\n", labelPostfix);
        formatter.format("while_check_cond_%d:\n", labelPostfix);
        whileStatement.cond().accept(this);
        String condValue = exprResults.pop();
        formatIndented("br i1 %s, label %%while_body_%d, label %%while_end_%d\n", condValue, labelPostfix, labelPostfix);

        // while body
        formatter.format("while_body_%d:\n", labelPostfix);
        whileStatement.body().accept(this);
        formatIndented("br label %%while_check_cond_%d\n", labelPostfix);

        // end
        formatter.format("while_end_%d:\n", labelPostfix);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
        formatIndented("call void (i32) @print_int(i32 %s)\n", exprResults.pop());
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
        String rv = exprResults.pop();

        AstType lvType = formatAssignmentLv(assignStatement.lv());
        String destRegister = exprResults.pop();

        formatStore(rv, destRegister, lvType);
    }

    private AstType formatAssignmentLv(String lv) {
        String destRegister;
        AstType lvType;

        if (!methodVariableTypes.containsKey(lv)) {
            destRegister = loadFieldToRegister(lv);
            lvType = OOUtils.getFieldType(currentClass, lv);
        } else {
            destRegister = "%" + lv;
            lvType = methodVariableTypes.get(lv);
        }

        exprResults.push(destRegister);
        return lvType;
    }
    private String loadFieldToRegister(String fieldName) {
        String tempRegister = nextAnonymousReg();
        formatIndented("%s = getelementptr i8, i8* %%this, i32 %d\n", 
                tempRegister, OOUtils.getFieldOffset(currentClass, fieldName));
        String destRegister = nextAnonymousReg();
        formatIndented("%s = bitcast i8* %s to ", destRegister, tempRegister);
        OOUtils.getFieldType(currentClass, fieldName).accept(this);
        formatter.format("*\n");
        return destRegister;
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        AstType lvType = formatAssignmentLv(assignArrayStatement.lv());
        String arrayPtrReg = exprResults.pop();

        assignArrayStatement.index().accept(this);
        String index = exprResults.pop();

        // load the address to the array
        String arrayReg = nextAnonymousReg();
        formatIndented("%s = load i32*, i32** %s\n", arrayReg, arrayPtrReg);

        // get pointer to array element
        formatGetArrayElementPtr(arrayReg, index);
        String elementPtrReg = exprResults.pop();

        assignArrayStatement.rv().accept(this);
        String rv = exprResults.pop();

        // store value in element pointer
        formatIndented("store i32 %s, i32* %s\n", rv, elementPtrReg);
    }

    private void visitArithmeticBinaryExpr(BinaryExpr e, String op) {
        // Note: we only use this to format arithmetic operations ("+", "-" and "*")
        // so the return type is always i32.
        e.e1().accept(this);
        String value1 = exprResults.pop();
        e.e2().accept(this);
        String value2 = exprResults.pop();
        String resultReg = nextAnonymousReg();
        formatIndented("%s = %s i32 %s, %s\n", resultReg, op, value1, value2);
        exprResults.push(resultReg);
    }

    @Override
    public void visit(AndExpr e) {
        int labelPostfix = nextLabelPostfix();

        // calculate first expression
        e.e1().accept(this);
        String value1 = exprResults.pop();
        formatIndented("br label %%and_left_cond_%d\n", labelPostfix);

        // check first expression's result
        formatter.format("and_left_cond_%d:\n", labelPostfix);
        formatIndented("br i1 %s, label %%and_check_right_cond_%d, label %%and_result_%d\n",
                value1, labelPostfix, labelPostfix);

        // calculate second expression's result
        formatter.format("and_check_right_cond_%d:\n", labelPostfix);
        e.e2().accept(this);
        String value2 = exprResults.pop();
        formatIndented("br label %%and_right_cond_%d\n", labelPostfix);

        // jump to the result calculation
        formatter.format("and_right_cond_%d:\n", labelPostfix);
        formatIndented("br label %%and_result_%d\n", labelPostfix);

        // calculate result (based on the label that we got here from)
        String resultReg = nextAnonymousReg();
        formatter.format("and_result_%d:\n", labelPostfix);
        formatIndented("%s = phi i1 [0, %%and_left_cond_%d], [%s, %%and_right_cond_%d]\n",
                resultReg, labelPostfix, value2, labelPostfix);
        exprResults.push(resultReg);
    }

    @Override
    public void visit(LtExpr e) {
        e.e1().accept(this);
        String value1 = exprResults.pop();
        e.e2().accept(this);
        String value2 = exprResults.pop();
        String resultReg = nextAnonymousReg();
        formatIndented("%s = icmp slt i32 %s, %s\n", resultReg, value1, value2);
        exprResults.push(resultReg);
    }
    
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
        String resultReg = nextAnonymousReg();
        formatIndented("%s = sub i1 1, %s\n", resultReg, exprResults.pop());
        exprResults.push(resultReg);
    }
    
    @Override
    public void visit(AddExpr e) {
        visitArithmeticBinaryExpr(e, "add");
    }

    @Override
    public void visit(SubtractExpr e) {
        visitArithmeticBinaryExpr(e, "sub");
    }

    @Override
    public void visit(MultExpr e) {
        visitArithmeticBinaryExpr(e, "mul");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        String arrayPtrReg = exprResults.pop();

        e.indexExpr().accept(this);
        String index = exprResults.pop();

        formatGetArrayElementPtr(arrayPtrReg, index);
        String elementPtrReg = exprResults.pop();
        // load element to get the value
        String elementValueReg = nextAnonymousReg();
        formatIndented("%s = load i32, i32* %s\n", elementValueReg, elementPtrReg);

        exprResults.push(elementValueReg);
    }

    private void formatGetArrayElementPtr(String arrayReg, String index) {
        // check that the index is greater than 0
        int labelPostfix = nextLabelPostfix();
        String cmpReg = nextAnonymousReg();
        formatIndented("%s = icmp slt i32 %s, 0\n", cmpReg, index);
        formatIndented("br i1 %s, label %%negative_index_%d, label %%check_against_array_len_%d\n\n",
                cmpReg, labelPostfix, labelPostfix);
        formatter.format("negative_index_%d:\n", labelPostfix);
        formatIndented("call void @throw_oob()\n");
        formatIndented("br label %%check_against_array_len_%d\n\n", labelPostfix);
        formatter.format("check_against_array_len_%d:\n", labelPostfix);

        // load array length
        String firstElementReg = nextAnonymousReg();
        formatIndented("%s = getelementptr i32, i32* %s, i32 0\n", firstElementReg, arrayReg);
        String arrayLengthReg = nextAnonymousReg();
        formatIndented("%s = load i32, i32* %s\n", arrayLengthReg, firstElementReg);

        // check that index < array.length (equivalently, that !(array.length <= index))
        cmpReg = nextAnonymousReg();
        formatIndented("%s = icmp sle i32 %s, %s\n", cmpReg, arrayLengthReg, index);
        formatIndented("br i1 %s, label %%index_ge_array_len_%d, label %%valid_index_%d\n\n",
                cmpReg, labelPostfix, labelPostfix);
        formatter.format("index_ge_array_len_%d:\n", labelPostfix);
        formatIndented("call void @throw_oob()\n");
        formatIndented("br label %%valid_index_%d\n\n", labelPostfix);
        formatter.format("valid_index_%d:\n", labelPostfix);

        // add 1 to the index (the first element is the array length)
        String indexReg = nextAnonymousReg();
        formatIndented("%s = add i32 %s, 1\n", indexReg, index);

        // get a pointer to the element
        String elementPtrReg = nextAnonymousReg();
        formatIndented("%s = getelementptr i32, i32* %s, i32 %s\n", elementPtrReg, arrayReg, indexReg);

        exprResults.push(elementPtrReg);
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);
        String elementPtrReg = exprResults.pop();

        // load element pointer to get the value
        String elementValueReg = nextAnonymousReg();
        formatIndented("%s = load i32, i32* %s\n", elementValueReg, elementPtrReg);

        exprResults.push(elementValueReg);
    }


    @Override
    public void visit(MethodCallExpr e) {
        e.ownerExpr().accept(this);
        String ownerReg = exprResults.pop();

        // bitcast to pointer to the vtable
        String vtablePtrReg = nextAnonymousReg();
        formatIndented("%s = bitcast i8* %s to i8***\n", vtablePtrReg, ownerReg);

        // load the vtable
        String vtableReg = nextAnonymousReg();
        formatIndented("%s = load i8**, i8*** %s\n", vtableReg, vtablePtrReg);

        // get a pointer to the method's element in the vtable
        String methodElementReg = nextAnonymousReg();
        int methodIndex = OOUtils.getMethodIndex(classOfCalledMethod, e.methodId());
        formatIndented("%s = getelementptr i8*, i8** %s, i32 %d\n", methodElementReg, vtableReg, methodIndex);

        // load element to get the pointer to the method
        String tempMethodPtrReg = nextAnonymousReg();
        formatIndented("%s = load i8*, i8** %s\n", tempMethodPtrReg, methodElementReg);

        AstType returnType = OOUtils.getMethodReturnType(classOfCalledMethod, e.methodId());
        List<AstType> formalArgsTypes = OOUtils.getMethodFormalArgsTypes(classOfCalledMethod, e.methodId());

        // bitcast to a pointer of a method with the exact signature
        String methodPtrReg = nextAnonymousReg();
        formatIndented("%s = bitcast i8* %s to ", methodPtrReg, tempMethodPtrReg);
        formatMethodSignature(returnType, formalArgsTypes);
        formatter.format("\n");

        // make method call
        formatMethodCall(methodPtrReg, ownerReg, e.actuals(), returnType, formalArgsTypes);
    }

    private void formatMethodCall(String methodPtrReg, String ownerReg, List<Expr> actuals,
                                  AstType returnType, List<AstType> formalArgsTypes) {
        List<String> actualValues = new ArrayList<>();
        for (var actual : actuals) {
            actual.accept(this);
            actualValues.add(exprResults.pop());
        }

        String resultReg = nextAnonymousReg();
        formatIndented("%s = call ", resultReg);
        returnType.accept(this);
        formatter.format(" %s(i8* %s", methodPtrReg, ownerReg);
        for (int i = 0; i < actuals.size(); i++) {
            formatter.format(", ");
            formalArgsTypes.get(i).accept(this);
            formatter.format(" ");
            formatter.format("%s", actualValues.get(i));
        }
        formatter.format(")\n");
        exprResults.push(resultReg);
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        exprResults.push(Integer.toString(e.num()));
    }

    @Override
    public void visit(TrueExpr e) {
        exprResults.push("1");
    }

    @Override
    public void visit(FalseExpr e) {
        exprResults.push("0");
    }

    @Override
    public void visit(IdentifierExpr e) {
        String identifierReg;
        AstType type;
        if (!methodVariableTypes.containsKey(e.id())) {
            identifierReg = loadFieldToRegister(e.id());
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
        String valueReg = nextAnonymousReg();
        formatLoad(valueReg, identifierReg, type);
        exprResults.push(valueReg);

    }

    public void visit(ThisExpr e) {
        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        classOfCalledMethod = currentClass;

        exprResults.push("%this");
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
        String arrayLength = exprResults.pop();

        // format array length validation
        String cmpReg = nextAnonymousReg();
        int labelPostfix = nextLabelPostfix();
        formatIndented("%s = icmp slt i32 %s, 0\n", cmpReg, arrayLength);
        formatIndented("br i1 %s, label %%invalid_arr_len_%d, label %%valid_array_len_%d\n\n",
                cmpReg, labelPostfix, labelPostfix);
        formatter.format("invalid_arr_len_%d:\n", labelPostfix);
        formatIndented("call void @throw_oob()\n");
        formatIndented("br label %%valid_array_len_%d\n\n", labelPostfix);
        formatter.format("valid_array_len_%d:\n", labelPostfix);

        // calculate array size
        String totalSizeReg = nextAnonymousReg();
        formatIndented("%s = add i32 %s, 1\n", totalSizeReg, arrayLength);

        // allocate new array, and store arrayLen in the first cell
        String tempReg = nextAnonymousReg();
        formatIndented("%s = call i8* @calloc(i32 4, i32 %s)\n", tempReg, totalSizeReg);
        String arrayReg = nextAnonymousReg();
        formatIndented("%s = bitcast i8* %s to i32*\n", arrayReg, tempReg);
        formatIndented("store i32 %s, i32* %s\n", arrayLength, arrayReg);
        exprResults.push(arrayReg);
    }

    @Override
    public void visit(NewObjectExpr e) {
        String instanceReg = nextAnonymousReg();
        formatIndented("%s = call i8* @calloc(i32 1, i32 %d)\n", instanceReg, OOUtils.getInstanceSize(e.classId()));
        String vtablePtrReg = nextAnonymousReg();
        formatIndented("%s = bitcast i8* %s to i8***\n", vtablePtrReg, instanceReg);
        String vtableFirstElementReg = nextAnonymousReg();
        int numOfMethods = OOUtils.getNumberOfMethods(e.classId());
        formatIndented("%s = getelementptr [%d x i8*], [%d x i8*]* @.%s_vtable, i32 0, i32 0\n",
                vtableFirstElementReg, numOfMethods, numOfMethods, e.classId());
        formatIndented("store i8** %s, i8*** %s\n", vtableFirstElementReg, vtablePtrReg);

        // In case we got here from a method call expression (as the owner expression),
        // save the class name to classOfCalledMethod
        classOfCalledMethod = e.classId();

        exprResults.push(instanceReg);
    }

    @Override
    public void visit(IntAstType t) {
        formatter.format("i32");
    }

    @Override
    public void visit(BoolAstType t) {
        formatter.format("i1");
    }

    @Override
    public void visit(IntArrayAstType t) {
        formatter.format("i32*");
    }

    @Override
    public void visit(RefType t) {
        formatter.format("i8*");
    }
}

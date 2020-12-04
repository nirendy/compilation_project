package ast.llvm_formatting;

import ast.AstType;
import ast.IntAstType;

import java.util.ArrayList;
import java.util.List;

public class LLVMObjectOrientedUtils {
    public int getFieldOffset(String className, String fieldName) {
        // TODO: implement (waiting for @nimrod.pansky)
        return 8;
    }

    public AstType getFieldType(String className, String fieldName) {
        // TODO: implement (waiting for @nimrod.pansky)
        return new IntAstType();
    }

    public int getMethodIndex(String className, String methodName) {
        // TODO: implement (waiting for @nimrod.pansky)
        return 5;
    }

    public AstType getMethodReturnType(String className, String methodName) {
        // TODO: implement (waiting for @nimrod.pansky)
        return new IntAstType();
    }

    public List<AstType> getMethodFormalArgsTypes(String className, String methodName) {
        // TODO: implement (waiting for @nimrod.pansky)
        ArrayList<AstType> formalArgsTypes = new ArrayList<>();
        formalArgsTypes.add(new IntAstType());
        return formalArgsTypes;
    }

    public int getInstanceSize(String className) {
        // TODO: implement (waiting for @nimrod.pansky)
        return 12;
    }

    public int getNumberOfMethods(String className) {
        // TODO: implement (waiting for @nimrod.pansky)
        return 2;
    }
}

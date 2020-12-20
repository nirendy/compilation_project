package ast.semantic_checks;

import ast.*;

import java.util.HashSet;
import java.util.Set;

public class SemanticChecksUtils {
    private final Set<Class<? extends Expr>> methodCallOwnerExprTypes = new HashSet<>();

    public SemanticChecksUtils() {
        this.methodCallOwnerExprTypes.add(ThisExpr.class);
        this.methodCallOwnerExprTypes.add(IdentifierExpr.class);
        this.methodCallOwnerExprTypes.add(NewObjectExpr.class);
    }

    public boolean isValidOwnerExpressionType(Expr ownerExpr) {
        return methodCallOwnerExprTypes.contains(ownerExpr.getClass());
    }
}

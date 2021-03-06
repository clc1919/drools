package org.drools.modelcompiler.builder.generator;

import java.util.HashSet;

import org.drools.javaparser.ast.NodeList;
import org.drools.javaparser.ast.drlx.expr.PointFreeExpr;
import org.drools.javaparser.ast.expr.Expression;
import org.drools.javaparser.ast.expr.NameExpr;
import org.drools.javaparser.ast.expr.SimpleName;
import org.drools.javaparser.ast.expr.StringLiteralExpr;
import org.drools.modelcompiler.builder.PackageModel;
import org.drools.modelcompiler.builder.generator.expressiontyper.ExpressionTyper;
import org.drools.modelcompiler.builder.generator.expressiontyper.TypedExpressionResult;
import org.drools.modelcompiler.domain.Overloaded;
import org.drools.modelcompiler.domain.Person;
import org.junit.Test;
import org.kie.soup.project.datamodel.commons.types.ClassTypeResolver;
import org.kie.soup.project.datamodel.commons.types.TypeResolver;

import static org.junit.Assert.*;

public class ToTypedExpressionTest {

    private final HashSet<String> imports = new HashSet<>();
    private final PackageModel packageModel = new PackageModel("", null);
    final TypeResolver typeResolver = new ClassTypeResolver(imports, getClass().getClassLoader());
    private final RuleContext ruleContext = new RuleContext(null, packageModel, null, typeResolver);

    {
        imports.add("org.drools.modelcompiler.domain.Person");
    }


    @Test
    public void toTypedExpressionTest() {
        assertEquals(typedResult("$mark.getAge()", int.class), toTypedExpression("$mark.age", null, aPersonDecl("$mark")));
        assertEquals(typedResult("$p.getName()", String.class), toTypedExpression("$p.name", null, aPersonDecl("$p")));

        TypedExpression actual = toTypedExpression("name.length", Person.class);
        assertEquals(typedResult("_this.getName().length()", int.class), actual);


        assertEquals(typedResult("_this.method(5,9,\"x\")", int.class), toTypedExpression("method(5,9,\"x\")", Overloaded.class));
        assertEquals(typedResult("_this.getAddress().getCity().length()", int.class), toTypedExpression("address.getCity().length", Person.class));

        TypedExpression inlineCastResult = typedResult("((org.drools.modelcompiler.domain.Person) _this).getName()", String.class);
        assertEquals(inlineCastResult, toTypedExpression("this#Person.name", Object.class));

    }

    @Test
    public void pointFreeTest() {
        final PointFreeExpr expression = new PointFreeExpr(null, new NameExpr("name"), NodeList.nodeList(new StringLiteralExpr("[A-Z]")), new SimpleName("matches"), false, null, null, null, null);
        TypedExpressionResult typedExpressionResult = new ExpressionTyper(ruleContext, Person.class, null, true).toTypedExpression(expression);
        final TypedExpression actual = typedExpressionResult.getTypedExpression().get();
        final TypedExpression expected = typedResult("eval(org.drools.model.operators.MatchesOperator.INSTANCE, _this.getName(), \"[A-Z]\")", String.class);
        assertEquals(expected, actual);
    }

    private TypedExpression toTypedExpression(String inputExpression, Class<?> patternType, DeclarationSpec... declarations) {

        for(DeclarationSpec d : declarations) {
            ruleContext.addDeclaration(d);
        }
        Expression expression = DrlxParseUtil.parseExpression(inputExpression).getExpr();
        return new ExpressionTyper(ruleContext, patternType, null, true).toTypedExpression(expression).getTypedExpression().get();
    }


    private TypedExpression typedResult(String expressionResult, Class<?> classResult) {
        Expression resultExpression = DrlxParseUtil.parseExpression(expressionResult).getExpr();
        return new TypedExpression(resultExpression, classResult);
    }

    private DeclarationSpec aPersonDecl(String $mark) {
        return new DeclarationSpec($mark, Person.class);
    }

}

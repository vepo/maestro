package dev.vepo.maestro.lang.model;

public sealed interface Expression permits LiteralExpression,
        FieldReferenceExpression, FunctionCallExpression,
        ComparisonExpression, LogicalExpression, NotExpression,
        InPredicate, BetweenPredicate, IsNullPredicate, IsNotNullPredicate,
        LikePredicate, RegexPredicate, ParenthesizedExpression {

    public static Expression and(Expression left, Expression right) {
        return new LogicalExpression(left, LogicalOperator.AND, right);
    }

    public static Expression or(Expression left, Expression right) {
        return new LogicalExpression(left, LogicalOperator.OR, right);
    }

    public static Expression parenthesis(Expression group) {
        return new ParenthesizedExpression(group);
    }

    public static Expression gt(Expression left, Expression right) {
        return new ComparisonExpression(left, ComparisonOperator.GT, right);
    }

    public static Expression lt(Expression left, Expression right) {
        return new ComparisonExpression(left, ComparisonOperator.LT, right);
    }

    public static Expression eq(Expression left, Expression right) {
        return new ComparisonExpression(left, ComparisonOperator.EQ, right);
    }

    public static Expression in(String fieldName, Literal... literals) {
        return new InPredicate(fieldName, literals);
    }

    public static Expression not(Expression expression) {
        return new NotExpression(expression);
    }
}
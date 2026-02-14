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
}
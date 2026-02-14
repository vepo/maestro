package dev.vepo.maestro.lang.model;

public sealed interface Expression 
    permits LiteralExpression, FieldReferenceExpression, FunctionCallExpression,
            ComparisonExpression, LogicalExpression, NotExpression,
            InPredicate, BetweenPredicate, IsNullPredicate, IsNotNullPredicate,
            LikePredicate, RegexPredicate, ParenthesizedExpression {}
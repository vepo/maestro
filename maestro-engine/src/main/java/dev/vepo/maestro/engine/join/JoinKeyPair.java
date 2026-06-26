package dev.vepo.maestro.engine.join;

import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.Expression;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;

public record JoinKeyPair(String leftField, String rightField) {

    public static JoinKeyPair fromCondition(Expression condition) {
        if (condition instanceof ComparisonExpression comparison && comparison.operator() == ComparisonOperator.EQ
                && comparison.left() instanceof FieldReferenceExpression left
                && comparison.right() instanceof FieldReferenceExpression right) {
            return new JoinKeyPair(left.fieldName(), right.fieldName());
        }
        throw new IllegalArgumentException("Join condition must be field = field equality");
    }
}

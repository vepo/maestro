package dev.vepo.maestro.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Gherkin-like scaffold for parser and domain tests. Scenario strings must use
 * Maestro domain language.
 */
final class Scenario {

    static final class Given {
        private final List<String> steps = new ArrayList<>();

        Given(String description) {
            steps.add("Given " + description);
        }

        Given and(String context) {
            steps.add("And " + context);
            return this;
        }

        When when(String action) {
            return new When(steps, action);
        }
    }

    static final class Then {
        private final List<String> steps;

        Then(List<String> steps, String outcome) {
            this.steps = new ArrayList<>(steps);
            this.steps.add("Then " + outcome);
        }

        Then and(String outcome) {
            steps.add("And " + outcome);
            return this;
        }

        void run(Runnable given, Runnable when, Runnable then) {
            given.run();
            when.run();
            then.run();
        }

        List<String> steps() {
            return List.copyOf(steps);
        }
    }

    static final class When {
        private final List<String> steps;

        When(List<String> steps, String action) {
            this.steps = new ArrayList<>(steps);
            this.steps.add("When " + action);
        }

        When and(String action) {
            steps.add("And " + action);
            return this;
        }

        Then then(String outcome) {
            return new Then(steps, outcome);
        }
    }

    static Given given(String description) {
        return new Given(description);
    }

    private Scenario() {}
}

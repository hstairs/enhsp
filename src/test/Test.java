package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.util.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import planners.ENHSP;
import utils.ArgParams;
import validators.Validator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Test {

    @ParameterizedTest
    @MethodSource("provideTestsForPlanner")
    public void plannerTest(Instance instance, String planner) {


        String input = String.format("-o %s -f %s", instance.domain, instance.problem);
        if (planner != null) {
            input = String.format("%s -planner %s", input, planner);
        }

        ArgParams args = ArgParams.parseInput(input.split(" "));

        try {
            ENHSP e = new ENHSP(args);
            e.plan();
        } catch (Exception ex) {
            ex.printStackTrace();
            assertEquals(true, false);
        }
    }

    private static ArrayList<Arguments> provideTestsForPlanner() {

        ArrayList<Arguments> args = new ArrayList<Arguments>();

        List<String> planners = Arrays.asList(
                null,
                "sat-hadd",
                "sat-hradd",
                "haddabs",
                "sat-aibr",
                "opt-hrmax",
                "opt-hmax",
                "opt-blind"
                //,"opt-hlm",
                //"opt-hlmrc"
        );

        List<Instance> instances = Arrays.asList(
                new Instance("/examples/seq_planning/plant-watering/validate/ex1/domain.pddl","/examples/seq_planning/plant-watering/validate/ex1/problem.pddl"),
                new Instance("/ijcai18_benchmarks/fo_counters/domain.pddl", "/ijcai18_benchmarks/fo_counters/instance_4.pddl"),
                new Instance("/ijcai18_benchmarks/TPP/domain.pddl", "/ijcai18_benchmarks/TPP/p01.pddl")
        );

        for (Instance instance : instances) {
            for (String planner : planners) {
                args.add(Arguments.of(instance, planner));
            }
        }


        return args;
    }

    @ParameterizedTest
    @MethodSource("provideTestsForValidator")
    public void validatorTest(Instance instance) {


        String input = String.format("-v -o %s -f %s -ip %s", instance.domain, instance.problem, instance.plan);

        ArgParams args = ArgParams.parseInput(input.split(" "));

        try {
            Validator v = new Validator(args);
            Boolean result = v.validate();
            assertEquals(result, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assertEquals(true, false);
        }
    }

    private static ArrayList<Arguments> provideTestsForValidator() {

        ArrayList<Arguments> args = new ArrayList<Arguments>();

        List<Instance> instances = Arrays.asList(
                new Instance(
                        "/examples/seq_planning/plant-watering/validate/ex1/domain.pddl",
                        "/examples/seq_planning/plant-watering/validate/ex1/problem.pddl",
                        "/examples/seq_planning/plant-watering/validate/ex1/plan.txt"
                ),
                new Instance(
                        "/examples/seq_planning/sailing/validate/domain.pddl",
                        "/examples/seq_planning/sailing/validate/problem.pddl",
                        "/examples/seq_planning/sailing/validate/plan.txt"
                )
        );

        for (Instance instance : instances) {
            args.add(Arguments.of(instance));
        }


        return args;
    }

    private static class Instance {
        public String domain;
        public String problem;
        public String plan;
        private String relativeDomain;
        private String relativeProblem;

        public Instance(String domain, String problem) {
            String path = Paths.get("").toAbsolutePath().toString();
            this.domain = path + domain;
            this.problem = path + problem;
            this.relativeDomain = domain;
            this.relativeProblem = problem;
        }

        public Instance(String domain, String problem, String plan){
            String path = Paths.get("").toAbsolutePath().toString();
            this.domain = path + domain;
            this.problem = path + problem;
            this.plan = path + plan;
            this.relativeDomain = domain;
            this.relativeProblem = problem;
        }

        @Override
        public String toString() {
            return relativeProblem;
        }
    }

}
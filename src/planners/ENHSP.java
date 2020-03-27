package planners;

import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.heuristics.Heuristic;
import com.hstairs.ppmajal.heuristics.advanced.quasi_hm;
import com.hstairs.ppmajal.plan.SimplePlan;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.search.PDDLSearchEngine;
import com.hstairs.ppmajal.search.SearchNode;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.ArgParams;
import utils.PlannerSearchEngine;
import utils.Planner;

/*
 * Copyright (C) 2016-2017 Enrico Scala. Email enricos83@gmail.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

/**
 * @author enrico
 */
public class ENHSP {

    private ArgParams args;

    private Heuristic heuristicFunction;
    private EPddlProblem problem;
    private boolean isPDDLPlus;
    private PddlDomain domain;
    private PddlDomain domainHeuristic;
    private EPddlProblem heuristicProblem;
    private long veryStart;
    private boolean isCopyOfTheProblem;
    private Planner planner;
    private long overalPlanningTime;

    public ENHSP(ArgParams args) throws Exception {

        this.args = args;
        this.veryStart = System.currentTimeMillis();
        this.domain = new PddlDomain(this.args.domainFile);
        this.domain.substituteEqualityConditions();
        this.isPDDLPlus = !domain.getProcessesSchema().isEmpty() || !domain.eventsSchema.isEmpty();

        System.out.println("Domain parsed");

        this.problem = new EPddlProblem(args.problemFile, domain.getConstants(), domain.types, domain);
        //this second model is the one used in the heuristic. This can potentially be different from the one used in the execution model. Decoupling it
        //allows us to a have a finer control on the machine
        //the third one is the validation model, where, also in this case we test our plan against a potentially more accurate description

        System.out.println("Problem parsed");
        System.out.println("Grounding..");
        this.problem.transformGoal();
        this.problem.groundingActionProcessesConstraints();

        List<String> heuristicsWhitelist = Arrays.asList("hadd", "hff", "hlm", "hm_max", "hrmax", "hmax", "haddabs");

        if (isPDDLPlus || heuristicsWhitelist.contains(args.heuristic)) {
            this.domainHeuristic = new PddlDomain(args.domainFile);
            this.domainHeuristic.substituteEqualityConditions();
            this.heuristicProblem = new EPddlProblem(args.problemFile, domainHeuristic.getConstants(), domain.getTypes(), domainHeuristic);
            this.heuristicProblem.transformGoal();
            this.heuristicProblem.groundingActionProcessesConstraints();//optimise this using clone.
//          heuristicProblem.syncAllVariablesAndUpdateCollections(getProblem());//This way we are sure we only keep one copy of each variable

            isCopyOfTheProblem = true;

            if (this.isPDDLPlus) {
                heuristicProblem.setDeltaTimeVariable(args.deltaTH.toString());
                getProblem().setDeltaTimeVariable(args.deltaT.toString());

            }
        } else {
            this.heuristicProblem = this.problem;
        }
    }


    public void plan() throws Exception {

        this.simplifyModels();
        do {
            SimplePlan sp = this.search();
            if (sp == null) {
                return;
            }
            float depthLimit = sp.getCost();
            sp.savePlan("/tmp/plan." + depthLimit);
            if (args.anyTime) {
                System.out.println("NEW COST ==================================================================================>" + depthLimit);
            }
            sp = null;
            System.gc();
        } while (args.anyTime);
    }

    /**
     * @return the heuristicFunction
     */
    public Heuristic getHeuristicFunction() {
        return heuristicFunction;
    }

    /**
     * @param heuristicFunction the heuristicFunction to set
     */
    public void setHeuristicFunction(Heuristic heuristicFunction) {
        this.heuristicFunction = heuristicFunction;
    }

    /**
     * @return the problem
     */
    public EPddlProblem getProblem() {
        return problem;
    }

    private void simplifyModels() throws Exception {
        System.out.println("Light Validation Completed");
        this.planner = new Planner(args);

        if (args.debug == 11) {
            System.out.println("Before Reachability: " + getProblem().actions);
        }

        System.out.println("Simplification..");
        problem.setAction_cost_from_metric(!args.ignoreMetric);
        getProblem().simplifyAndSetupInit(true, planner.aibrPreprocessing);

        if (isCopyOfTheProblem) {
            heuristicProblem.setAction_cost_from_metric(!args.ignoreMetric);
            heuristicProblem.simplifyAndSetupInit();
        }

        System.out.println("Grounding and Simplification finished");
        System.out.println("|A|:" + getProblem().getActions().size());
        System.out.println("|P|:" + getProblem().getProcessesSet().size());
        System.out.println("|E|:" + getProblem().getEventsSet().size());
        System.out.println("Size(X):" + problem.getNumberOfNumericVariables());
        System.out.println("Size(F):" + problem.getNumberOfBooleanVariables());

        if (isPDDLPlus) {
            System.out.println("Delta time heuristic model:" + args.deltaTH);
            System.out.println("Delta time planning model:" + args.deltaMax);
            System.out.println("Delta time search-execution model:" + args.deltaT);
            System.out.println("Delta time validation model:" + args.deltaVal);
        }

        planner.heuristic = getHeuristicFunction() != null ? getHeuristicFunction() : this.planner.getHeuristic(getProblem(), heuristicProblem, args.nOfSubDomains);

    }

    private SimplePlan search() throws Exception {

        PlannerSearchEngine se = new PlannerSearchEngine(planner, args, isPDDLPlus);

        LinkedList rawPlan = se.getRawPlan(getProblem(), args.timeout);

        Runtime.getRuntime().addShutdownHook(new Thread() {//this is to save json also when the planner is interrupted
            @Override
            public void run() {
                if (args.savingJSON) {
                    se.getSearchEngine().searchSpaceHandle.print_json(getProblem().getPddlFileReference() + ".sp_log");
                }
            }
        });

        overalPlanningTime = (System.currentTimeMillis() - veryStart);
        SimplePlan sp = validate(se.getSearchEngine(), rawPlan);
        printInfo(sp, se.getSearchEngine());

        return sp;
    }

    private SimplePlan validate(PDDLSearchEngine searchEngine, LinkedList raw_plan) throws Exception {

        SimplePlan sp = new SimplePlan(domain, getProblem(), false, isPDDLPlus);  //placeholder for the plan to be found


        System.out.println("Starting Validation");

        if (raw_plan == null) {
            return null;
        }
        // Print some useful information on the outcome of the planning process
        sp.print_trace = args.printTrace;

        PDDLState lastState = null;
        if (!isPDDLPlus) {
            sp.addAll(raw_plan);
            lastState = sp.execute((PDDLState) getProblem().getInit(), getProblem().globalConstraints);
            System.out.println("(Pddl2.1 semantics) Plan is valid:" + lastState.satisfy(getProblem().getGoals()));
        } else { //This is when you have also autonomous processes going on

            PddlDomain validationDomain = new PddlDomain(args.domainFile);
            EPddlProblem validationProblem = new EPddlProblem(args.problemFile, validationDomain.getConstants(), validationDomain.getTypes(), validationDomain);
            //this is when you have processes
            validationProblem.groundingActionProcessesConstraints();
            validationProblem.setDeltaTimeVariable(args.deltaVal.toString());
            validationProblem.simplifyAndSetupInit(true);
            Float time = sp.build_pddl_plus_plan(raw_plan, args.epsilon);

            lastState = sp.execute(
                    (PDDLState) validationProblem.getInit(),
                    validationProblem.globalConstraints,
                    validationProblem.getProcessesSet(),
                    validationProblem.getEventsSet(),
                    searchEngine.planningDelta,
                    args.deltaVal,
                    time
            );

            boolean goal_reached = lastState.satisfy(getProblem().getGoals());
            System.out.println("(Pddl+ semantics) Plan is valid:" + goal_reached);

            if (lastState != null) {
                sp.setDuration(!isPDDLPlus ? sp.size(): lastState.time);
            }
            return sp;
        };

        return sp;
    };

    private void printInfo(SimplePlan sp, PDDLSearchEngine searchEngine) {

        if (sp != null) {
            System.out.println("Problem Solved");
            if (isPDDLPlus) {
                System.out.println(sp.printPDDLPlusPlan());
            } else {
                System.out.println(sp);
            }
            System.out.println("Plan-Length:" + sp.size());
            System.out.println("Duration:" + sp.getDuration());
            System.out.println("Metric (Plan):" + sp.getCost());

            if (args.savePlan) {
                sp.savePlan(getProblem().getPddlFileReference() + "_c_" + planner.heuristicString + "_gw_" + planner.gw + "_hw_" + planner.hw + "_delta_" + args.deltaT + ".plan", true);
            }

            if (args.printTrace) {
                FileWriter file = null;
                try {
                    file = new FileWriter(getProblem().getPddlFileReference() + "_search_" + args.searchEngine + "_h_" + planner.heuristicString + "_break_ties_" + planner.breakTies + ".npt");
                    //System.out.println(this.json_rep.toJSONString());
                    file.write(sp.numeric_plan_trace.toJSONString());
                    file.close();
                } catch (IOException ex) {
                    Logger.getLogger(SearchNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Numeric Plan Trace saved");
            }
        } else {
            System.out.println("Problem unsolvable");
        }
        System.out.println("Metric (Search):" + searchEngine.currentG);
        System.out.println("Planning Time:" + overalPlanningTime);
        System.out.println("Heuristic Time:" + searchEngine.getHeuristicCpuTime());
        System.out.println("Search Time:" + searchEngine.getOverallSearchTime());
        System.out.println("Expanded Nodes:" + searchEngine.getNodesExpanded());
        System.out.println("States Evaluated:" + searchEngine.getNumberOfEvaluatedStates());
        System.out.println("Fixed constraint violations during search (zero-crossing):" + searchEngine.constraintsViolations);
        System.out.println("Number of Dead-Ends detected:" + searchEngine.deadEndsDetected);
        System.out.println("Number of Duplicates detected:" + searchEngine.duplicatesNumber);
        if (searchEngine.getHeuristic() instanceof quasi_hm) {
            System.out.println("Number of LP invocations:" + ((quasi_hm) searchEngine.getHeuristic()).n_lp_invocations);
        }
        if (args.savingJSON) {
            searchEngine.searchSpaceHandle.print_json(getProblem().getPddlFileReference() + ".sp_log");
        }
    }
}

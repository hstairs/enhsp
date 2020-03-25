package planners;

import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.heuristics.Aibr;
import com.hstairs.ppmajal.heuristics.Heuristic;
import com.hstairs.ppmajal.heuristics.advanced.GoalCounting;
import com.hstairs.ppmajal.heuristics.advanced.h1;
import com.hstairs.ppmajal.heuristics.advanced.habs_add;
import com.hstairs.ppmajal.heuristics.advanced.hlm;
import com.hstairs.ppmajal.heuristics.advanced.quasi_hm;
import com.hstairs.ppmajal.heuristics.blindHeuristic;
import com.hstairs.ppmajal.plan.SimplePlan;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.search.PDDLSearchEngine;
import com.hstairs.ppmajal.search.SearchEngine;
import com.hstairs.ppmajal.search.SearchNode;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
 *
 * @author enrico
 *
 *
 *
 */
public class ENHSP {

    private String domainFile;
    private String problemFile;
    private String search_engine;
    private String hw;
    private String heuristic = "aibr";
    private String gw;
    private int debug_level = 0;
    private boolean saving_json = false;
    private String delta_t;
    private float depthLimit;
    private boolean save_plan;
    private boolean print_trace;
    private String break_ties;
    private String planner;
    private Float epsilon;
    private String epsilon_string;
    private Integer debug;
    private String delta_t_h;
    private String delta_max;
    private String delta_val;
    private boolean eco_saving_json;
    private boolean hh_pruning;
    private boolean ignore_metric;
    private Integer num_of_subdomains;
    private Heuristic heuristicFunction;
    private EPddlProblem problem;
    private boolean pddlPlus;
    private PddlDomain domain;
    private PddlDomain domainHeuristic;
    private EPddlProblem heuristicProblem;
    private long very_start;
    private boolean copyOfTheProblem;
    private boolean anyTime;
    private long timeOut;
    private boolean aibrPreprocessing;
    private Heuristic h;
    private long overal_planning_time;
    private String inputPlan;

    public ENHSP(boolean copyProblem) {
        copyOfTheProblem = copyProblem;
    }

    public void parsingDomainAndProblem(String[] args) {
        try {
            parseInput(args);
            very_start = System.currentTimeMillis();
            domain = new PddlDomain(domainFile);
            domain.substituteEqualityConditions();
            pddlPlus = !domain.getProcessesSchema().isEmpty() || !domain.eventsSchema.isEmpty();

            System.out.println("Domain parsed");

            problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);
            //this second model is the one used in the heuristic. This can potentially be different from the one used in the execution model. Decoupling it
            //allows us to a have a finer control on the machine
            //the third one is the validation model, where, also in this case we test our plan against a potentially more accurate description

            System.out.println("Problem parsed");
            System.out.println("Grounding..");
            getProblem().transformGoal();
            getProblem().groundingActionProcessesConstraints();
            if (pddlPlus || copyOfTheProblem || heuristic.equals("hadd") || heuristic.equals("hff")
                    || heuristic.equals("hlm") || heuristic.equals("hm_max") || heuristic.equals("hrmax") || heuristic.equals("hmax")
                    || heuristic.equals("haddabs")) {
                domainHeuristic = new PddlDomain(domainFile);
                domainHeuristic.substituteEqualityConditions();
                heuristicProblem = new EPddlProblem(problemFile, domainHeuristic.getConstants(), domain.getTypes(),domainHeuristic);
                heuristicProblem.transformGoal();
                heuristicProblem.groundingActionProcessesConstraints();//optimise this using clone.
//                heuristicProblem.syncAllVariablesAndUpdateCollections(getProblem());//This way we are sure we only keep one copy of each variable
                copyOfTheProblem = true;
                if (pddlPlus) {
                    heuristicProblem.setDeltaTimeVariable(delta_t_h);
                    getProblem().setDeltaTimeVariable(delta_t);

                }
            } else {
                heuristicProblem = problem;

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void planning() {

        try {
            simplifyModels();
            do {
                SimplePlan sp = search();
                if (sp == null) {
                    return;
                }
                depthLimit = sp.getCost();
                sp.savePlan("/tmp/plan." + depthLimit);
                if (anyTime) {
                    System.out.println("NEW COST ==================================================================================>" + depthLimit);
                }
                sp = null;
                System.gc();
            } while (anyTime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void parseInput(String[] args) {
        Options options = new Options();
        options.addRequiredOption("o", "domain", true, "PDDL domain file");
        options.addRequiredOption("f", "problem", true, "PDDL problem file");
        options.addOption("planner", true, "Fast Preconfgured Planner. For available options look into the code. This overrides all other parameters but domain and problem specs.");
        options.addOption("h", true, "heuristic: options (default is AIBR):\n"
                + "aibr, Additive Interval Based relaxation heuristic\n"
                + "hadd, Additive version of subgoaling heuristic\n"
                + "hmax, Hmax for Numeric Planning\n"
                + "hrmax, Hmax for Numeric Planning with redundant constraints\n"
                + "hff, hadd with extraction of relaxed plan a-la ff manner\n"
                + "lm_actions_rc_dc, Landmark based heuristic "
                + "with redundant constraints and metric sensitive intersection (Requires CPLEX 12.6.3)\n"
                + "lm_actions, Landmark based heuristic (Requires CPLEX 12.6.3)\n"
                + "lm_actions_rc, Landmark based heuristic  (Requires CPLEX 12.6.3)\n"
                + "blind, goal sensitive heuristic (1 to non goal-states, 0 to goal-states");
        options.addOption("s", true, "allows to select search strategy (default is WAStar):\n"
                + "gbfs, Greedy Best First Search (f(n) = h(n))\n"
                + "WAStar, WA* (f(n) = g(n) + h_w*h(n))\n"
                + "wa_star_4, WA* (f(n) = g(n) + 4*h(n))\n"
                + "ehc, Enforced Hill Climbing\n"
                + "gbfs_ha, Greedy Best First Search with Helpful Actions Pruning\n"
                + "ehc_ha, Enforced Hill Climbing with Helpful Actions Pruning");
        options.addOption("ties", true, "tie-breaking (default is arbitrary): larger_g, smaller_g, arbitrary");
        options.addOption("delta_max", true, "planning decision executionDelta: float");
        options.addOption("delta_exec", true, "planning execution executionDelta: float");
        options.addOption("delta_h", true, "planning heuristic executionDelta: float");
        options.addOption("delta_val", true, "validation executionDelta: float");
        options.addOption("delta", true, "global executionDelta time. Override other delta_<max,exec,val,h> configurations: float");
        options.addOption("debugLevel", true, "debugLevel level: integer");
        options.addOption("epsilon", true, "epsilon separation: float");
        options.addOption("gw", true, "g-values weight: float");
        options.addOption("hw", true, "h-values weight: float");
        options.addOption("sjr", false, "save state space explored in json file");
        options.addOption("hh", false, "activate helpful actions pruning");
        options.addOption("sp", false, "save the plan obtained");
        options.addOption("pt", false, "print state trajectory (Experimental)");
        options.addOption("im", false, "Ignore Metric in the heuristic");
        options.addOption("dl", true, "bound on plan-cost: float (Experimental)");
        options.addOption("k", true, "maximal number of subdomains. This works in combination with haddabs: integer");
        options.addOption("anytime", false, "Run in anytime modality. Incrementally tries to find an upper bound. Does not stop until the user decides so");
        options.addOption("timeout", true, "Timeout for anytime modality");
        options.addOption("dap", false, "disable Aibr Preprocessing");
        options.addOption("ip",true,"Read plan from file");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            domainFile = cmd.getOptionValue("o");
            problemFile = cmd.getOptionValue("f");
            planner = cmd.getOptionValue("planner");
            heuristic = cmd.getOptionValue("h");
            if (heuristic == null) {
                heuristic = "aibr";
            }
            search_engine = cmd.getOptionValue("s");
            if (search_engine == null) {
                search_engine = "WAStar";
            }
            inputPlan = cmd.getOptionValue("ip");
            break_ties = cmd.getOptionValue("ties");
            delta_max = cmd.getOptionValue("delta_max");
            if (delta_max == null) {
                delta_max = "1.0";
            }
            delta_t = cmd.getOptionValue("delta_exec");
            if (delta_t == null) {
                delta_t = "1.0";
            }
            delta_t_h = cmd.getOptionValue("delta_h");
            if (delta_t_h == null) {
                delta_t_h = "1.0";
            }
            delta_val = cmd.getOptionValue("delta_val");
            if (delta_val == null) {
                delta_val = "1";
            }
            String temp = cmd.getOptionValue("dl");
            if (temp != null) {
                depthLimit = Float.parseFloat(temp);
            } else {
                depthLimit = Float.NaN;
            }

            String timeOutString = cmd.getOptionValue("timeout");
            if (timeOutString != null) {
                timeOut = Long.parseLong(timeOutString) * 1000;
            } else {
                timeOut = Long.MAX_VALUE;
            }

            String delta = cmd.getOptionValue("delta");
            if (delta != null) {
                delta_t_h = delta;
                delta_val = delta;
                delta_max = delta;
                delta_t = delta;
            }
            String deb = cmd.getOptionValue("debugLevel");
            if (deb != null) {
                debug = Integer.parseInt(deb);
            } else {
                debug = 0;
            }

            String k = cmd.getOptionValue("k");
            if (k != null) {
                num_of_subdomains = Integer.parseInt(k);
            } else {
                num_of_subdomains = 2;
            }

            epsilon_string = cmd.getOptionValue("epsilon");
            if (epsilon_string == null) {
                epsilon = 0f;
            } else {
                epsilon = Float.parseFloat(epsilon_string);
            }

            gw = cmd.getOptionValue("gw");
            hw = cmd.getOptionValue("hw");
            saving_json = cmd.hasOption("sjr");
            hh_pruning = cmd.hasOption("hh");
            print_trace = cmd.hasOption("pt");
            save_plan = cmd.hasOption("sp");
            ignore_metric = cmd.hasOption("im");
            anyTime = cmd.hasOption("anytime");
            aibrPreprocessing = !cmd.hasOption("dap");

        } catch (ParseException exp) {
//            Logger.getLogger(ENHSP.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("enhsp", options);
            System.exit(-1);
        }

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
        if (planner != null) {
            switch (planner) {
                case "sat-hadd"://this is the version used for ijcai-16
                    System.out.println("GBFS with numeric hadd");
                    heuristic = "hadd";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break_ties = "smaller_g";
                    break;
                case "sat-hradd"://this is the version used for ijcai-16
                    System.out.println("GBFS with numeric hadd and redundant constraints");
                    heuristic = "hradd";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                                        break_ties = "smaller_g";
                    break;
                case "sat-haddabs"://this is the version used for ijcai-16
                    System.out.println("GBFS with effect-abstraction heuristic");
                    heuristic = "haddabs";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break_ties = "smaller_g";
                    break;
                case "sat-aibr":// this is the version used in the ecai-16 paper
                    System.out.println("A* with aibr");
                    heuristic = "aibr";
                    gw = "1";
                    hw = "1";
                    search_engine = "WAStar";
                    break;
                
                case "opt-hrmax":// this is the version used in the ijcai-16 paper
                    System.out.println("A* with numeric hrmax");
                    heuristic = "hrmax";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "WAStar";
                    break;
                case "opt-hmax":// this is the version used in the ijcai-16 paper
                    System.out.println("A* with numeric hrmax");
                    heuristic = "hmax";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "WAStar";
                    break;
                case "opt-blind":
                    System.out.println("A* with 0-1 goal heuristic");
                    heuristic = "blind";
                    aibrPreprocessing = false;
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "WAStar";
                    break;
                case "opt-hlm":
                    System.out.println("A* with light numeric landmarks (no redundant constraints no dominance analysis");
                    heuristic = "lm_actions";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "WAStar";
                    break;
                case "opt-hlmrc"://this is the version used in the ijcai-17 paper on landmarks
                    System.out.println("A* with redundant constraints numeric landmarks");
                    heuristic = "lm_actions_rc";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "WAStar";
                    break;
                default:
                    System.out.println("! ====== ! Warning: Unknown planner configuration. Going with default: wastar with aibr ! ====== !");
                    heuristic = "aibr";
                    search_engine = "WAStar";
                    break;
            }
        }

        if (debug == 11) {
            System.out.println("Before Reachability: " + getProblem().actions);
        }
        System.out.println("Simplification..");
        problem.setAction_cost_from_metric(!ignore_metric);
        getProblem().simplifyAndSetupInit(true, aibrPreprocessing);
        if (copyOfTheProblem) {
            heuristicProblem.setAction_cost_from_metric(!ignore_metric);
            heuristicProblem.simplifyAndSetupInit();
        }

        System.out.println("Grounding and Simplification finished");
        System.out.println("|A|:" + getProblem().getActions().size());
        System.out.println("|P|:" + getProblem().getProcessesSet().size());
        System.out.println("|E|:" + getProblem().getEventsSet().size());
        System.out.println("Size(X):" + problem.getNumberOfNumericVariables());
        System.out.println("Size(F):" + problem.getNumberOfBooleanVariables());
        if (pddlPlus) {
            System.out.println("Delta time heuristic model:" + delta_t_h);
            System.out.println("Delta time planning model:" + delta_max);
            System.out.println("Delta time search-execution model:" + delta_t);
            System.out.println("Delta time validation model:" + delta_val);
        }
//        System.out.println(problem.getActions());
//        Set<String> subgoaling_based_heuristic = new HashSet<String>(Arrays.asList("h1","hmax_ref","h1_ref", "h1_5","h1i","lm_actions","hmax","hmmax","h1gi","lm_actions_rc","lm_actions_dc","lm_actions_rc_dc","hm_max","hm_add","hmaxnr"));

        h = null;
        //next is highly customized configuration
        if (getHeuristicFunction() != null) {
            h = getHeuristicFunction();
        } else {

            switch (heuristic) {

                case "hadd_exp": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    break;
                }
                case "hadd": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    break;
                }
                case "hadd_ni": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = true;
                    break;
                }
                case "hadd_ni_no_cost": {
                    h = new h1(getProblem(), true);
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = true;
                    break;
                }
                case "hff": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;

                    break;
                }
                case "hff_agg": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).aggressiveRelaxedPlan = true;

                    break;
                }
                case "hff_agg_rc": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).aggressiveRelaxedPlan = true;

                    break;
                }
                case "hff_max": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    ((h1) h).additive_h = false;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    
                    break;
                }
                case "hff_max_agg": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    ((h1) h).additive_h = false;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).aggressiveRelaxedPlan = true;
                    break;
                }
                case "hff_max_agg_rc": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true ;
                    ((h1) h).additive_h = false;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).aggressiveRelaxedPlan = true;
                    break;
                }
                case "hff_max_rc": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    ((h1) h).additive_h = false;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    break;
                }
                case "hff_max_rc_agg": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    ((h1) h).additive_h = false;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).aggressiveRelaxedPlan = true;
                    break;
                }
                case "hff_pp": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    ((h1) h).additive_h = true;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).only_mutual_exclusion_processes = true;
                    break;
                }
                case "hff_pp_rc": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    ((h1) h).only_mutual_exclusion_processes = true;
                    break;
                }
                case "hff_rc": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = false;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    break;
                }
                case "hff_ni": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = true;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    break;
                }
                case "hff_ni_rc": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    h.additive_h = true;
                    ((h1) h).ibrDisabled = true;
                    ((h1) h).extractRelaxedPlan = true;
                    
                    break;
                }
                case "hiadd": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = false;
                    h.additive_h = true;
                    ((h1) h).integer_actions = true;
                    break;
                }
                case "hradd": {
                    h = new h1(getProblem());
                    ((h1) h).useRedundantConstraints = true;
                    h.additive_h = true;
                    break;
                }
                case "hrmax": {
                    h = new h1(getProblem());
                    ((h1) h).additive_h = false;
                    ((h1) h).useRedundantConstraints = true;
                    ((h1) h).conservativehmax = false;//this corresponds to ijcai-16 version
                    break;
                }
                case "hrmax_cons": {
                    h = new h1(getProblem());
                    ((h1) h).additive_h = false;
                    ((h1) h).useRedundantConstraints = true;
                    ((h1) h).conservativehmax = true;//this corresponds to ijcai-16 version
                    break;
                }
                case "hmax": {
                    h = new h1(getProblem());
                    ((h1) h).additive_h = false;
                    ((h1) h).useRedundantConstraints = false;
                    ((h1) h).conservativehmax = false;//this corresponds to ijcai-16 version
                    break;
                }
                case "hmax_cons": {
                    h = new h1(getProblem());
                    ((h1) h).additive_h = false;
                    ((h1) h).useRedundantConstraints = false;
                    ((h1) h).conservativehmax = true;//this corresponds to ijcai-16 version
                    break;
                }
                case "aibr": {
                    h = new Aibr(heuristicProblem);
                    ((Aibr) h).set(false, true);
                    break;
                }

                case "aibr_cons": {
                    h = new Aibr(heuristicProblem);
                    ((Aibr) h).set(true, true);
                    break;
                }
                case "hm_max": {

                    h = new quasi_hm(heuristicProblem);
                    //h.additive_h = true;
                    h.additive_h = false;
                    ((quasi_hm) h).greedy = false;
                    break;
                }
                case "hm_max_gr": {
                    h = new quasi_hm(heuristicProblem);
                    h.additive_h = false;
                    ((quasi_hm) h).greedy = true;
                    break;
                }
                case "hm_add": {
                    h = new quasi_hm(heuristicProblem);
                    h.additive_h = true;
                    ((quasi_hm) h).greedy = false;
                    break;
                }
                case "hm_add_gr": {
                    h = new quasi_hm(heuristicProblem);
                    h.additive_h = true;
                    ((quasi_hm) h).greedy = true;
                    break;
                }

                case "lm_actions": {
                    //ssearchEngine.setupHeuristic(new hlm(heuristicProblem));
                    h = new hlm(heuristicProblem);
                    ((hlm) h).lp_cost_partinioning = true;
                    break;
                }
                case "lm_actions_rc": {

                    h = new hlm(heuristicProblem);
                    ((hlm) h).lp_cost_partinioning = true;
                    ((hlm) h).useRedundantConstraints = true;
                    ((hlm) h).red_constraints = true;
                    //lm.lp_cost_partinioning = true;
                    break;
                }

                case "lm_actions_dc": {
                    h = new hlm(heuristicProblem);
                    ((hlm) h).smart_intersection = true;
                    ((hlm) h).lp_cost_partinioning = true;
                    break;
                }
                case "lm_actions_rc_dc": {
                    h = new hlm(heuristicProblem);
                    ((hlm) h).smart_intersection = true;
                    ((hlm) h).red_constraints = true;
                    ((hlm) h).lp_cost_partinioning = true;
                    break;
                }
                case "lm_actions_mip": {
                    h = new hlm(heuristicProblem);
                    ((hlm) h).mip = true;
                    ((hlm) h).lp_cost_partinioning = true;
                    break;
                }
                case "haddabs": {
                    //searchEngine.setupHeuristic(new habs_add(heuristicProblem, Integer.MAX_VALUE));
                    h = new habs_add(heuristicProblem, Integer.MAX_VALUE);
                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;
                    ((habs_add) h).midPointSampling = true;
                    ((habs_add) h).planExtraction = false;
                    break;
                }
                case "hffabs": {
                    h = new habs_add(heuristicProblem, Integer.MAX_VALUE);
                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;
                    ((habs_add) h).midPointSampling = true;
                    ((habs_add) h).planExtraction = true;
                    break;
                }

                case "haddabs2": {
                    h = new habs_add(heuristicProblem, 2);

                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;
                    break;
                }
                case "haddabsk": {
                    h = new habs_add(heuristicProblem, num_of_subdomains);
                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;

                    break;
                }
                case "haddabsonline": {
                    h = new habs_add(heuristicProblem, num_of_subdomains);
                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;
                    ((habs_add) h).onlineRepresentatives = true;

                    break;
                }
                case "haddabsmidpoint": {
                    h = new habs_add(heuristicProblem, Integer.MAX_VALUE);
                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;
                    ((habs_add) h).midPointSampling = true;

                    break;
                }
                case "haddabsmidkpoint": {
                    h = new habs_add(heuristicProblem, num_of_subdomains);
                    ((habs_add) h).setMetric(heuristicProblem.getMetric());
                    ((habs_add) h).additive_h = true;
                    ((habs_add) h).midPointSampling = true;

                    break;
                }

                case "blind": {
                    h = new blindHeuristic(heuristicProblem);
                    break;
                }
                case "gc": {
                    h = new GoalCounting(heuristicProblem);
                    break;
                }
                case "gce": {
                    h = new GoalCounting(heuristicProblem, true);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private SimplePlan search() throws Exception {

        LinkedList rawPlan = null;//raw list of actions returned by the search strategies

        final PDDLSearchEngine searchEngine = new PDDLSearchEngine(h); //manager of the search strategies
        Runtime.getRuntime().addShutdownHook(new Thread() {//this is to save json also when the planner is interrupted
            @Override
            public void run() {
                if (saving_json) {
                    searchEngine.searchSpaceHandle.print_json(getProblem().getPddlFileReference() + ".sp_log");
                }
            }
        });
        if (pddlPlus) {
            searchEngine.executionDelta = Float.parseFloat(delta_t);
            searchEngine.processes = true;
            searchEngine.planningDelta = Float.parseFloat(delta_max);
        }

        if (debug > 0) {
            searchEngine.getHeuristic().debug = debug;
            searchEngine.debugLevel = debug;
        }
        searchEngine.saveSearchTreeAsJson = saving_json;

        if (break_ties != null) {
            switch (break_ties) {
                case "smaller_g":
                    searchEngine.tbRule = SearchEngine.TieBreaking.LOWERG;
                    break;
                case "larger_g":
                    searchEngine.tbRule = SearchEngine.TieBreaking.HIGHERG;
                    break;
                default:
                    System.out.println("Wrong setting for break-ties. Arbitrary tie breaking");
                    break;
            }
        } else {//the following is the arbitrary setting
            break_ties = "arbitrary";
            searchEngine.tbRule = SearchEngine.TieBreaking.ARBITRARY;

        }

        if (hw != null) {
            searchEngine.setWH(Float.parseFloat(hw));
            System.out.println("w_h set to be " + hw);
        } else {
            searchEngine.setWH(1);
        }
        if (gw != null) {
            searchEngine.setWG(Float.parseFloat(gw));
            System.out.println("g_h set to be " + gw);
        } else {
            searchEngine.setWG(1);

        }

        if (depthLimit != Float.NaN) {
            searchEngine.depthLimit = depthLimit;
            System.out.println("Setting horizon to:" + depthLimit);
        } else {
            searchEngine.depthLimit = Float.POSITIVE_INFINITY;
        }

        searchEngine.helpfulActionsPruning = hh_pruning;
        searchEngine.getHeuristic().helpful_actions_computation = hh_pruning;

        if ("ehc".equals(search_engine)) {
            System.out.println("Running Enforced Hill Climbing (BFS)");
            searchEngine.forgettingEhc = true;
            rawPlan = searchEngine.enforced_hill_climbing(getProblem());
        } else if ("uehc".equals(search_engine)) {
            System.out.println("Running Uniform Search Enforced Hill Climbing (BFS)");
            searchEngine.forgettingEhc = true;
            rawPlan = searchEngine.enforced_hill_climbing(getProblem(), SearchEngine.Explorator.WASTAR);
        } else if ("ehc_ha".equals(search_engine)) {
            System.out.println("Running Enforced Hill Climbing (BFS) with Helpful Actions");
            searchEngine.getHeuristic().helpful_actions_computation = true;
            searchEngine.helpfulActionsPruning = true;
            rawPlan = searchEngine.enforced_hill_climbing(getProblem());
        } else if ("ehc_dfs".equals(search_engine)) {
            System.out.println("Running Enforced Hill Climbing (DFS)");
            searchEngine.bfsTieBreaking = false;
            rawPlan = searchEngine.enforced_hill_climbing(getProblem());
        } else if ("WAStar".equals(search_engine)) {
            System.out.println("Running WA-STAR");
            rawPlan = searchEngine.WAStar(getProblem(), timeOut);
        } else if ("wa_star_4".equals(search_engine)) {
            System.out.println("Running greedy WA-STAR with hw = 4");
            searchEngine.setWH(4);
            rawPlan = searchEngine.WAStar(getProblem());
        } else if ("gbfs".equals(search_engine)) {
            System.out.println("Running Greedy Best First Search");
            if (gw == null) {
                searchEngine.setWG(0);
            }
            rawPlan = searchEngine.greedy_best_first_search(getProblem(), timeOut);
        } else if ("gbfs_ha".equals(search_engine)) {
            System.out.println("Running Greedy Best First Search with Helpful Actions");
            searchEngine.getHeuristic().helpful_actions_computation = true;
            searchEngine.helpfulActionsPruning = true;
            if (gw == null) {
                searchEngine.setWG(0);
            }
            rawPlan = searchEngine.greedy_best_first_search(getProblem(), timeOut);
        } else if ("gbfs_cha".equals(search_engine)) {
            System.out.println("Running Greedy Best First Search with Conservative Helpful Actions");
            searchEngine.getHeuristic().helpful_actions_computation = true;
            searchEngine.getHeuristic().allAchieverActions = true;
            searchEngine.helpfulActionsPruning = true;
            if (gw == null) {
                searchEngine.setWG(0);
            }
            rawPlan = searchEngine.greedy_best_first_search(getProblem());
        } else if ("dfs".equals(search_engine)) {
            System.out.println("Running Depth First Search");
            heuristic = "dfs";
            searchEngine.bfsTieBreaking = false;
            rawPlan = searchEngine.blindSearch(getProblem());
        } else if ("brfs".equals(search_engine)) {
            System.out.println("Running Uniform Cost Search");
            heuristic = "brfs";
            searchEngine.bfsTieBreaking = true;
            rawPlan = searchEngine.blindSearch(getProblem());
        } else if ("ida".equals(search_engine)) {
            System.out.println("IDA* (Experimental)");
            searchEngine.bfsTieBreaking = true;
            rawPlan = searchEngine.idastar(getProblem(), true, Long.MAX_VALUE);
        } else if ("idaMem".equals(search_engine)) {
            System.out.println("IDA* (Experimental) with Memory");
            searchEngine.bfsTieBreaking = true;
            rawPlan = searchEngine.idastar(getProblem(), true, false, true, Long.MAX_VALUE);
        } else if ("dfsbb".equals(search_engine)) {
            System.out.println("DFSBnB* (Experimental)");
            searchEngine.bfsTieBreaking = true;
            rawPlan = searchEngine.dfsbnb(getProblem());
        } else if ("dfsbbm".equals(search_engine)) {
            System.out.println("DFSBnB* (Experimental)");
            searchEngine.bfsTieBreaking = true;
            rawPlan = searchEngine.dfsbnb(getProblem(), true);
        } else if ("manual".equals(search_engine)){
//            
//            SimplePlan sp = new SimplePlan(domain, getProblem(), false, pddlPlus);
//            sp.parseSolution(inputPlan);
//            rawPlan = sp
        }else{
            System.out.println("Strategy is not correct");
            System.exit(-1);
        }

        overal_planning_time = (System.currentTimeMillis() - very_start);
        SimplePlan sp = validate(searchEngine, rawPlan);
        printInfo(sp, searchEngine);
        return sp;
    }

    private SimplePlan validate(PDDLSearchEngine searchEngine, LinkedList raw_plan) throws CloneNotSupportedException, Exception {
        SimplePlan sp = new SimplePlan(domain, getProblem(), false, pddlPlus);  //placeholder for the plan to be found
        PDDLState lastState = null;
        System.out.println("Starting Validation");
        if (raw_plan != null) {// Print some useful information on the outcome of the planning process
            sp.print_trace = print_trace;
            if (!pddlPlus) {
                sp.addAll(raw_plan);
                lastState = sp.execute((PDDLState) getProblem().getInit(), getProblem().globalConstraints);
                System.out.println("(Pddl2.1 semantics) Plan is valid:" + lastState.satisfy(getProblem().getGoals()));
            } else { //This is when you have also autonomous processes going on
                PddlDomain validationDomain = new PddlDomain(domainFile);
                EPddlProblem validationProblem = new EPddlProblem(problemFile, validationDomain.getConstants(), validationDomain.getTypes(),validationDomain);
                //this is when you have processes
                validationProblem.groundingActionProcessesConstraints();
//                validationProblem.syncAllVariablesAndUpdateCollections(getProblem());
                validationProblem.setDeltaTimeVariable(delta_val);
                validationProblem.simplifyAndSetupInit(true);
                Float time = sp.build_pddl_plus_plan(raw_plan, epsilon);
                lastState = sp.execute((PDDLState) validationProblem.getInit(), validationProblem.globalConstraints, validationProblem.getProcessesSet(), validationProblem.getEventsSet(), searchEngine.planningDelta, Float.parseFloat(delta_val), time);
//                System.out.println("Last PDDLState:"+last_state.pddlPrint());
                boolean goal_reached = lastState.satisfy(getProblem().getGoals());
                System.out.println("(Pddl+ semantics) Plan is valid:" + goal_reached);
            }
        }else{
            return null;
        }
        if (lastState != null) {
            if (!pddlPlus) {
                sp.setDuration(sp.size());
            } else {
                sp.setDuration(lastState.time);//                System.out.println("Duration Via Simulation:"+String.format("%.7f",last_state.getTime().getNumber()));
            }
        }
        return sp;
    }

    private void printInfo(SimplePlan sp, PDDLSearchEngine searchEngine) {

        if (sp != null) {
            System.out.println("Problem Solved");
            if (pddlPlus) {
                System.out.println(sp.printPDDLPlusPlan());
            } else {
                System.out.println(sp);
            }
            System.out.println("Plan-Length:" + sp.size());
            System.out.println("Duration:" + sp.getDuration());
            System.out.println("Metric (Plan):" + sp.getCost());
            if (save_plan) {
                sp.savePlan(getProblem().getPddlFileReference() + "_c_" + heuristic + "_gw_" + gw + "_hw_" + gw + "_delta_" + delta_t + ".plan", true);
            }
            if (print_trace) {
                FileWriter file = null;
                try {
                    file = new FileWriter(getProblem().getPddlFileReference() + "_search_" + search_engine + "_h_" + heuristic + "_break_ties_" + break_ties + ".npt");
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
        System.out.println("Planning Time:" + overal_planning_time);
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
        if (saving_json) {
            searchEngine.searchSpaceHandle.print_json(getProblem().getPddlFileReference() + ".sp_log");
        }
    }
}

/**
 * *******************************************************************
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 ********************************************************************
 */
/**
 * *******************************************************************
 * Description: Expressive Numeric Heuristic Search Planner. 
 *
 * Author: Enrico Scala 2016 Contact: enricos83@gmail.com
 *
 ********************************************************************
 */

import conditions.NumFluentAssigner;
import domain.PddlDomain;
import extraUtils.Utils;
import java.util.LinkedList;
import plan.SimplePlan;
import problem.EPddlProblem;
import problem.State;
import search.SearchStrategies;
import heuristics.advanced.Uniform_cost_search_H1;
import heuristics.advanced.asymptotic_ibr;
import heuristics.Aibr;
import heuristics.advanced.Bellman_Ford_Hm;
import heuristics.advanced.Uniform_cost_search_H1_RC;
import heuristics.advanced.Uniform_cost_search_HM;
import heuristics.advanced.landmarks_factory;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import search.SearchNode;

public class ENHSP {

    private static String domainFile;
    private static String problemFile;
    private static String search_engine;
    private static String hw;
    private static String config = "1";
    private static String gw;
    private static int debug_level = 0;
    private static boolean dec_heuristic = false;
    private static boolean greedy_bf = false;
    private static boolean max_red_constraint = false;
    private static boolean saving_json = false;
    private static String delta_t;
    private static String horizon;
    private static float resolution_execution;
    private static boolean save_plan;
    private static Boolean admissible;
    private static boolean print_trace;
    private static String break_ties;
    private static String planner;

    /**
     * @param args the command line arguments
     */
    public static void parseInput(String[] args) {
        //Eseguibile -o domain -f problem -s solution -r tipo-repair 
        String usage = "usage:\n executable-name(java -jar...) "
                + "\n-o domain -f problem "
                + "\n Planner configuration: -planner <string> (options: ssnp_sat, ssnp_opt, hp_all, hp_rp)"
                + "\n                                ssnp_sat is a satisficing planner using numeric h_add"
                + "\n                                ssnp_opt is a cost-optimal planner using numeric h_max"
                + "\n                                hp_all is a planner with autonomous processes using pure AIBR heuristic"
                + "\n                                hp_rp is a planner with autonomous processes using AIBR and relaxed plan extraction heuristic"
                + "\n                                Note: for planning with autonomus processes, it is assumed delta_t = 1. To change that use -delta parameter below"
                + "\n                        -delta <float> (specify the delta used to approximate the passage of time). This can be used when hp_* planner is used "
                + "\n\n Low-level configurations, aka build your planner (not to be used in combination with above planner (-planner) configuration: "
                + "\n             -s <hc (Hill Climbing), wa_star (h_w = 4), gbfs (GreedyBestFirstSearch, g_w = 0), bfs (A* without node reopening), brfs, dfs"
                + "\n             -gw <float> (weight for the g-values, overrides previous setting)"
                + "\n             -hw <float> (weight for the h-values, overrides previous setting)"
                + "\n             -h <integer/string> select heuristic to be used. Look into the code for more information."
                + "\n             -horizon <integer> (depth of the search tree)"
                + "\n             -break_ties larger_g, smaller_g (default: Arbitrary)"
                + "\n             -adm (Admissible setting; default no)"
                + "\n\n Other ones:"
                + "\n             -sjr (activate search tree saving in jason file)"
                + "\n             -sp  (save plan)";

        if (args.length < 4) {
            System.err.println("Number of parameters is lower than expected (" + args.length + ")");
            System.err.println(usage);
            System.exit(-1);
        } else {
            domainFile = Utils.searchParameterValue(args, "-o");
            problemFile = Utils.searchParameterValue(args, "-f");
            planner = Utils.searchParameterValue(args, "-planner");
            search_engine = Utils.searchParameterValue(args, "-s");
            hw = Utils.searchParameterValue(args, "-hw");
            gw = Utils.searchParameterValue(args, "-gw");
            delta_t = Utils.searchParameterValue(args, "-delta");
            horizon = Utils.searchParameterValue(args, "-horizon");
            config = Utils.searchParameterValue(args, "-h");
            dec_heuristic = Utils.searchParameter(args, "-dec"); //only decreasing values of heuristic
            greedy_bf = Utils.searchParameter(args, "-gbf"); //greedy bellman ford -- obsolete
            max_red_constraint = Utils.searchParameter(args, "-mrc"); //Max Redundant Constraints -- obsolete
            saving_json = Utils.searchParameter(args, "-sjr"); //Save the search tree in a jason file
            save_plan = Utils.searchParameter(args, "-sp"); //Save the plan
            admissible = Utils.searchParameter(args, "-adm"); //Save the plan
            print_trace = Utils.searchParameter(args, "-print_trace"); //print_trace
            break_ties = Utils.searchParameterValue(args, "-break_ties"); //print_trace
            String res_validation = Utils.searchParameterValue(args, "-exec_res"); //Resolution for the validation
            if (res_validation != null) {
                resolution_execution = Float.parseFloat(res_validation);
            } else {
                resolution_execution = 1f;
            }
            if (delta_t == null) {
                delta_t = "1";
            }

            if (domainFile == null || problemFile == null) {
                System.err.println(usage);
                System.exit(-1);
            }
            if (config == null) {
                config = "aibr";
            }

        }
    }

    public static void main(String[] args) throws Exception {

        parseInput(args);
        PddlDomain domain = new PddlDomain(domainFile);
        System.out.println("Domain Parsed");
        final EPddlProblem problem = new EPddlProblem(problemFile, domain.getConstants());
        final EPddlProblem validation_problem = new EPddlProblem(problemFile, domain.getConstants());

        System.out.println("Problem parsed");
        domain.validate(problem);
        System.out.println("Light Validation Completed");

        SimplePlan sp = new SimplePlan(domain, validation_problem);  //placeholder for the plan to be found
        final SearchStrategies searchStrategies = new SearchStrategies(); //manager of the search strategies

        //The following add a handler for storing the search tree in a jason file, if specified.
        Runtime.getRuntime().addShutdownHook(new Thread() {//this is to save json also when the planner is interrupted
            @Override
            public void run() {
                if (saving_json) {
                    searchStrategies.search_space_handle.print_json(problem.getPddlFileReference() + ".sp_log");
                }
            }
        });
        LinkedList raw_plan = null;//raw list of actions returned by the search strategies
        if (!domain.getProcessesSchema().isEmpty()) {//this is when you have processes
            problem.getInit().addNumericFluent(new NumFluentAssigner("#t", Float.parseFloat(delta_t))); //this is the discretisation factor
            problem.getInit().addNumericFluent(new NumFluentAssigner("time_elapsed", 0));//this is the clock variable
            searchStrategies.delta = Float.parseFloat(delta_t);
            searchStrategies.processes = true;
        }
        System.out.println("Grounding..");
        problem.generateActionsAndProcesses();
        problem.generateConstraints();
        problem.transform_numeric_condition();
//        problem.prettyPrint();
//        System.out.println(problem.globalConstraints.pddlPrint(true));
        System.out.println("Grounding and Simplification finished");
        System.out.println("|A|:" + problem.getActions().size());
        System.out.println("|P|:" + problem.processesSet.size());

        if (planner != null) {
            searchStrategies.breakties_on_larger_g = false;
            searchStrategies.breakties_on_smaller_g = false;
            switch (planner) {
                case "ssnp_sat":
                    System.out.println("GBFS with numeric h1");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions()));
                    searchStrategies.getHeuristic().additive_h = true;
                    searchStrategies.getHeuristic().greedy = false;
                    searchStrategies.getHeuristic().integer_actions = false;
                    searchStrategies.set_w_g(0);
                    searchStrategies.set_w_h(1);
                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                case "sssnp_sat":
                    System.out.println("GBFS with numeric h1 in a greedier version");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions()));
                    searchStrategies.getHeuristic().additive_h = true;
                    searchStrategies.getHeuristic().greedy = true;
                    searchStrategies.getHeuristic().integer_actions = false;

                    searchStrategies.set_w_g(0);
                    searchStrategies.set_w_h(1);
                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                case "sssinp_sat":
                    System.out.println("GBFS with numeric h1 in a greedier version");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions()));
                    searchStrategies.getHeuristic().additive_h = true;
                    searchStrategies.getHeuristic().greedy = true;
                    searchStrategies.getHeuristic().integer_actions = true;

                    searchStrategies.set_w_g(0);
                    searchStrategies.set_w_h(1);
                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                case "ssrnp_sat":
                    System.out.println("GBFS with numeric h1_5");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1_RC(problem.getGoals(), problem.getActions()));
                    searchStrategies.getHeuristic().additive_h = true;
                    searchStrategies.set_w_g(0);
                    searchStrategies.set_w_h(1);

                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                case "ssnp_opt":
                    System.out.println("A* with numeric hmax");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1_RC(problem.getGoals(), problem.getActions()));
                    searchStrategies.getHeuristic().additive_h = false;
                    searchStrategies.set_w_g(1);
                    searchStrategies.set_w_h(1);
                    searchStrategies.breakties_on_larger_g = true;

                    raw_plan = searchStrategies.wa_star(problem);
                    break;
                case "bfs_opt":
                    System.out.println("A* with numeric hmax");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1_RC(problem.getGoals(), problem.getActions()));
                    searchStrategies.getHeuristic().additive_h = false;
                    searchStrategies.set_w_g(1);
                    searchStrategies.set_w_h(1);
                    searchStrategies.breakties_on_larger_g = true;

                    raw_plan = searchStrategies.blindSearch(problem);
                    break;
                case "hmnp_opt":
                    System.out.println("A* with numeric hmax plus global consistency");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_HM(problem.getGoals(), problem.getActions(), problem.processesSet, problem.globalConstraints));
                    searchStrategies.getHeuristic().additive_h = false;
                    searchStrategies.set_w_g(1);
                    searchStrategies.set_w_h(1);
                    searchStrategies.breakties_on_larger_g = true;
                    raw_plan = searchStrategies.wa_star(problem);
                    break;
                case "hmnp_sat":
                    System.out.println("GBFS with numeric hmax plus global consistency");
                    searchStrategies.setup_heuristic(new Uniform_cost_search_HM(problem.getGoals(), problem.getActions(), problem.processesSet, problem.globalConstraints));
                    searchStrategies.getHeuristic().additive_h = false;
                    searchStrategies.set_w_g(0);
                    searchStrategies.set_w_h(1);
//                    searchStrategies.breakties_on_larger_g = true;
                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                case "hp_all":
                    System.out.println("A* with all actions AIBR heuristic");
                    searchStrategies.setup_heuristic(new Aibr(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Aibr h = (Aibr) searchStrategies.getHeuristic();
                    h.set(false, true);
                    searchStrategies.set_w_g(1);
                    searchStrategies.set_w_h(1);
                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                case "hp_rp":
                    System.out.println("GBFS with relaxed plan AIBR heuristic");
                    searchStrategies.setup_heuristic(new Aibr(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Aibr heur = (Aibr) searchStrategies.getHeuristic();
                    heur.set(false, false);
                    heur.extract_plan = true;
                    searchStrategies.set_w_g(1);
                    searchStrategies.set_w_h(1);
                    raw_plan = searchStrategies.greedy_best_first_search(problem);
                    break;
                default:
                    break;
            }
        } else { //next is highly customized configuration
            switch (config) {
                case "h1": {
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Uniform_cost_search_H1 h = (Uniform_cost_search_H1) searchStrategies.getHeuristic();
                    h.additive_h = true;
                    break;
                }
                case "h1i": {
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Uniform_cost_search_H1 h = (Uniform_cost_search_H1) searchStrategies.getHeuristic();
                    h.additive_h = true;
                    h.integer_actions = true;
                    break;
                }
                case "h1g": {
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Uniform_cost_search_H1 h = (Uniform_cost_search_H1) searchStrategies.getHeuristic();
                    h.additive_h = true;
                    h.greedy = true;
                    //h.quasi_integer_actions = true;
                    break;
                }
                case "h1gi": {
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Uniform_cost_search_H1 h = (Uniform_cost_search_H1) searchStrategies.getHeuristic();
                    h.additive_h = true;
                    h.greedy = true;
                    h.integer_actions = true;
                    break;
                }
                case "h1_5": {
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1_RC(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Uniform_cost_search_H1_RC h = (Uniform_cost_search_H1_RC) searchStrategies.getHeuristic();
                    h.additive_h = true;
                    break;
                }
                case "hmax": {
                    //optimal planning setting.. hmax as for the IJCAI-16 paper
                    searchStrategies.setup_heuristic(new Uniform_cost_search_H1_RC(problem.getGoals(), problem.getActions()));
                    Uniform_cost_search_H1_RC h = (Uniform_cost_search_H1_RC) searchStrategies.getHeuristic();
                    h.additive_h = false;
                    break;
                }

                case "aibr": {
                    searchStrategies.setup_heuristic(new asymptotic_ibr(problem.getGoals(), problem.getActions(), problem.processesSet));
                    asymptotic_ibr h = (asymptotic_ibr) searchStrategies.getHeuristic();
                    h.set(false, true);
                    break;
                }
                case "aibr2": {
                    searchStrategies.setup_heuristic(new Aibr(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Aibr h = (Aibr) searchStrategies.getHeuristic();
                    h.set(false, true);
                    h.extract_plan = false;
                    break;
                }
                case "aibr_rp": {
                    System.out.println("20");
                    searchStrategies.setup_heuristic(new Aibr(problem.getGoals(), problem.getActions(), problem.processesSet));
                    Aibr h = (Aibr) searchStrategies.getHeuristic();
                    h.set(false, true);
                    h.extract_plan = true;
                    break;
                }
                case "exp_gc": {
                    searchStrategies.setup_heuristic(new Uniform_cost_search_HM(problem.getGoals(), problem.getActions(), problem.processesSet, problem.globalConstraints));
                    Uniform_cost_search_HM h = (Uniform_cost_search_HM) searchStrategies.getHeuristic();
                    //h.additive_h = true;
                    h.additive_h = false;
                    h.integer_variables = false;
                    h.greedy = false;
                    break;
                }
                default:
                    break;
            }
            searchStrategies.json_rep_saving = saving_json;
            if (break_ties != null) {
                if (break_ties.equals("smaller_g")) {
                    searchStrategies.breakties_on_larger_g = false;
                    searchStrategies.breakties_on_smaller_g = true;
                } else if (break_ties.equals("larger_g")) {
                    searchStrategies.breakties_on_larger_g = true;
                } else {
                    System.out.println("Wrong setting for break-ties. Arbitrary tie breaking");
                }
            } else {//the following is the arbitrary setting
                searchStrategies.breakties_on_larger_g = false;
                searchStrategies.breakties_on_smaller_g = false;

            }

            if (hw != null) {
                searchStrategies.set_w_h(Float.parseFloat(hw));
                System.out.println("w_h set to be " + hw);
            } else {
                searchStrategies.set_w_h(1);
            }
            if (gw != null) {
                searchStrategies.set_w_g(Float.parseFloat(gw));
                System.out.println("g_h set to be " + gw);
            } else {
                searchStrategies.set_w_g(1);

            }

            if (horizon != null) {
                searchStrategies.horizon = Integer.parseInt(horizon);
                System.out.println("Setting horizon to:" + horizon);
            } else {
                searchStrategies.horizon = Integer.MAX_VALUE;
            }

            if ("hc".equals(search_engine)) {
                raw_plan = searchStrategies.enforced_hill_climbing(problem);
            } else if ("hc_dfs".equals(search_engine)) {
                searchStrategies.bfs = false;
                raw_plan = searchStrategies.enforced_hill_climbing(problem);
            } else if ("bfs".equals(search_engine) || search_engine == null) {
                searchStrategies.preferred_operators_active = false;
                raw_plan = searchStrategies.greedy_best_first_search(problem);
            } else if ("wa_star".equals(search_engine)) {
                System.out.println("Running WA-STAR");
                raw_plan = searchStrategies.wa_star(problem);
            } else if ("gbfs".equals(search_engine)) {
                System.out.println("Running GBFS");
                raw_plan = searchStrategies.greedy_best_first_search(problem);
            } else if ("dfs".equals(search_engine)) {
                config = "brfs";
                searchStrategies.bfs = false;
                raw_plan = searchStrategies.blindSearch(problem);
            } else if ("brfs".equals(search_engine)) {
                config = "brfs";
                searchStrategies.bfs = true;
                raw_plan = searchStrategies.blindSearch(problem);
            } else {
                System.out.println("Strategy is not correct");
                System.exit(-1);
            }
        }
        if (raw_plan != null) {// Print some useful information on the outcome of the planning process
            System.out.println("Problem Solved");
            sp.print_trace = print_trace;
            State last_state = null;
            if (problem.processesSet.isEmpty()) {
                sp.addAll(raw_plan);
                last_state = sp.execute(problem.getInit(), problem.globalConstraints);
                System.out.println("(Pddl2.1 semantics) Plan is valid:" + last_state.satisfy(problem.getGoals()));
                System.out.println(sp);
                System.out.println("Plan-Length:" + sp.size());
            } else {//This is when you have also autonomous processes going on
                sp.build_pddl_plus_plan(raw_plan, Float.parseFloat(delta_t), 0.0000001f);
                last_state = sp.execute(problem.getInit(), problem.globalConstraints, problem.processesSet, searchStrategies.delta, resolution_execution);
                System.out.println("(Pddl+ semantics) Plan is valid:" + last_state.satisfy(problem.getGoals()));
                System.out.println(sp);
                System.out.println("Plan-Length:" + sp.size());
            }
            if (print_trace) {
                FileWriter file = null;
                try {
                    file = new FileWriter(problem.getPddlFileReference() + ".npt");
                    //System.out.println(this.json_rep.toJSONString());
                    file.write(sp.numeric_plan_trace.toJSONString());
                    file.close();
                } catch (IOException ex) {
                    Logger.getLogger(SearchNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Numeric Plan Trace saved.");
            }
            if (save_plan) {
                sp.savePlan(problem.getPddlFileReference() + "_c_" + config + "_gw_" + gw + "_hw_" + gw + "_delta_" + delta_t + ".plan", true);
            }
            if (problem.getMetric() != null && problem.getMetric().getMetExpr() != null) {
                System.out.println("Metric-Value:" + problem.getMetric().getMetExpr().eval(last_state));
            }

        } else {
            System.out.println("Problem Unsolvable");
        }

        System.out.println("Heuristic Time:" + SearchStrategies.heuristic_time);
        System.out.println("Planning Time:" + SearchStrategies.overall_search_time);
        System.out.println("Expanded Nodes:" + SearchStrategies.nodes_expanded);
        System.out.println("States Evaluated:" + SearchStrategies.states_evaluated);
        System.out.println("Total Cost:" + sp.cost);
        System.out.println("Priority Queue Size:" + SearchStrategies.priority_queue_size);
        System.out.println("Number of Dead-Ends detected:" + SearchStrategies.num_dead_end_detected);
        System.out.println("Number of duplicates detected:" + SearchStrategies.number_duplicates);
        System.out.println("Number of Nodes re-opened:" + SearchStrategies.node_reopened);
        System.out.println("Number of LP invocations:" + searchStrategies.getHeuristic().invocation);

        if (saving_json) {
            searchStrategies.search_space_handle.print_json(problem.getPddlFileReference() + ".sp_log");
        }

    }

}

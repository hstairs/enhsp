package utils;

import com.hstairs.ppmajal.heuristics.Aibr;
import com.hstairs.ppmajal.heuristics.Heuristic;
import com.hstairs.ppmajal.heuristics.advanced.*;
import com.hstairs.ppmajal.heuristics.blindHeuristic;
import com.hstairs.ppmajal.problem.EPddlProblem;

public class Planner {

    public String description;
    public String heuristicString;
    public Float gw;
    public Float hw;
    public String searchEngine;
    public String breakTies;
    public Boolean aibrPreprocessing = false;
    public Heuristic heuristic;

    public Planner(ArgParams args){
        String key = args.planner == null ? "default" : args.planner;
        switch (key) {
            case "sat-hadd"://this is the version used for ijcai-16
                init("GBFS with numeric hadd", "hadd", 0.0f, 1.0f, "gbfs", "smaller_g");
                break;
            case "sat-hradd"://this is the version used for ijcai-16
                init("GBFS with numeric hadd and redundant constraints", "hradd", 0.0f, 1.0f, "gbfs", "smaller_g");
                break;
            case "haddabs"://this is the version used for ijcai-16
                init("GBFS with effect-abstraction heuristic", "haddabs", 0.0f, 1.0f, "gbfs", "smaller_g");
                break;
            case "sat-aibr":// this is the version used in the ecai-16 paper
                init("A* with aibr", "aibr", 1.0f, 1.0f, "WAStar", args.breakTies);
                break;
            case "opt-hrmax":// this is the version used in the ijcai-16 paper
                init("A* with numeric hrmax", "hrmax", 1.0f, 1.0f, "WAStar", "larger_g");
                break;
            case "opt-hmax":// this is the version used in the ijcai-16 paper
                init("A* with numeric hmax", "hmax", 1.0f, 1.0f, "WAStar", "larger_g");
                break;
            case "opt-blind":
                init("A* with 0-1 goal heuristic", "blind", 1.0f, 1.0f, "WAStar", "larger_g", true);
                break;
            case "opt-hlm":
                String descr1 = "A* with light numeric landmarks (no redundant constraints no dominance analysis";
                init(descr1, "lm_actions", 1.0f, 1.0f, "WAStar", "larger_g");
                break;
            case "opt-hlmrc"://this is the version used in the ijcai-17 paper on landmarks
                String descr2 = "A* with redundant constraints numeric landmarks";
                init(descr2, "lm_actions_rc", 1.0f, 1.0f, "WAStar", "larger_g");
                break;
            default:
                init(String.format("Heuristic: %s, SearchEngine: %s, BreakTies: %s", args.heuristic, args.searchEngine, args.breakTies), args.heuristic, args.gw, args.hw, args.searchEngine, args.breakTies);
                break;
        }
    }

    private void init(String description, String heuristics, Float gw, Float hw, String searchEngine, String breakTies, Boolean aibrPreprocessing){
        this.description = description;
        this.heuristicString = heuristics;
        this.gw = gw;
        this.hw = hw;
        this.searchEngine = searchEngine;
        this.breakTies = breakTies;
        this.aibrPreprocessing = aibrPreprocessing;
        System.out.println(this.description);
    }

    private void init(String description, String heuristics, Float gw, Float hw, String searchEngine, String breakTies){
        init(description, heuristics, gw, hw, searchEngine, breakTies, false);
    }

    public Heuristic getHeuristic(EPddlProblem problem, EPddlProblem heuristicProblem, Integer numOfSubdomains) {
        Heuristic h = null;

        switch (this.heuristicString) {
            case "hadd_exp":
            case "hadd": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                break;
            }
            case "hadd_ni": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                ((h1) h).ibrDisabled = true;
                break;
            }
            case "hadd_ni_no_cost": {
                h = new h1(problem, true);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                ((h1) h).ibrDisabled = true;
                break;
            }
            case "hff": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                break;
            }
            case "hff_agg":
            case "hff_agg_rc": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                ((h1) h).aggressiveRelaxedPlan = true;
                break;
            }
            case "hff_max": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                ((h1) h).additive_h = false;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                break;
            }
            case "hff_max_agg": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                ((h1) h).additive_h = false;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                ((h1) h).aggressiveRelaxedPlan = true;
                break;
            }
            case "hff_max_agg_rc":
            case "hff_max_rc_agg": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                ((h1) h).additive_h = false;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                ((h1) h).aggressiveRelaxedPlan = true;
                break;
            }
            case "hff_max_rc": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                ((h1) h).additive_h = false;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                break;
            }
            case "hff_pp": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                ((h1) h).additive_h = true;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                ((h1) h).only_mutual_exclusion_processes = true;
                break;
            }
            case "hff_pp_rc": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                h.additive_h = true;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;
                ((h1) h).only_mutual_exclusion_processes = true;
                break;
            }
            case "hff_rc": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                h.additive_h = true;
                ((h1) h).ibrDisabled = false;
                ((h1) h).extractRelaxedPlan = true;

                break;
            }
            case "hff_ni": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                ((h1) h).ibrDisabled = true;
                ((h1) h).extractRelaxedPlan = true;
                break;
            }
            case "hff_ni_rc": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                h.additive_h = true;
                ((h1) h).ibrDisabled = true;
                ((h1) h).extractRelaxedPlan = true;

                break;
            }
            case "hiadd": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = false;
                h.additive_h = true;
                ((h1) h).integer_actions = true;
                break;
            }
            case "hradd": {
                h = new h1(problem);
                ((h1) h).useRedundantConstraints = true;
                h.additive_h = true;
                break;
            }
            case "hrmax": {
                h = new h1(problem);
                ((h1) h).additive_h = false;
                ((h1) h).useRedundantConstraints = true;
                ((h1) h).conservativehmax = false;//this corresponds to ijcai-16 version
                break;
            }
            case "hrmax_cons": {
                h = new h1(problem);
                ((h1) h).additive_h = false;
                ((h1) h).useRedundantConstraints = true;
                ((h1) h).conservativehmax = true;//this corresponds to ijcai-16 version
                break;
            }
            case "hmax": {
                h = new h1(problem);
                ((h1) h).additive_h = false;
                ((h1) h).useRedundantConstraints = false;
                ((h1) h).conservativehmax = false;//this corresponds to ijcai-16 version
                break;
            }
            case "hmax_cons": {
                h = new h1(problem);
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
                h = new habs_add(heuristicProblem, numOfSubdomains);
                ((habs_add) h).setMetric(heuristicProblem.getMetric());
                ((habs_add) h).additive_h = true;

                break;
            }
            case "haddabsonline": {
                h = new habs_add(heuristicProblem, numOfSubdomains);
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
                h = new habs_add(heuristicProblem, numOfSubdomains);
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
        return h;
    }
}

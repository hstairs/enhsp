package utils;

import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.PddlProblem;
import com.hstairs.ppmajal.search.PDDLSearchEngine;

import java.util.LinkedList;

public class PlannerSearchEngine {

    private PDDLSearchEngine searchEngine;
    private Planner planner;

    public PlannerSearchEngine(Planner planner, ArgParams args, boolean isPDDLPlus){

        this.planner = planner;
        this.searchEngine = new PDDLSearchEngine(planner.heuristic); //manager of the search strategies

        if (isPDDLPlus) {
            searchEngine.executionDelta = args.deltaT;
            searchEngine.processes = true;
            searchEngine.planningDelta = args.deltaMax;
        }

        if (args.debug > 0) {
            searchEngine.getHeuristic().debug = args.debug;
            searchEngine.debugLevel = args.debug;
        }

        searchEngine.saveSearchTreeAsJson = args.savingJSON;

        if (args.breakTies != null) {
            switch (args.breakTies) {
                case "smaller_g":
                    searchEngine.tbRule = com.hstairs.ppmajal.search.SearchEngine.TieBreaking.LOWERG;
                    break;
                case "larger_g":
                    searchEngine.tbRule = com.hstairs.ppmajal.search.SearchEngine.TieBreaking.HIGHERG;
                    break;
                default:
                    System.out.println("Wrong setting for break-ties. Arbitrary tie breaking");
                    break;
            }
        } else {//the following is the arbitrary setting
            args.breakTies = "arbitrary";
            searchEngine.tbRule = com.hstairs.ppmajal.search.SearchEngine.TieBreaking.ARBITRARY;
        }

        searchEngine.setWH(planner.hw);
        System.out.println("w_h set to be " + planner.hw);

        searchEngine.setWG(planner.gw);
        System.out.println("g_h set to be " + planner.gw);

        searchEngine.depthLimit = args.depthLimit;
        System.out.println("Setting horizon to:" + args.depthLimit);

        searchEngine.helpfulActionsPruning = args.hhPruning;
        searchEngine.getHeuristic().helpful_actions_computation = args.hhPruning;
    }

    public PDDLSearchEngine getSearchEngine(){
        return this.searchEngine;
    }

    public LinkedList getRawPlan(EPddlProblem problem, Long timeout) throws Exception {
        LinkedList rawPlan = null;

        switch (this.planner.searchEngine) {
            case "ehc": {
                System.out.println("Running Enforced Hill Climbing (BFS)");
                searchEngine.forgettingEhc = true;
                rawPlan = searchEngine.enforced_hill_climbing(problem);
                break;
            }
            case "uehc": {
                System.out.println("Running Uniform Search Enforced Hill Climbing (BFS)");
                searchEngine.forgettingEhc = true;
                rawPlan = searchEngine.enforced_hill_climbing(problem, com.hstairs.ppmajal.search.SearchEngine.Explorator.WASTAR);
                break;
            }
            case "ehc_ha": {
                System.out.println("Running Enforced Hill Climbing (BFS) with Helpful Actions");
                searchEngine.getHeuristic().helpful_actions_computation = true;
                searchEngine.helpfulActionsPruning = true;
                rawPlan = searchEngine.enforced_hill_climbing(problem);
                break;
            }
            case "ehc_dfs": {
                System.out.println("Running Enforced Hill Climbing (DFS)");
                searchEngine.bfsTieBreaking = false;
                rawPlan = searchEngine.enforced_hill_climbing(problem);
                break;
            }
            case "WAStar": {
                System.out.println("Running WA-STAR");
                rawPlan = searchEngine.WAStar(problem, timeout);
                break;
            }
            case "wa_star_4": {
                System.out.println("Running greedy WA-STAR with hw = 4");
                searchEngine.setWH(4);
                rawPlan = searchEngine.WAStar(problem);
                break;
            }
            case "gbfs": {
                System.out.println("Running Greedy Best First Search");
                rawPlan = searchEngine.greedy_best_first_search(problem, timeout);
                break;
            }
            case "gbfs_ha": {
                System.out.println("Running Greedy Best First Search with Helpful Actions");
                searchEngine.getHeuristic().helpful_actions_computation = true;
                searchEngine.helpfulActionsPruning = true;
                rawPlan = searchEngine.greedy_best_first_search(problem, timeout);
                break;
            }
            case "gbfs_cha": {
                System.out.println("Running Greedy Best First Search with Conservative Helpful Actions");
                searchEngine.getHeuristic().helpful_actions_computation = true;
                searchEngine.getHeuristic().allAchieverActions = true;
                searchEngine.helpfulActionsPruning = true;
                rawPlan = searchEngine.greedy_best_first_search(problem);
                break;
            }
            case "dfs": {
                System.out.println("Running Depth First Search");
                searchEngine.bfsTieBreaking = false;
                rawPlan = searchEngine.blindSearch(problem);
                break;
            }
            case "brfs": {
                System.out.println("Running Uniform Cost Search");
                searchEngine.bfsTieBreaking = true;
                rawPlan = searchEngine.blindSearch(problem);
                break;
            }
            case "ida": {
                System.out.println("IDA* (Experimental)");
                searchEngine.bfsTieBreaking = true;
                rawPlan = searchEngine.idastar(problem, true, Long.MAX_VALUE);
                break;
            }
            case "idaMem": {
                System.out.println("IDA* (Experimental) with Memory");
                searchEngine.bfsTieBreaking = true;
                rawPlan = searchEngine.idastar(problem, true, false, true, Long.MAX_VALUE);
                break;
            }
            case "dfsbb": {
                System.out.println("DFSBnB* (Experimental)");
                searchEngine.bfsTieBreaking = true;
                rawPlan = searchEngine.dfsbnb(problem);
                break;
            }
            case "dfsbbm": {
                System.out.println("DFSBnB* (Experimental)");
                searchEngine.bfsTieBreaking = true;
                rawPlan = searchEngine.dfsbnb(problem, true);
                break;
            }
            default: {
                System.out.println("Strategy is not correct");
                System.exit(-1);
            }
        }

        return rawPlan;
    }

}

package planners;

import com.hstairs.ppmajal.conditions.AndCond;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.pddl.heuristics.BlindHeuristic;
import com.hstairs.ppmajal.pddl.heuristics.advanced.Aibr;
import com.hstairs.ppmajal.pddl.heuristics.advanced.GoalCounting;
import com.hstairs.ppmajal.pddl.heuristics.advanced.H1;
import com.hstairs.ppmajal.pddl.heuristics.advanced.LM;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.PDDLSearchEngine;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.search.SearchEngine;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import com.hstairs.ppmajal.search.SearchHeuristic;
import com.hstairs.ppmajal.transition.TransitionGround;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import java.io.IOException;
import static java.lang.System.out;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private String searchEngineString;
    private String hw;
    private String heuristic = "aibr";
    private String gw;
    private boolean saving_json = false;
    private String deltaExecution;
    private float depthLimit;
    private String savePlan;
    private boolean printTrace;
    private String tieBreaking;
    private String planner;
    private String deltaHeuristic;
    private String deltaPlanning;
    private String deltaValidation;
    private boolean helpfulActionsPruning;
    private Integer numSubdomains;
    private SearchHeuristic heuristicFunction;
    private EPddlProblem problem;
    private boolean pddlPlus;
    private PddlDomain domain;
    private PddlDomain domainHeuristic;
    private EPddlProblem heuristicProblem;
    private long overallStart;
    private boolean copyOfTheProblem;
    private boolean anyTime;
    private long timeOut;
    private boolean aibrPreprocessing;
    private SearchHeuristic h;
    private long overallPlanningTime;
    private float endGValue;
    private boolean helpfulTransitions;
    private boolean internalValidation = false;
    private int planLength;
    private String redundantConstraints;
    private String groundingType;
    private boolean naiveGrounding;
    private boolean stopAfterGrounding;
    private boolean printEvents;
    private boolean sdac;
    private boolean onlyPlan;

    public ENHSP(boolean copyProblem) {
        copyOfTheProblem = copyProblem;
    }

    public int getPlanLength() {
        return planLength;
    }

    public Pair<PddlDomain, EPddlProblem> parseDomainProblem(String domainFile, String problemFile, String delta, PrintStream out) {
        try {
            final PddlDomain localDomain = new PddlDomain(domainFile);
            //domain.substituteEqualityConditions();
            pddlPlus = !localDomain.getProcessesSchema().isEmpty() || !localDomain.eventsSchema.isEmpty();
            out.println("Domain parsed");
            final EPddlProblem localProblem = new EPddlProblem(problemFile, localDomain.getConstants(), localDomain.types, localDomain, out, groundingType, sdac);
            if (!localDomain.getProcessesSchema().isEmpty()) {
                localProblem.setDeltaTimeVariable(delta);
            }
            //this second model is the one used in the heuristic. This can potentially be different from the one used in the execution model. Decoupling it
            //allows us to a have a finer control on the machine
            //the third one is the validation model, where, also in this case we test our plan against a potentially more accurate description
            out.println("Problem parsed");
            out.println("Grounding..");
            localProblem.groundingSimplication(aibrPreprocessing, stopAfterGrounding);
            if (stopAfterGrounding) {
                System.exit(1);
            }
            return Pair.of(localDomain, localProblem);
        } catch (Exception ex) {
            Logger.getLogger(ENHSP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void parsingDomainAndProblem(String[] args) {
        try {
            overallStart = System.currentTimeMillis();
            Pair<PddlDomain, EPddlProblem> res = parseDomainProblem(domainFile, problemFile, deltaExecution, System.out);
            domain = res.getKey();
            problem = res.getRight();
            if (pddlPlus) {
                res = parseDomainProblem(domainFile, problemFile, deltaHeuristic, new PrintStream(new OutputStream() {
                    public void write(int b) {}}));
                domainHeuristic = res.getKey();
                heuristicProblem = res.getRight();
                copyOfTheProblem = true;
            } else {
                heuristicProblem = problem;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void configurePlanner() {
        if (planner != null) {
            setPlanner();
        }
    }

    public void planning() {

        try {
            printStats();
            configureHeuristic();
            do {
                LinkedList sp = search();
                if (sp == null) {
                    return;
                }
                depthLimit = endGValue;
                if (anyTime) {
                    System.out.println("NEW COST ==================================================================================>" + depthLimit);
                }
                sp = null;
                System.gc();
            } while (anyTime);
        } catch (Exception ex) {
            Logger.getLogger(ENHSP.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void parseInput(String[] args) {
        Options options = new Options();
        options.addRequiredOption("o", "domain", true, "PDDL domain file");
        options.addRequiredOption("f", "problem", true, "PDDL problem file");
        options.addOption("planner", true, "Fast Preconfgured Planner. For available options look into the code. This overrides all other parameters but domain and problem specs.");
        options.addOption("h", true, "heuristic: options (default is AIBR):\n"
                + "aibr, Additive Interval Based relaxation heuristic\n"
                + "hadd, Additive version of subgoaling heuristic\n"
                + "hradd, Additive version of subgoaling heuristic plus redundant constraints\n"
                + "hmax, Hmax for Numeric Planning\n"
                + "hrmax, Hmax for Numeric Planning with redundant constraints\n"
                + "hmrp, heuristic based on MRP extraction\n"
                + "blind, goal sensitive heuristic (1 to non goal-states, 0 to goal-states");
        options.addOption("s", true, "allows to select search strategy (default is WAStar):\n"
                + "gbfs, Greedy Best First Search (f(n) = h(n))\n"
                + "WAStar, WA* (f(n) = g(n) + h_w*h(n))\n"
                + "wa_star_4, WA* (f(n) = g(n) + 4*h(n))\n");
        options.addOption("ties", true, "tie-breaking (default is arbitrary): larger_g, smaller_g, arbitrary");
        options.addOption("dp", "delta_planning", true, "planning decision executionDelta: float");
        options.addOption("de", "delta_execuction", true, "planning execution executionDelta: float");
        options.addOption("dh", "delta_heuristic", true, "planning heuristic executionDelta: float");
        options.addOption("dv", "delta_validation", true, "validation executionDelta: float");
        options.addOption("d", "delta", true, "Override other delta_<planning,execuction,validation,heuristic> configurations: float");
        options.addOption("epsilon", true, "epsilon separation: float");
        options.addOption("wg", true, "g-values weight: float");
        options.addOption("wh", true, "h-values weight: float");
        options.addOption("sjr", false, "save state space explored in json file");
        options.addOption("ha", "helpful-actions", true, "activate helpful actions pruning");
        options.addOption("pe", "print-events-plan", false, "activate printing of events");

        options.addOption("ht", "helpful-transitions", true, "activate up-to-macro actions");
        options.addOption("sp", true, "Save plan. Argument is filename");
        options.addOption("pt", false, "print state trajectory (Experimental)");
        options.addOption("im", false, "Ignore Metric in the heuristic");
        options.addOption("dap", false, "Disable Aibr Preprocessing");
        options.addOption("red", "redundant_constraints", true, "Choose mechanism for redundant constraints generation among, "
                + "no, brute and smart. No redundant constraints generation is the default");
        options.addOption("gro", "grounding", true, "Activate grounding via internal mechanism, fd or metricff or internal or naive (default is internal)");

        options.addOption("dl", true, "bound on plan-cost: float (Experimental)");
        options.addOption("k", true, "maximal number of subdomains. This works in combination with haddabs: integer");
        options.addOption("anytime", false, "Run in anytime modality. Incrementally tries to find an upper bound. Does not stop until the user decides so");
        options.addOption("timeout", true, "Timeout for anytime modality");
        options.addOption("stopgro", false, "Stop After Grounding");
        options.addOption("ival", false, "Internal Validation");
        options.addOption("sdac", false, "Activate State Dependent Action Cost (Very Experimental!)");
        options.addOption("onlyplan",false,"Print only the plan without waiting");

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
            searchEngineString = cmd.getOptionValue("s");
            if (searchEngineString == null) {
                searchEngineString = "WAStar";
            }
            tieBreaking = cmd.getOptionValue("ties");
            deltaPlanning = cmd.getOptionValue("dp");
            if (deltaPlanning == null) {
                deltaPlanning = "1.0";
            }
            String optionValue = cmd.getOptionValue("red");
            if (optionValue == null) {
                redundantConstraints = "no";
            } else {
                redundantConstraints = optionValue;
            }
            optionValue = cmd.getOptionValue("gro");
            if (optionValue != null) {
                groundingType = optionValue;
            } else {
                groundingType = "internal";
            }
            internalValidation = cmd.hasOption("ival");

            deltaExecution = cmd.getOptionValue("de");
            if (deltaExecution == null) {
                deltaExecution = "1.0";
            }
            deltaHeuristic = cmd.getOptionValue("dh");
            if (deltaHeuristic == null) {
                deltaHeuristic = "1.0";
            }
            deltaValidation = cmd.getOptionValue("dv");
            if (deltaValidation == null) {
                deltaValidation = "1";
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
                deltaHeuristic = delta;
                deltaValidation = delta;
                deltaPlanning = delta;
                deltaExecution = delta;
            }

            String k = cmd.getOptionValue("k");
            if (k != null) {
                numSubdomains = Integer.parseInt(k);
            } else {
                numSubdomains = 2;
            }

            gw = cmd.getOptionValue("wg");
            hw = cmd.getOptionValue("wh");
            saving_json = cmd.hasOption("sjr");
            sdac = cmd.hasOption("sdac");
            helpfulActionsPruning = cmd.getOptionValue("ha") != null && "true".equals(cmd.getOptionValue("ha"));
            printEvents = cmd.hasOption("pe");
            printTrace = cmd.hasOption("pt");
            savePlan = cmd.getOptionValue("sp");
            onlyPlan = cmd.hasOption("onlyplan");
            anyTime = cmd.hasOption("anytime");
            aibrPreprocessing = !cmd.hasOption("dap");
            stopAfterGrounding = cmd.hasOption("stopgro");
            helpfulTransitions = cmd.getOptionValue("ht") != null && "true".equals(cmd.getOptionValue("ht"));
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
    public SearchHeuristic getHeuristicFunction() {
        return heuristicFunction;
    }

    /**
     * @param heuristicFunction the heuristicFunction to set
     */
    public void setHeuristicFunction(SearchHeuristic heuristicFunction) {
        this.heuristicFunction = heuristicFunction;
    }

    /**
     * @return the problem
     */
    public EPddlProblem getProblem() {
        return problem;
    }

    public void printStats() {
        System.out.println("Grounding and Simplification finished");
        System.out.println("|A|:" + getProblem().getActions().size());
        System.out.println("|P|:" + getProblem().getProcessesSet().size());
        System.out.println("|E|:" + getProblem().getEventsSet().size());
        if (pddlPlus) {
            System.out.println("Delta time heuristic model:" + deltaHeuristic);
            System.out.println("Delta time planning model:" + deltaPlanning);
            System.out.println("Delta time search-execution model:" + deltaExecution);
            System.out.println("Delta time validation model:" + deltaValidation);
        }
    }

    private void setPlanner() {
        helpfulTransitions = false;
        helpfulActionsPruning = false;
        tieBreaking = "arbitrary";
        switch (planner) {
            case "sat-hmrp":
                heuristic = "hmrp";
                searchEngineString = "gbfs";
                tieBreaking = "arbitrary";
                break;
            case "sat-hmrph":
                heuristic = "hmrp";
                helpfulActionsPruning = true;
                searchEngineString = "gbfs";
                tieBreaking = "arbitrary";
                break;
            case "sat-hmrphj":
                heuristic = "hmrp";
                helpfulActionsPruning = true;
                helpfulTransitions = true;
                searchEngineString = "gbfs";
                tieBreaking = "arbitrary";
                break;
            case "sat-hmrpff":
                heuristic = "hmrp";
                helpfulActionsPruning = false;
                redundantConstraints = "brute";
                helpfulTransitions = false;
                searchEngineString = "gbfs";
                tieBreaking = "arbitrary";
                break;
            case "sat-hadd":
                heuristic = "hadd";
                searchEngineString = "gbfs";
                tieBreaking = "smaller_g";
                break;
            case "sat-aibr":
                heuristic = "aibr";
                searchEngineString = "WAStar";
                tieBreaking = "arbitrary";
                break;
            case "sat-hradd":
                heuristic = "hradd";
                searchEngineString = "gbfs";
                tieBreaking = "smaller_g";
                break;
            case "opt-hmax":
                heuristic = "hmax";
                searchEngineString = "WAStar";
                tieBreaking = "larger_g";
                break;
            case "opt-hrmax":
                heuristic = "hrmax";
                searchEngineString = "WAStar";
                tieBreaking = "larger_g";
                break;
            case "opt-blind":
                heuristic = "blind";
                searchEngineString = "WAStar";
                tieBreaking = "larger_g";
                aibrPreprocessing = false;
                break;
            default:
                System.out.println("! ====== ! Warning: Unknown planner configuration. Going with default: gbfs with hadd ! ====== !");
                heuristic = "hadd";
                searchEngineString = "gbfs";
                tieBreaking = "smaller_g";
                break;
        }

    }

    private void setHeuristic() {
        System.out.println("ha:" + helpfulActionsPruning + " ht" + helpfulTransitions);

        Map<AndCond, Collection<IntArraySet>> redConstraint = null;
        if ("smart".equals(redundantConstraints)) {
            System.out.println("Redundant constriants");
            final H1 h1 = new H1(problem, true, true, false, "smart", false, true, false, false);
            h1.computeEstimate(problem.getInit());
            redConstraint = h1.generateSmartRedundantConstraints();
        }

        switch (heuristic) {
            case "gc": {
                h = new GoalCounting(heuristicProblem);
                break;
            }
            case "hadd": {
                h = new H1(heuristicProblem, true, false, false, redundantConstraints, helpfulActionsPruning, false, helpfulTransitions, false, redConstraint, false, false);
                break;
            }
            case "hradd": {
                h = new H1(heuristicProblem, true, false, false, "brute", false, false, false, false);
                break;
            }

            case "hrmax": {
                h = new H1(heuristicProblem, false, false, false, "brute", false, false, false, false);
                break;
            }
            case "hmax": {
                h = new H1(heuristicProblem, false, false, false, redundantConstraints, false, false, false, false, redConstraint, false, false);
                break;
            }
            case "hmrp": {
                h = new H1(heuristicProblem, true, true, false, redundantConstraints, helpfulActionsPruning, false, helpfulTransitions, true, redConstraint, false, false);
                break;
            }
            case "blind": {
                h = new BlindHeuristic(heuristicProblem);
                break;
            }
            case "aibr": {
                System.out.println("AIBR selected");
                h = new Aibr(heuristicProblem);
                break;
            }
            case "hlm-count": {
                System.out.println("HLM selected");
                h = new LM(heuristicProblem);
                break;
            }
            case "hlm-lp": {
                System.out.println("HLM selected");
                System.out.println(redundantConstraints);
                h = new LM(heuristicProblem, "lp",redundantConstraints,"cplex");
                break;
            }
            case "hlm-lp-gurobi": {
                System.out.println("HLM selected");
                System.out.println(redundantConstraints);
                h = new LM(heuristicProblem, "lp",redundantConstraints,"gurobi");
                break;
            }
            default:
                if (heuristic != null) {
                    System.out.println("Folding back to 1-0 heuristic. Input heuristic is not supported");
                }
                h = new BlindHeuristic(heuristicProblem);
                break;
        }

    }

    private void configureHeuristic() throws Exception {

        h = null;
        //next is highly customized configuration
        if (getHeuristicFunction() != null) {
            h = getHeuristicFunction();
        } else {
            setHeuristic();
        }
    }

    private LinkedList<Pair<BigDecimal, Object>> search() throws Exception {

        LinkedList<Pair<BigDecimal, Object>> rawPlan = null;//raw list of actions returned by the search strategies

        final PDDLSearchEngine searchEngine = new PDDLSearchEngine(h, problem); //manager of the search strategies
        Runtime.getRuntime().addShutdownHook(new Thread() {//this is to save json also when the planner is interrupted
            @Override
            public void run() {
                if (saving_json) {
                    searchEngine.searchSpaceHandle.print_json(getProblem().getPddlFileReference() + ".sp_log");
                }
            }
        });
        if (pddlPlus) {
            searchEngine.executionDelta = new BigDecimal(deltaExecution);
            searchEngine.processes = true;
            searchEngine.planningDelta = new BigDecimal(deltaPlanning);
        }

        searchEngine.saveSearchTreeAsJson = saving_json;

        if (tieBreaking != null) {
            switch (tieBreaking) {
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
            tieBreaking = "arbitrary";
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

        System.out.println("Helpful Action Pruning Activated");
        searchEngine.helpfulActionsPruning = helpfulActionsPruning;
        if ("WAStar".equals(searchEngineString)) {
            System.out.println("Running WA-STAR");
            rawPlan = searchEngine.WAStar(getProblem(), timeOut);
        } else if ("wa_star_4".equals(searchEngineString)) {
            System.out.println("Running greedy WA-STAR with hw = 4");
            searchEngine.setWH(4);
            rawPlan = searchEngine.WAStar(getProblem());
        } else if ("gbfs".equals(searchEngineString)) {
            System.out.println("Running Greedy Best First Search");
            if (gw == null) {
                searchEngine.setWG(0);
            }
            rawPlan = searchEngine.greedy_best_first_search(getProblem(), timeOut);
        } else if ("gbfs_ha".equals(searchEngineString)) {
            System.out.println("Running Greedy Best First Search with Helpful Actions");
            if (gw == null) {
                searchEngine.setWG(0);
            }
            rawPlan = searchEngine.greedy_best_first_search(getProblem(), timeOut);
        } else if ("ida".equals(searchEngineString)) {
            System.out.println("Running IDAStar");
            rawPlan = searchEngine.idastar(getProblem(), true);
        } else {
            throw new RuntimeException("Search strategy is not correct");
        }
        endGValue = searchEngine.currentG;

        overallPlanningTime = (System.currentTimeMillis() - overallStart);
        //SimplePlan sp = validate(searchEngine, rawPlan);
//        if (savePlan != null) {
//            enhspUtil.ENHSPUtils.savePlan(new LinkedList<Pair<Float,TransitionGround>>(), problem, savePlan);
//        }
        boolean valid = true;
        if (printTrace) {
            String fileName = getProblem().getPddlFileReference() + "_search_" + searchEngineString + "_h_" + heuristic + "_break_ties_" + tieBreaking + ".npt";
            valid = searchEngine.validate(rawPlan,new BigDecimal(this.deltaExecution), new BigDecimal(deltaExecution), fileName);
            System.out.println("Numeric Plan Trace saved to " + fileName);
        } else if (internalValidation) {
            Pair<PddlDomain, EPddlProblem> res = parseDomainProblem(domainFile, problemFile, deltaValidation, new PrintStream(new OutputStream() {
                    public void write(int b) {}}));
            PDDLSearchEngine validator = new PDDLSearchEngine(h, res.getRight());
            valid = validator.validate(rawPlan,new BigDecimal(this.deltaExecution), new BigDecimal(deltaValidation),"/tmp/temp_trace.pddl");
            if (valid) {
                System.out.println("Plan is valid");
            }else{
                System.out.println("Plan is not valid");
            }
        }
        printInfo(rawPlan, searchEngine);
        return rawPlan;
    }

//    private SimplePlan validate(PDDLSearchEngine searchEngine, LinkedList raw_plan) throws CloneNotSupportedException, Exception {
//        SimplePlan sp = new SimplePlan(domain, getProblem(), false, pddlPlus);  //placeholder for the plan to be found
//        PDDLState lastState = null;
//        System.out.println("Starting Validation");
//        if (raw_plan != null) {// Print some useful information on the outcome of the planning process
//            sp.print_trace = print_trace;
//            if (!pddlPlus) {
//                sp.addAll(raw_plan);
//                lastState = sp.execute((PDDLState) getProblem().getInit(), getProblem().globalConstraints);
//                System.out.println("(Pddl2.1 semantics) Plan is valid:" + lastState.satisfy(getProblem().getGoals()));
//            } else { //This is when you have also autonomous processes going on
//                PddlDomain validationDomain = new PddlDomain(domainFile);
//                EPddlProblem validationProblem = new EPddlProblem(problemFile, validationDomain.getConstants(), validationDomain.getTypes(),validationDomain);
//                //this is when you have processes
//                validationProblem.groundingActionProcessesConstraints();
////                validationProblem.syncAllVariablesAndUpdateCollections(getProblem());
//                validationProblem.setDeltaTimeVariable(delta_val);
//                validationProblem.simplifyAndSetupInit(true);
//                Float time = sp.build_pddl_plus_plan(raw_plan, epsilon);
//                lastState = sp.execute((PDDLState) validationProblem.getInit(), validationProblem.globalConstraints, validationProblem.getProcessesSet(), validationProblem.getEventsSet(), searchEngine.planningDelta, Float.parseFloat(delta_val), time);
////                System.out.println("Last PDDLState:"+last_state.pddlPrint());
//                boolean goal_reached = lastState.satisfy(getProblem().getGoals());
//                System.out.println("(Pddl+ semantics) Plan is valid:" + goal_reached);
//            }
//        }else{
//            return null;
//        }
//        if (lastState != null) {
//            if (!pddlPlus) {
//                sp.setDuration(sp.size());
//            } else {
//                sp.setDuration(lastState.time);//                System.out.println("Duration Via Simulation:"+String.format("%.7f",last_state.getTime().getNumber()));
//            }
//        }
//        return sp;
//    }
    private void printInfo(LinkedList<Pair<BigDecimal, Object>> sp, PDDLSearchEngine searchEngine) throws CloneNotSupportedException {

        PDDLState s = (PDDLState) searchEngine.getLastState();
        if (pddlPlus && sp != null){
        }
        if (sp != null) {
            System.out.println("Problem Solved");
            printPlan(sp, pddlPlus, s,savePlan);
            System.out.println("Plan-Length:" + sp.size());
            planLength = sp.size();
        } else {
            System.out.println("Problem unsolvable");
        }
        if (pddlPlus && sp != null) {
            System.out.println("Elapsed Time: " + s.time);
        }
        System.out.println("Metric (Search):" + searchEngine.currentG);
        System.out.println("Planning Time:" + overallPlanningTime);
        System.out.println("Heuristic Time:" + searchEngine.getHeuristicCpuTime());
        System.out.println("Search Time:" + searchEngine.getOverallSearchTime());
        System.out.println("Expanded Nodes:" + searchEngine.getNodesExpanded());
        System.out.println("States Evaluated:" + searchEngine.getNumberOfEvaluatedStates());
        System.out.println("Fixed constraint violations during search (zero-crossing):" + searchEngine.constraintsViolations);
        System.out.println("Number of Dead-Ends detected:" + searchEngine.deadEndsDetected);
        System.out.println("Number of Duplicates detected:" + searchEngine.duplicatesNumber);
//        if (searchEngine.getHeuristic() instanceof quasi_hm) {
//            System.out.println("Number of LP invocations:" + ((quasi_hm) searchEngine.getHeuristic()).n_lp_invocations);
//        }
        if (saving_json) {
            searchEngine.searchSpaceHandle.print_json(getProblem().getPddlFileReference() + ".sp_log");
        }
    }

    private void printPlan(LinkedList<Pair<BigDecimal, Object>> plan, boolean temporal, PDDLState par, String fileName) {
        float i = 0f;
        Pair<BigDecimal, Object> previous = null;
        List<String> fileContent = new ArrayList();
        boolean startProcess = false;
        int size = plan.size();
        int  j = 0;
        for (Pair<BigDecimal, Object> ele : plan) {
            j++;
            if (!temporal) {
                System.out.print(i + ": " + ele.getRight() + "\n");
                if (fileName != null){
                    TransitionGround t = (TransitionGround) ele.getRight();
                    fileContent.add(t.toString());
                }
                i++;
            } else {
                TransitionGround t = (TransitionGround) ele.getRight();
                if (t.getSemantics() == TransitionGround.Semantics.PROCESS) {
                    if (!startProcess) {
                        previous = ele;
                        startProcess = true;
                    }
                    if (j == size) {
                        if (!onlyPlan){
                            System.out.println(previous.getLeft() + ": -----waiting---- " + "[" + par.time + "]");
                        }
                    }
                } else {
                    if (t.getSemantics() != TransitionGround.Semantics.EVENT || printEvents) {
                        if (startProcess) {
                            startProcess = false;
                            if (!onlyPlan){
                                System.out.println(previous.getLeft() + ": -----waiting---- " + "[" + ele.getLeft() + "]");
                            }
                        }
                        System.out.print(ele.getLeft() + ": " + ele.getRight() + "\n");
                        if (fileName != null) {
                            fileContent.add(ele.getLeft() + ": "+ t.toString());
                        }
                    } else {
                        if (j == size) {
                            if (!onlyPlan){
                                System.out.println(previous.getLeft() + ": -----waiting---- " + "[" + ele.getLeft() + "]");
                            }
                        }
                    }
                }
            }
        }
        
        if (fileName != null) {
            try {
                if (temporal){
                    fileContent.add(par.time+": @PlanEND ");
                }
                Files.write(Path.of(fileName), fileContent);
                
            } catch (IOException ex) {
                Logger.getLogger(ENHSP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

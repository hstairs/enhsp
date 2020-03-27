package utils;

import org.apache.commons.cli.*;

public class ArgParams {

    public String domainFile;
    public String problemFile;
    public String planFile;
    public String planner;
    public String heuristic;
    public String searchEngine;
    public String inputPlan;
    public String breakTies;
    public Float delta;
    public Float deltaMax;
    public Float deltaT;
    public Float deltaTH;
    public Float deltaVal;
    public Float depthLimit;
    public Float epsilon;
    public Float gw;
    public Float hw;
    public Long timeout;
    public Integer debug;
    public Integer nOfSubDomains;
    public Boolean validate;
    public Boolean savingJSON;
    public Boolean hhPruning;
    public Boolean printTrace;
    public Boolean savePlan;
    public Boolean ignoreMetric;
    public Boolean anyTime;
    public Boolean aibrPreProcessing;

    public ArgParams(){

    }

    private static Options getOptions(){
        Options options = new Options();
        options.addRequiredOption("o", "domain", true, "PDDL domain file");
        options.addRequiredOption("f", "problem", true, "PDDL problem file");
        options.addOption("ip",true,"Plan File");
        options.addOption("v", "validate", false, "Validate the plan file against the domain and the problem");
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
        return options;
    }

    public static ArgParams parseInput(String[] args) {

        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        ArgParams p = new ArgParams();

        try {
            CommandLine cmd = parser.parse(options, args);
            p.domainFile = cmd.getOptionValue("o");
            p.problemFile = cmd.getOptionValue("f");
            p.planFile = cmd.getOptionValue("ip");
            p.planner = cmd.getOptionValue("planner");
            p.heuristic = cmd.getOptionValue("h", "aibr");
            p.searchEngine = cmd.getOptionValue("s", "WAStar");
            p.inputPlan = cmd.getOptionValue("ip");
            p.breakTies = cmd.getOptionValue("ties");
            p.delta = Float.parseFloat(cmd.getOptionValue("delta", "0"));
            p.deltaMax = p.delta != 0f ? p.delta : Float.parseFloat(cmd.getOptionValue("delta_max", "1.0"));
            p.deltaT = p.delta != 0f ? p.delta : Float.parseFloat(cmd.getOptionValue("delta_exec", "1.0"));
            p.deltaTH = p.delta != 0f ? p.delta :Float.parseFloat(cmd.getOptionValue("delta_h", "1.0"));
            p.deltaVal = p.delta != 0f ? p.delta : Float.parseFloat(cmd.getOptionValue("delta_val", "1.0"));
            p.depthLimit = cmd.hasOption("dl") ? Float.parseFloat(cmd.getOptionValue("dl")) : Float.POSITIVE_INFINITY;
            p.timeout = cmd.hasOption("timeout") ? Long.parseLong(cmd.getOptionValue("timeout")) * 1000 : Long.MAX_VALUE;
            p.debug = cmd.hasOption("debugLevel") ? Integer.parseInt(cmd.getOptionValue("debugLevel")) : 0;
            p.nOfSubDomains = cmd.hasOption("k") ?  Integer.parseInt(cmd.getOptionValue("k")) : 2;
            p.epsilon = cmd.hasOption("epsilon") ? Float.parseFloat(cmd.getOptionValue("epsilon")) : 0f;
            p.gw = Float.parseFloat(cmd.getOptionValue("gw", "0"));
            p.hw = Float.parseFloat(cmd.getOptionValue("hw", "0"));
            p.savingJSON = cmd.hasOption("sjr");
            p.hhPruning = cmd.hasOption("hh");
            p.printTrace = cmd.hasOption("pt");
            p.savePlan = cmd.hasOption("sp");
            p.ignoreMetric = cmd.hasOption("im");
            p.anyTime = cmd.hasOption("anytime");
            p.validate = cmd.hasOption("v");
            p.aibrPreProcessing = !cmd.hasOption("dap");

        } catch (ParseException exp) {
//            Logger.getLogger(ENHSP.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("enhsp", options);
            System.exit(-1);
        }

        return p;
    }

}

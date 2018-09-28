/* 
 * Copyright (C) 2015-2017, Enrico Scala, contact: enricos83@gmail.com
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
import domain.PddlDomain;
import heuristics.Aibr;
import heuristics.Aibr_rp;
import heuristics.advanced.h1;
import heuristics.advanced.habs_add;
import heuristics.advanced.habs_max;
import heuristics.advanced.hlm;
import heuristics.advanced.quasi_hm;
import heuristics.blind_heuristic;

import org.apache.commons.cli.*;
import plan.SimplePlan;

import search.SearchNode;
import search.SearchStrategies;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import problem.EPddlProblem;
import problem.GroundProcess;
import problem.PDDLGroundAction;
import problem.PDDLState;

public class ENHSP {

    private static String domainFile;
    private static String problemFile;
    private static String search_engine;
    private static String hw;
    private static String heuristic = "aibr";
    private static String gw;
    private static int debug_level = 0;
    private static boolean saving_json = false;
    private static String delta_t;
    private static String depth_limit;
    private static boolean save_plan;
    private static boolean print_trace;
    private static String break_ties;
    private static String planner;
    private static Float epsilon;
    private static String epsilon_string;
    private static Integer debug;
    private static String delta_t_h;
    private static String delta_max;
    private static String delta_val;
    private static boolean eco_saving_json;
    private static boolean hh_pruning;
    private static boolean ignore_metric;
    private static Integer num_of_subdomains;


    public static void main(String[] args) throws Exception {

        parseInput(args);
        long very_start = System.currentTimeMillis();
        PddlDomain domain = new PddlDomain(domainFile);
        if (debug > 2) {
            domain.prettyPrint();
        }

        PddlDomain domain_heuristic = new PddlDomain(domainFile);
        PddlDomain domain_validation = new PddlDomain(domainFile);

        System.out.println("Domain parsed");

        final EPddlProblem problem = new EPddlProblem(problemFile, domain.getConstants(),domain.types);

        //this second model is the one used in the heuristic. This can potentially be different from the one used in the execution model. Decoupling it 
        //allows us to a have a finer control on the machine
        final EPddlProblem heuristic_model = new EPddlProblem(problemFile, domain_heuristic.getConstants(),domain.getTypes());

        //the third one is the validation model, where, also in this case we test our plan against a potentially more accurate description
        final EPddlProblem validation_problem = new EPddlProblem(problemFile, domain.getConstants(),domain.getTypes());

        System.out.println("Problem parsed");
        domain.validate(problem);
        domain_heuristic.validate(heuristic_model);
        domain_validation.validate(validation_problem);
        System.out.println("Grounding..");
        problem.grounding_action_processes_constraints();
        heuristic_model.grounding_action_processes_constraints();

//        boolean can_be_dominant_constraints = domain_heuristic.can_be_abstract_dominant_constraints();

//            System.out.println("Dominant constraints can exist? "+can_be_dominant_constraints);

        //final EPddlProblem heuristic_model = (EPddlProblem) problem.clone();
        System.out.println("Light Validation Completed");

        SimplePlan sp = new SimplePlan(domain, problem);  //placeholder for the plan to be found
        final SearchStrategies searchStrategies = new SearchStrategies(); //manager of the search strategies

        //The following add a handler for storing the search tree in a json file, if specified.
        Runtime.getRuntime().addShutdownHook(new Thread() {//this is to save json also when the planner is interrupted
            @Override
            public void run() {
                if (saving_json) {
                    searchStrategies.search_space_handle.print_json(problem.getPddlFileReference() + ".sp_log");
                }
            }
        });
        LinkedList raw_plan = null;//raw list of actions returned by the search strategies
        if (!domain.getProcessesSchema().isEmpty() || !domain.eventsSchema.isEmpty()) {
            //this is when you have processes
            problem.setDeltaTimeVariable(delta_t);
            heuristic_model.setDeltaTimeVariable(delta_t_h);
            validation_problem.setDeltaTimeVariable(delta_val);
            System.out.println("Delta time validation model:" + delta_val);
            System.out.println("Delta time execution model:" + delta_t);
            System.out.println("Delta time heuristic model:" + delta_t_h);
            System.out.println("Delta Max:" + delta_max);
            searchStrategies.delta = Float.parseFloat(delta_t);
            searchStrategies.processes = true;
            searchStrategies.delta_max = Float.parseFloat(delta_max);
        } else {//this is when you have processes
        }
        PDDLState last_state = null;
//      problem.grounding_reachability();

        System.out.println("Simplification..");
        //problem.setAction_cost_from_metric(!ignore_metric);
        problem.simplifications_action_processes_constraints();
        heuristic_model.setAction_cost_from_metric(!ignore_metric);
        heuristic_model.simplifications_action_processes_constraints();
        
        if (debug == 11) {//print the models after the simplification
            System.out.println("Exec Model: Grounded Processes Representation:" + problem.processesSet);
            for (GroundProcess p : problem.processesSet) {
                System.out.println(p.toPDDL());
            }

            System.out.println("Exec Model: Grounded Actions Representation:" + problem.processesSet);
            for (PDDLGroundAction p : (Collection<PDDLGroundAction>) problem.getActions()) {
                System.out.println(p.toPDDL());
                System.out.println(p.getAction_cost());
            }
            System.out.println(problem.getGoals().pddlPrint(false));
            System.out.println("Heuristic Model: Grounded Processes Representation:" + heuristic_model.processesSet);
            for (GroundProcess p : heuristic_model.processesSet) {
                System.out.println(p.toPDDL());
            }
        }
        //problem.grounding_action_processes_constraints();
        //heuristic_model.associate_action_processes_constraints(problem);
        //problem.grounding_plus_simplifications();

//      System.out.println("DEBUG:Ground Processes:"+problem.processesSet);
//      System.out.println(problem.globalConstraints.pddlPrint(true));
        System.out.println("Grounding and Simplification finished");
        System.out.println("|A|:" + problem.getActions().size());
        System.out.println("|P|:" + problem.processesSet.size());
        System.out.println("|E|:" + problem.eventsSet.size());

//        Set<String> subgoaling_based_heuristic = new HashSet<String>(Arrays.asList("h1","hmax_ref","h1_ref", "h1_5","h1i","lm_actions","hmax","hmmax","h1gi","lm_actions_rc","lm_actions_dc","lm_actions_rc_dc","hm_max","hm_add","hmaxnr"));
        if (planner != null) {
            searchStrategies.breakties_on_larger_g = false;
            searchStrategies.breakties_on_smaller_g = false;
            switch (planner) {
                case "c_h_sat":
                    System.out.println("GBFS with numeric hadd plus helpful actions pruning plus sat breaking ties");
                    heuristic = "hadd";
                    gw = "1";
                    hw = "4";
                    search_engine = "gbfs_ha";
                    break_ties = "larger_g";
                    break;
                case "c_h_sat_hff":
                    System.out.println("GBFS with numeric hff plus helpful actions pruning plus sat breaking ties");
                    heuristic = "hff";
                    gw = "1";
                    hw = "4";
                    search_engine = "gbfs_ha";
                    break_ties = "larger_g";
                    break;
                case "sat"://this is the version used for ijcai-16
                    System.out.println("GBFS with numeric hadd");
                    heuristic = "hadd";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break;
                case "sat_red"://this is the version used for ijcai-16
                    System.out.println("GBFS with numeric hadd and redundant constraints");
                    heuristic = "hradd";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break;
                case "aibr":// this is the version used in the ecai-16 paper
                    System.out.println("A* with aibr");
                    heuristic = "aibr";
                    gw = "1";
                    hw = "1";
                    search_engine = "wa_star";
                    break;
                case "sat_hff"://this is a new version
                    System.out.println("GBFS with numeric hff");
                    heuristic = "hff";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break_ties = "larger_g";
                    break;
                case "sat_hff_ni"://this is a new version
                    System.out.println("GBFS with numeric hff");
                    heuristic = "hff_ni";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break_ties = "larger_g";
                    break;
                case "sat_hff_red"://this is a new version
                    System.out.println("GBFS with numeric hff");
                    heuristic = "hff_rc";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs";
                    break_ties = "larger_g";
                    break;
                case "sat_h_hff":
                    System.out.println("GBFS with numeric hff and helpful actions pruning");
                    heuristic = "hff";
                    gw = "0";
                    hw = "1";
                    search_engine = "gbfs_ha";
                    break_ties = "larger_g";
                    break;
                case "c_sat":
                    System.out.println("WA-STAR with numeric hadd");
                    heuristic = "hadd";
                    gw = "1";
                    hw = "4";
                    search_engine = "wa_star";
                    break_ties = "larger_g";
                    break;
                case "c_sat_hff":
                    System.out.println("GBFS with numeric hff wa_star");
                    heuristic = "hff_ni";
                    gw = "1";
                    hw = "4";
                    search_engine = "gbfs";
                    break_ties = "larger_g";
                    break;
                case "opt":// this is the version used in the ijcai-16 paper
                    System.out.println("A* with numeric hrmax");
                    heuristic = "hrmax";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "wa_star";
                    break;
                case "sat_hrmax":
                    System.out.println("A* with numeric hrmax and h_w = 4");
                    heuristic = "hrmax";
                    gw = "1";
                    hw = "4";
                    break_ties = "larger_g";
                    search_engine = "wa_star";
                    break;
                case "b_opt":
                    System.out.println("A* with 0-1 goal heuristic");
                    heuristic = "blind";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "wa_star";
                    break;
                case "llm_opt":
                    System.out.println("A* with light numeric landmarks (no redundant constraints no dominance analysis");
                    heuristic = "lm_actions";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "wa_star";
                    break;
                case "rc_lm_opt"://this is the version used in the ijcai-17 paper on landmarks
                    System.out.println("A* with redundant constraints numeric landmarks");
                    heuristic = "lm_actions_rc_dc";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "wa_star";
                    break;
                case "lm_opt"://this is the version used in the ijcai-17 paper on landmarks
                    System.out.println("A* with light numeric landmarks");
                    heuristic = "lm_actions_dc";
                    gw = "1";
                    hw = "1";
                    break_ties = "larger_g";
                    search_engine = "wa_star";
                    break;

                default:
                    heuristic = "aibr";
                    search_engine = "wa_star";
                    break;
            }
        }

        //next is highly customized configuration
        switch (heuristic) {
           
            case "hadd": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = false;
                h.additive_h = true;
                break;
            }
            case "hadd_ni": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = false;
                h.additive_h = true;
                h.ibr_deactivated = true;
                break;
            }
            case "hff": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = false;
                h.additive_h = true;
                h.ibr_deactivated = false;
                h.relaxed_plan_extraction = true;
                h.integer_actions = true;
                break;
            }
            case "hff_pp":{
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = false;
                h.additive_h = true;
                h.ibr_deactivated = false;
                h.relaxed_plan_extraction = true;
                h.integer_actions = true;
                h.only_mutual_exclusion_processes = true;
                break;
            }
            case "hff_pp_rc":{
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = true;
                h.additive_h = true;
                h.ibr_deactivated = false;
                h.relaxed_plan_extraction = true;
                h.integer_actions = true;
                h.only_mutual_exclusion_processes = true;
                break;
            }
            case "hff_rc": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = true;
                h.additive_h = true;
                h.ibr_deactivated = false;
                h.relaxed_plan_extraction = true;
                h.integer_actions = true;
                break;
            }
            case "hff_ni": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = false;
                h.additive_h = true;
                h.ibr_deactivated = true;
                h.relaxed_plan_extraction = true;
                h.integer_actions = true;
                break;
            }
            case "hff_ni_rc": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = true;
                h.additive_h = true;
                h.ibr_deactivated = true;
                h.relaxed_plan_extraction = true;
                h.integer_actions = true;
                break;
            }
            case "hiadd": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.red_constraints = false;
                h.additive_h = true;
                h.integer_actions = true;
                break;
            }
            case "hradd": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.additive_h = true;
                h.red_constraints = true;
                break;
            }
            case "hrmax": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.additive_h = false;
                h.red_constraints = true;
                h.conservativehmax = false;//this corresponds to ijcai-16 version
                break;
            }
            case "hrmax_cons": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.additive_h = false;
                h.red_constraints = true;
                h.conservativehmax = true;//this corresponds to ijcai-16 version
                break;
            }
            case "hmax": {
                searchStrategies.setup_heuristic(new h1(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                h1 h = (h1) searchStrategies.getHeuristic();
                h.additive_h = false;
                h.red_constraints = false;
                h.conservativehmax = true;//this corresponds to ijcai-16 version
                break;
            }
            case "aibr": {
                searchStrategies.setup_heuristic(new Aibr(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                Aibr h = (Aibr) searchStrategies.getHeuristic();
                h.set(false, true);
                break;
            }
            case "aibr_rp": {
                searchStrategies.setup_heuristic(new Aibr_rp(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet));
                Aibr_rp h = (Aibr_rp) searchStrategies.getHeuristic();
                h.set(false, false);
                h.extract_plan = true;
                break;
            }
            case "aibr_cons": {
                searchStrategies.setup_heuristic(new Aibr(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet));
                Aibr h = (Aibr) searchStrategies.getHeuristic();
                h.set(true, true);
                break;
            }
            case "hm_max": {
                searchStrategies.setup_heuristic(new quasi_hm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.globalConstraints));
                quasi_hm h = (quasi_hm) searchStrategies.getHeuristic();
                //h.additive_h = true;
                h.additive_h = false;
                h.integer_variables = false;
                h.greedy = false;
                break;
            }
            case "hm_max_gr": {
                searchStrategies.setup_heuristic(new quasi_hm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.globalConstraints));
                quasi_hm h = (quasi_hm) searchStrategies.getHeuristic();
                //h.additive_h = true;
                h.additive_h = false;
                h.integer_variables = false;
                h.greedy = true;
                break;
            }
            case "hm_add": {
                searchStrategies.setup_heuristic(new quasi_hm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.globalConstraints));
                quasi_hm h = (quasi_hm) searchStrategies.getHeuristic();
                //h.additive_h = true;
                h.additive_h = true;
                h.integer_variables = false;
                h.greedy = false;
                break;
            }
            case "hm_add_gr": {
                searchStrategies.setup_heuristic(new quasi_hm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.globalConstraints));
                quasi_hm h = (quasi_hm) searchStrategies.getHeuristic();
                //h.additive_h = true;
                h.additive_h = true;
                h.integer_variables = false;
                h.greedy = true;
                break;
            }

            case "lm_actions": {
                searchStrategies.setup_heuristic(new hlm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                hlm lm = (hlm) searchStrategies.getHeuristic();
                lm.lp_cost_partinioning = true;
                break;
            }
            case "lm_actions_rc": {

                searchStrategies.setup_heuristic(new hlm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                hlm lm = (hlm) searchStrategies.getHeuristic();
                lm.red_constraints = true;
                lm.lp_cost_partinioning = true;
                break;
            }
            
            case "lm_actions_dc": {
                searchStrategies.setup_heuristic(new hlm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                hlm lm = (hlm) searchStrategies.getHeuristic();
                lm.smart_intersection = true;
                lm.lp_cost_partinioning = true;
                break;
            }
            case "lm_actions_rc_dc": {
                searchStrategies.setup_heuristic(new hlm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet));
                hlm lm = (hlm) searchStrategies.getHeuristic();
                lm.smart_intersection = true;
                lm.red_constraints = true;
                lm.lp_cost_partinioning = true;
                break;
            }
            case "lm_actions_mip": {
                searchStrategies.setup_heuristic(new hlm(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet));
                hlm lm = (hlm) searchStrategies.getHeuristic();
                lm.mip = true;
                lm.lp_cost_partinioning = true;
                break;
            }
            case "haddabs": {
                searchStrategies.setup_heuristic(new habs_add(heuristic_model.getGoals(), heuristic_model.getActions(),heuristic_model.processesSet,heuristic_model.eventsSet, Integer.MAX_VALUE));
                habs_add h = (habs_add) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                h.additive_h = true;
                h.midPointSampling = true;
                
                break;
            }

            case "haddabs2": {
                searchStrategies.setup_heuristic(new habs_add(heuristic_model.getGoals(), heuristic_model.getActions(), 2));
                habs_add h = (habs_add) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                h.additive_h = true;
                
                break;
            }
            case "haddabsk": {
                searchStrategies.setup_heuristic(new habs_add(heuristic_model.getGoals(), heuristic_model.getActions(), num_of_subdomains));
                habs_add h = (habs_add) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                h.additive_h = true;
                
                break;
            }
            case "haddabsonline": {
                searchStrategies.setup_heuristic(new habs_add(heuristic_model.getGoals(), heuristic_model.getActions(), num_of_subdomains));
                habs_add h = (habs_add) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                h.additive_h = true;
                h.onlineRepresentatives = true;
                
                
                break;
            } 
            case "haddabsmidpoint": {
                searchStrategies.setup_heuristic(new habs_add(heuristic_model.getGoals(), heuristic_model.getActions(), Integer.MAX_VALUE));
                habs_add h = (habs_add) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                h.additive_h = true;
                h.midPointSampling = true;
                
                break;
            }
            case "haddabsmidkpoint": {
                searchStrategies.setup_heuristic(new habs_add(heuristic_model.getGoals(), heuristic_model.getActions(), num_of_subdomains));
                habs_add h = (habs_add) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                h.additive_h = true;
                h.midPointSampling = true;
                
                break;
            }
            
            case "hmaxabs": {
                searchStrategies.setup_heuristic(new habs_max(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet));
                habs_max h = (habs_max) searchStrategies.getHeuristic();
                h.setMetric(heuristic_model.getMetric());
                break;
            }
            
            case "blind": {
                searchStrategies.setup_heuristic(new blind_heuristic(heuristic_model.getGoals(), heuristic_model.getActions(), heuristic_model.processesSet, heuristic_model.eventsSet));
                break;
            }
            default:
                break;
        }
        if (debug > 0) {
            searchStrategies.getHeuristic().debug = debug;
            searchStrategies.debug = debug;
        }
        searchStrategies.json_rep_saving = saving_json;
        searchStrategies.json_eco_rep_saving = eco_saving_json;

        if (break_ties != null) {
            if (break_ties.equals("smaller_g")) {
                searchStrategies.breakties_on_larger_g = false;
                searchStrategies.breakties_on_smaller_g = true;
            } else if (break_ties.equals("larger_g")) {
                searchStrategies.breakties_on_larger_g = true;
            } else {
                System.out.println("Wrong setting for break-ties. Arbitrary tie breaking");
                searchStrategies.breakties_on_smaller_g = false;
                searchStrategies.breakties_on_larger_g = false;
            }
        } else {//the following is the arbitrary setting
            break_ties = "arbitrary";
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

        if (depth_limit != null) {
            searchStrategies.depth_limit = Integer.parseInt(depth_limit);
            System.out.println("Setting horizon to:" + depth_limit);
        } else {
            searchStrategies.depth_limit = Integer.MAX_VALUE;
        }

        searchStrategies.helpful_actions_pruning = hh_pruning;
        searchStrategies.getHeuristic().helpful_actions_computation = hh_pruning;

        if ("ehc".equals(search_engine)) {
            System.out.println("Running Enforced Hill Climbing (BFS)");
            searchStrategies.forgetting_ehc = false;
            raw_plan = searchStrategies.enforced_hill_climbing(problem);
        } else if ("ehc_ha".equals(search_engine)) {
            System.out.println("Running Enforced Hill Climbing (BFS) with Helpful Actions");
            searchStrategies.getHeuristic().helpful_actions_computation = true;
            searchStrategies.getHeuristic().helpful_actions_computation = true;
            searchStrategies.helpful_actions_pruning = true;
            raw_plan = searchStrategies.enforced_hill_climbing(problem);
        } else if ("ehc_dfs".equals(search_engine)) {
            System.out.println("Running Enforced Hill Climbing (DFS)");
            searchStrategies.bfs = false;
            raw_plan = searchStrategies.enforced_hill_climbing(problem);
        } else if ("wa_star".equals(search_engine)) {
            System.out.println("Running WA-STAR");
            raw_plan = searchStrategies.wa_star(problem);
        } else if ("wa_star_4".equals(search_engine)) {
            System.out.println("Running greedy WA-STAR with hw = 4");
            searchStrategies.set_w_h(4);
            raw_plan = searchStrategies.wa_star(problem);
        } else if ("gbfs".equals(search_engine)) {
            System.out.println("Running Greedy Best First Search");
            if (gw == null) {
                searchStrategies.set_w_g(0);
            }
            raw_plan = searchStrategies.greedy_best_first_search(problem);
        } else if ("gbfs_ha".equals(search_engine)) {
            System.out.println("Running Greedy Best First Search with Helpful Actions");
            searchStrategies.getHeuristic().helpful_actions_computation = true;
            searchStrategies.helpful_actions_pruning = true;
            if (gw == null) {
                searchStrategies.set_w_g(0);
            }
            raw_plan = searchStrategies.greedy_best_first_search(problem);
        } else if ("gbfs_cha".equals(search_engine)) {
            System.out.println("Running Greedy Best First Search with Conservative Helpful Actions");
            searchStrategies.getHeuristic().helpful_actions_computation = true;
            searchStrategies.getHeuristic().weak_helpful_actions_pruning = true;
            searchStrategies.helpful_actions_pruning = true;
            if (gw == null) {
                searchStrategies.set_w_g(0);
            }
            raw_plan = searchStrategies.greedy_best_first_search(problem);
        } else if ("dfs".equals(search_engine)) {
            System.out.println("Running Depth First Search");
            heuristic = "dfs";
            searchStrategies.bfs = false;
            raw_plan = searchStrategies.blindSearch(problem);
        } else if ("brfs".equals(search_engine)) {
            System.out.println("Running Uniform Cost Search");
            heuristic = "brfs";
            searchStrategies.bfs = true;
            raw_plan = searchStrategies.blindSearch(problem);
        } else {
            System.out.println("Strategy is not correct");
            System.exit(-1);
        }

        long overal_planning_time = (System.currentTimeMillis() - very_start);
        if (raw_plan != null) {// Print some useful information on the outcome of the planning process
            System.out.println("Problem Solved");
            sp.print_trace = print_trace;
            if (problem.processesSet.isEmpty() && problem.eventsSet.isEmpty()) {
                sp.addAll(raw_plan);
                last_state = sp.execute(problem.getInit(), problem.globalConstraints);
                System.out.println("(Pddl2.1 semantics) Plan is valid:" + last_state.satisfy(problem.getGoals()));
                System.out.println(sp);
                System.out.println("Plan-Length:" + sp.size());
            } else {//This is when you have also autonomous processes going on
                Float time = sp.build_pddl_plus_plan(raw_plan, Float.parseFloat(delta_max), epsilon);
                System.out.println(sp.printPDDLPlusPlan());
                validation_problem.grounding_action_processes_constraints();
                validation_problem.simplifications_action_processes_constraints();
                last_state = sp.execute(validation_problem.getInit(), validation_problem.globalConstraints, validation_problem.processesSet, validation_problem.eventsSet, searchStrategies.delta_max, Float.parseFloat(delta_val), time);
//                System.out.println("Last PDDLState:"+last_state.pddlPrint());
                boolean goal_reached = last_state.satisfy(problem.getGoals());
                System.out.println("(Pddl+ semantics) Plan is valid:" + goal_reached);
                if (!goal_reached && debug > 3) {
                    System.out.println(last_state);
                }

                System.out.println("Plan-Length:" + sp.size());
            }
            if (print_trace) {
                
                FileWriter file = null;
                try {
                    file = new FileWriter(problem.getPddlFileReference() + "_search_" + search_engine + "_h_" + heuristic + "_break_ties_" + break_ties + ".npt");
                    //System.out.println(this.json_rep.toJSONString());
                    file.write(sp.numeric_plan_trace.toJSONString());
                    file.close();
                } catch (IOException ex) {
                    Logger.getLogger(SearchNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Numeric Plan Trace saved");
            }
            if (save_plan) {
                sp.savePlan(problem.getPddlFileReference() + "_c_" + heuristic + "_gw_" + gw + "_hw_" + gw + "_delta_" + delta_t + ".plan", true);
            }
            if (problem.getMetric() != null && problem.getMetric().getMetExpr() != null) {
                System.out.println("Metric-Value:" + searchStrategies.current_g);
            }

        } else {
            System.out.println("Problem Unsolvable");
        }
        System.out.println("Planning Time:" + overal_planning_time);
        System.out.println("Heuristic Time:" + SearchStrategies.heuristic_time);
        System.out.println("Search Time:" + SearchStrategies.overall_search_time);

        System.out.println("Expanded Nodes:" + SearchStrategies.nodes_expanded);
        System.out.println("States Evaluated:" + SearchStrategies.states_evaluated);
        if (last_state != null) {
            if (debug > 0) {
                System.out.println("Last PDDLState:" + last_state);
            }
            if (last_state.getTime() == null) {
                System.out.println("Duration:" + sp.size());
            } else {
                System.out.println("Duration:" + String.format("%.7f", sp.ending_time));
//                System.out.println("Duration Via Simulation:"+String.format("%.7f",last_state.getTime().getNumber()));
            }
        }
        System.out.println("Total Cost:" + sp.cost);
        System.out.println("Fixed constraint violations during search (zero-crossing):" + searchStrategies.constraints_violations);
        System.out.println("Number of Dead-Ends detected:" + SearchStrategies.num_dead_end_detected);
        System.out.println("Number of Duplicates detected:" + SearchStrategies.number_duplicates);
        System.out.println("Number of LP invocations:" + searchStrategies.getHeuristic().n_lp_invocations);

        if (saving_json) {
            searchStrategies.search_space_handle.print_json(problem.getPddlFileReference() + ".sp_log");
        }

    }

    private static void parseInput(String[] args) {
        Options options = new Options();
        options.addRequiredOption("o", "domain", true, "PDDL domain file");
        options.addRequiredOption("f", "problem", true, "PDDL problem file");
        options.addOption("planner", true, "Allows to set a pre-defined planner configuration. Available options are:\n "
                                           + "sat - gbfs+hadd, it corresponds to IJCAI-16 inadmissible setting\n"
                                           + "c_sat - wa_star+hadd+hw=4, a more systematic version of the above \n"
                                           + "c_h_sat - wa_star+hadd+hw=4+helpful actions, a more systematic version of the above but with helpful actions \n"
                                           + "opt - a_star+hrmax, optimal planning setting (IJCAI-16 admissible version)\n"
                                           + "aibr - a_star+aibr heuristic (ECAI-16 system)\n"
                                           + "lm_opt - a_star+hlm, optimal planning with landmarks (IJCAI-17 admissible version). This requires cplex 12.6.3 installed\n");
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
        options.addOption("s", true, "allows to select search strategy (default is wa_star):\n"
                                  + "gbfs, Greedy Best First Search (f(n) = h(n))\n"
                                  + "wa_star, WA* (f(n) = g(n) + h_w*h(n))\n"
                                  + "wa_star_4, WA* (f(n) = g(n) + 4*h(n))\n"
                                  + "ehc, Enforced Hill Climbing\n"
                                  + "gbfs_ha, Greedy Best First Search with Helpful Actions Pruning\n"
                                  + "ehc_ha, Enforced Hill Climbing with Helpful Actions Pruning");
        options.addOption("ties", true, "tie-breaking (default is arbitrary): larger_g, smaller_g, arbitrary");
        options.addOption("delta_max", true, "planning decision delta: float");
        options.addOption("delta_exec", true, "planning execution delta: float");
        options.addOption("delta_h", true, "planning heuristic delta: float");
        options.addOption("delta_val", true, "validation delta: float");
        options.addOption("delta", true, "global delta time. Override other delta_<max,exec,val,h> configurations: float");
        options.addOption("debug", true, "debug level: integer");
        options.addOption("epsilon", true, "epsilon separation: float");
        options.addOption("gw", true, "g-values weight: float");
        options.addOption("hw", true, "h-values weight: float");
        options.addOption("sjr", false, "save state space explored in json file");
        options.addOption("hh", false, "activate helpful actions pruning");
        options.addOption("sp", false, "save the plan obtained");
        options.addOption("pt", false, "print state trajectory (Experimental)");
        options.addOption("im", false, "Ignore Metric in the heuristic");
        options.addOption("dl", true, "bound on plan-cost: float (Experimental)");
        options.addOption("k", true, "maximal number of subdomains: integer");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            domainFile = cmd.getOptionValue("o");
            problemFile = cmd.getOptionValue("f");
            planner = cmd.getOptionValue("planner");
            heuristic = cmd.getOptionValue("h");
            if (heuristic == null)
                heuristic = "aibr";
            search_engine = cmd.getOptionValue("s");
            if (search_engine == null)
                search_engine = "wa_star";
            break_ties = cmd.getOptionValue("ties");
            delta_max = cmd.getOptionValue("delta_max");
            if (delta_max == null)
                delta_max = "1";
            delta_t = cmd.getOptionValue("delta_exec");
            if (delta_t == null)
                delta_t = "1.0";
            delta_t_h = cmd.getOptionValue("delta_h");
            if (delta_t_h == null)
                delta_t_h = "1.0";
            delta_val = cmd.getOptionValue("delta_val");
            if (delta_val == null)
                delta_val = "1";
            depth_limit = cmd.getOptionValue("dl");
            
            String delta = cmd.getOptionValue("delta");
            if (delta != null){
                delta_t_h = delta;
                delta_val = delta;
                delta_max = delta;
                delta_t = delta;
            }
            String deb = cmd.getOptionValue("debug");
            if (deb != null)
                debug = Integer.parseInt(deb);
            else
                debug = 0;
            
            String k = cmd.getOptionValue("k");
            if (k != null){
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

        } catch (ParseException exp) {
//            Logger.getLogger(ENHSP.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("enhsp", options);
            System.exit(-1);
        }


    }


}
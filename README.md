I tried to write a easy to read README. But it could be the case that this is not as easy to read as it is supposed to be. So please send me an email if you have any question (of any kind) on the planner. My email is enricos83 at gmail dot com.

# What is ENHSP?

This repository contains ENHSP, which stands for Expressive Numeric Heuristic Planner. It is a forward heuristic search planner, but it is expressive in that it can handle:

1. Classical Planning
2. Numeric Planning with linear and non-linear (!!) expressions
3. Planning with discretised autonomous processes and events
4. Global constraints, which are the analogous of always constraints of PDDL 

Note that the planner DOES NOT SUPPORT THE WHOLE ADL.

##Novel Features and License!
This new version of ENHSP (v0.4) has been extended to support:
1. discrete events!
2. universal and existential quantification in formulas (action precondition, constraints and goals)
3. negative preconditions
4. novel heuristics based on landmarks and search techniques for planning with autonomous processes! 

The text below can be outdated in some parts! Previous versions of the planner can be found checking out previous commits. The tag v0.3 is the last previous official version of the planner.
License: The planner is now delivered with a [LGPL3](https://www.gnu.org/licenses/lgpl-3.0.en.html) license

##Description

The planner reads in input a PDDL domain and problem file, and if you are lucky and your problem is not too complex, it provides you with a plan (a sequence of actions). In the case of planning with processes, the plan is a time-stamped plan (associated to each action, you find the time at which that instance of the action has to be executed). In dealing with autonomous processes, ENHSP discretises the problem (with a delta=1sec by default); so the plan is guaranteed to be valid only with respect to that discretisation.

The input language for the planner is PDDL. PDDL is the standard de facto language to express planning problems. The domain file expresses the signature of your predicates, functions and all the actions/processes/events available, in a parametrized way. The problem file expresses the particular instance of the planning problem (e.g., what is the initial value of predicate A? What is the goal?). For more information on PDDL I suggest you to start from [its wikipedia page](https://en.wikipedia.org/wiki/Planning_Domain_Definition_Language#cite_note-opt-15), and follow the links. ENHSP supports PDDL 2.1 in particular, and PDDL+ (for the support of autonomous processes) and also events (only recently introduced). We also allow to employ global constraint as a direct construct of the language (via the :constraint syntax).

The planner has been developed taking ideas from different papers (heuristics, decoupled deltas for discretisation): 

E. Scala, P. Haslum, S. Thiebaux: **Heuristics for Numeric Planning via Subgoaling**, IJCAI 2016

E. Scala, P. Haslum, S. Thiebaux, M. Ramirez, **Interval-Based Relaxation for General Numeric Planning**, ECAI 2016

E. Scala, P. Haslum, D. Magazzeni, S. Thiebaux: **Landmarks for Numeric Planning Problems**, IJCAI 2017

M. Ramirez, E. Scala, P. Haslum, S. Thiebaux: **Numerical Integration and Dynamic Discretization in Heuristic Search Planning over Hybrid Domains** in arXiv

The planner builds on the PPMaJaL library, which can be found [here](https://bitbucket.org/enricode/ppmajal-expressive-pddl-java-library/admin). PPMaJaL provides parsing, data structures, heuristics and search engine for ENHSP.

## Dependencies

The planner is written completely in [JAVA 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html). 

Before compiling make sure to have the Java machine installed on your computer. In Ubuntu this can be obtained executing the following commands from the bash:

```
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java8-installer
sudo apt-get install oracle-java8-set-default
```

ENHSP depends on a number of other external libraries, some for the PDDL parsing, other for some standard algorithm on graphs, and some to interface the API with a Linear Program solver:

In particular:

- [Antlr 3.4](http://www.antlr3.org) is used for parsing pddl problems. [Here](http://www.antlr3.org/download/antlr-3.4-complete.jar) the link to the actual library file that needs to be linked
- [Jgraph](http://jgrapht.org). This is for general algorithms on graphs
- [Ojalgo](http://ojalgo.org). The version used is the v40
- [Json Simple](https://github.com/fangyidong/json-simple). This is used to store information of the search space explored.
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/). This is used to facilitate parsing

They are all open source projects, so is this planner and the library it depends on. For your convenience, *the necessary jar files are all within the ``libs`` folder*. 


## Compilation, Running and Execution

To compile the software just go to the root folder and run `compile`; then you need to run the `install` script, which basically creates another script for running it, i.e., `enhsp` (the file needs to be given execution permissions via `chmod +x ./enhsp`). You can link the file from your /usr folder, so use the planner from wherever you want.

The compilation and running scripts have been written for a Linux based system (i.e., Ubuntu 16.04 works well with them), but I think you can have a similar version for any other system (e.g., Windows or MAC). As long as you provide your OS with a java runtime environment.

The planner can be executed from the root folder using the following command:
```
./enhsp -o <domain_file> -f <problem_file> -planner <string> (main options: sat, aibr, opt, lm_opt)
```
domain_file and problem_file are the PDDL input files.

- sat and opt are the configurations for the satisficing and the optimal version of ENHSP as for the IJCAI-16 paper. They differ on the use of the search strategy and the heuristic employed. They are made specifically for sequential numeric planning without autonomous processes (even though they can work with processes as well, in some case). These two planners have been developed mainly to test the ideas reported in the IJCAI 2016 paper.

- aibr is the main configuration for planning with autonomous processes. It is A* as search engine and the pure AIBR heuristic as for the ECAI 2016 paper.

- lm_opt is the planner with landmarks extraction and LP solving for the cost-partitioning problem. For this configuration you need to have CPLEX 12.6.3. Since CPLEX is a commercial product you need to have a licence for it (you can get a free academic licence), and link it by putting the cplex.jar library in the libs folder.

The planner can be also run using highly customized settings and other configurations not reported here. Using these configurations you can obtain more experimental versions of the planner, some of which are still not published; so use them with care. It may be useful to do so if your problem exhibits particular structures. As an example, when dealing with autonomous processes, ENHSP allows to decouple the delta to be used at execution level, heuristic level and validation level. 

To set these deltas as well as the other parameters (helpful actions, plan extraction so on and so forth), see the help running enhsp without any parameters

It also seems that for some versions of Ubuntu it is required to set the JAVA encoding through the command:
```
"export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8" 
```

# Examples

In the repository you will find examples on the types of problems that can be dealt with.

# Benchmarks

Besides the examples, there are also domains and instances used to test the planner in the ECAI and IJCAI papers.

##Ecai 2016

The instances used for ECAI 2016 are in the ecai16_benchmarks folder. The planner used for those instances is 'aibr' which corresponds to a setting using A* plus the pure AIBR heuristic used in the paper. Note that this does not mean that this planner cannot obtain *better* performances using other settings of parameters. The analysis performed in the ECAI 2016 paper was intended *just to measure the effectiveness of the heuristic and of the relaxation behind it*

##Ijcai 2016

The instances used for IJCAI 2016 are in the ejcai16_benchmarks folder. The planner used for those instances is:

- 'sat' which is GBFS plus the numeric inadmissibile additive h_{add} heuristic
- 'opt' which is A^* and the numeric admissibile h_{max} heuristic using also redundant constraints

In the folder you find two files of the results collected, which slightly differs (from a merely quantitatively standpoint) from the one presented in the paper.

##Ijcai 2017

The instances used for IJCAI 2017 are in the ecai17_benchmarks folder. The planner used for those instances is:

- 'lm_opt' which is A^* and the numeric admissibile h_{max} heuristic using also redundant constraints

The version with landmarks and no redundant constraints can be obtained using a combination of A^*, lm_actions for the heuristic and tie-breaking with larger g-values.


#Limitation and Known Caveats

The planner is an experimental tool developed as a proof of concepts to verify empirically some of the results obtained during our research activities. This means that there could be bugs, or things that need better optimisation, refinement so on and so forth. If you use the planner, *please let me know*! I am gonna also start to collect all the known issues in the following list:

1. General formulas in action preconditions, constraints and goals can be used only with the AIBR heuristic and the heuristic based on subgoaling (hadd, hmax). They are first translated in NNF and then supported natively in the heuristics. Other heuristics (e.g., lm_actions) don't support negative conditions and disjunctions.
2. ..

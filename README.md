
**ENHSP is an active project, with continuous bug fixing and improvements, so things written into this README file can be at times outdated. 
For a general overview of ENHSP and the different versions (ENHSP18, ENHSP-19, ENHSP-20), have a look at the ENHSP website: https://sites.google.com/view/enhsp/.
If you want to use ENHSP for publishing experiments, please send me an [email](enricos83@gmail.com) so that I can give you the correct version.**

I tried to write an easy to read README. But it could be the case that this is not as easy to read as it is supposed to be. So please send me an email if you have any question (of any kind) on the planner. My email is enricos83 at gmail dot com.


# What is ENHSP?

This repository contains ENHSP, which stands for Expressive Numeric Heuristic Planner. It is a forward heuristic search planner, but it is expressive in that it can handle:

1. Classical Planning
2. Numeric Planning with linear and non-linear (!!) expressions
3. Planning with discretised autonomous processes and events
4. Global constraints, which are the analogous of always constraints of PDDL 

Note that the planner does not support Durative Actions (you need to compile them out) and is not stable with the whole Action Description Language


## Description

The planner reads in input a PDDL domain and problem file, and if you are lucky and your problem is not too complex, it provides you with a plan (a sequence of actions). In the case of planning with processes, the plan is a time-stamped plan (associated to each action, you find the time at which that instance of the action has to be executed). In dealing with autonomous processes, ENHSP discretises the problem (with a delta=1sec by default); so the plan is guaranteed to be valid only with respect to that discretisation.

The input language for the planner is PDDL. PDDL is the standard de facto language to express planning problems. The domain file expresses the signature of your predicates, functions and all the actions/processes/events available, in a parametrized way. The problem file expresses the particular instance of the planning problem (e.g., what is the initial value of predicate A? What is the goal?). For more information on PDDL I suggest you to start from [its wikipedia page](https://en.wikipedia.org/wiki/Planning_Domain_Definition_Language#cite_note-opt-15), and follow the links. ENHSP supports PDDL 2.1 in particular, and PDDL+ (for the support of autonomous processes) and also events (only recently introduced). We also allow to employ global constraint as a direct construct of the language (via the :constraint syntax).

The planner has been developed taking ideas from different papers (heuristics, decoupled deltas for discretisation): 

E. Scala, P. Haslum, S. Thiebaux: **Heuristics for Numeric Planning via Subgoaling**, IJCAI 2016

E. Scala, P. Haslum, S. Thiebaux, M. Ramirez, **Interval-Based Relaxation for General Numeric Planning**, ECAI 2016

E. Scala, P. Haslum, D. Magazzeni, S. Thiebaux: **Landmarks for Numeric Planning Problems**, IJCAI 2017

M. Ramirez, E. Scala, P. Haslum, S. Thiebaux: **Numerical Integration and Dynamic Discretization in Heuristic Search Planning over Hybrid Domains** in arXiv

D. Li, E. Scala, P. Haslum, S. Bogomolov **Effect-Abstraction Based Relaxation for Linear Numeric Planning** In IJCAI 2018

E. Scala, P. Haslum, S. Thiebaux, M. Ramirez **Subgoaling Techniques for Satisficing and Optimal Numeric Planning** in JAIR 2020


The planner heavily builds on the JPDDL+ library, that is a substantial refactoring of PPMAJAL [here](https://gitlab.com/enricos83/PPMAJAL-Expressive-PDDL-Java-Library). As PPMaJaL, JPDDL+ provides parsing, data structures, heuristics and search engine for ENHSP. JPDDL+ will be released soon.

## Dependencies

The planner is written completely in JAVA, and in particular it works with [JAVA 15](https://openjdk.java.net/projects/jdk/15/). 

Before compiling make sure to have the Java machine installed on your computer. In Ubuntu this can be obtained executing the following commands from the bash:

```
sudo apt-get install openjdk-15
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

To compile the software just go to the root folder and run `./compile`; this will generate a big JAR file in the enhsp-dist folder


The planner can be executed from the root folder using the following command:
```
java -jar enhsp-dist/enhsp.jar -o <domain_file> -f <problem_file>
```
domain_file and problem_file are the PDDL input files.

It also seems that for some versions of Ubuntu it is required to set the JAVA encoding through the command:
```
"export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8" 
```

In the the ENHSP website, https://sites.google.com/view/enhsp/, you can find more information on the parameters, and how to set custom search and heuristic to run the planner.

# Benchmarks

Besides the examples, there are also domains and instances used to test the planner in the ECAI and IJCAI papers.

## Ecai 2016

The instances used for ECAI 2016 are in the ecai16_benchmarks folder. The planner used for those instances is 'aibr' which corresponds to a setting using A* plus the pure AIBR heuristic used in the paper. Note that this does not mean that this planner cannot obtain *better* performances using other settings of parameters. The analysis performed in the ECAI 2016 paper was intended *just to measure the effectiveness of the heuristic and of the relaxation behind it*

## Ijcai 2016

The instances used for IJCAI 2016 are in the ejcai16_benchmarks folder. The planner used for those instances is:

- 'sat' which is GBFS plus the numeric inadmissibile additive h_{add} heuristic
- 'opt' which is A^* and the numeric admissibile h_{max} heuristic using also redundant constraints

In the folder you find two files of the results collected, which slightly differs (from a merely quantitatively standpoint) from the one presented in the paper.

## Ijcai 2017

The instances used for IJCAI 2017 are in the ijcai17_benchmarks folder. The planner used for those instances is:

- 'lm_opt' which is A^* and the numeric admissibile h_{max} heuristic using also redundant constraints

The version with landmarks and no redundant constraints can be obtained using a combination of A^*, lm_actions for the heuristic and tie-breaking with larger g-values.

## Ijcai 2018

The instances used for IJCAI 2018 are in the ijcai18_benchmarks folder. The planner used for those instances can be obtained running:

- s gbfs -ties smaller_g and -h haddabs


# Limitation and Known Caveats

The planner is an experimental tool developed as a proof of concepts to verify empirically some of the results obtained during our research activities. This means that there could be bugs, or things that need better optimisation, refinement so on and so forth. If you use the planner, *please let me know*! I am gonna also start to collect all the known issues in the following list:

1. General formulas in action preconditions, constraints and goals can be used only with the AIBR heuristic and the heuristic based on subgoaling (hadd, hmax). They are first translated in NNF and then supported natively in the heuristics. Other heuristics (e.g., hlm-lp) don't support negative conditions and disjunctions.
2. *IMPORTANT*. As the grammar used in ENHSP to parse the PDDL file can read temporally qualified domains with timed initial literals and durative actions, in the PDDL input it is not possible to use predicates with keywords such as 'at' or 'start'.

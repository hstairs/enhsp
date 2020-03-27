package validators;

import com.hstairs.ppmajal.conditions.AndCond;
import com.hstairs.ppmajal.conditions.ComplexCondition;
import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.domain.*;
import com.hstairs.ppmajal.extraUtils.Utils;
import com.hstairs.ppmajal.plan.SimplePlan;
import com.hstairs.ppmajal.problem.*;
import utils.ArgParams;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {

    private LinkedList<GroundAction> rawPlan;
    private PddlDomain domain;
    private PddlDomain validationDomain;
    private EPddlProblem problem;
    private EPddlProblem validationProblem;
    private SimplePlan simplePlan;
    private ArgParams params;

    public Validator(ArgParams params) throws Exception{

        this.params = params;
        this.domain = new PddlDomain(params.domainFile);
        this.problem = new EPddlProblem(params.problemFile, domain.getConstants(), domain.getTypes(), domain);
        this.rawPlan = new LinkedList<GroundAction>();

        this.validationDomain = new PddlDomain(params.domainFile);
        this.validationProblem = new EPddlProblem(params.problemFile, this.validationDomain.getConstants(), this.validationDomain.getTypes(), this.validationDomain);

        this.simplePlan = new SimplePlan(domain, problem, false, true);

        Scanner scanner = new Scanner(new File(params.planFile));

        final Pattern pattern = Pattern.compile("(\\d*?\\.\\d*?):.*\\((.*?)\\)", Pattern.MULTILINE);

        while (scanner.hasNext()) {

            String line = scanner.nextLine();
            final Matcher matcher = pattern.matcher(line);
            boolean isMatch = matcher.find();

            if (!isMatch) {
                throw new Exception("Wrong syntax in plan file: " + line);
            }

            float time = Float.parseFloat(matcher.group(1));
            String parenthesis = matcher.group(2).trim();
            ArrayList<String> parenthesisItems = new ArrayList<String>(Arrays.asList(parenthesis.split(" ")));
            String actionName = parenthesisItems.get(0);
            ParametersAsTerms actionParams = new ParametersAsTerms();

            ActionSchema action = domain.getActionByName(actionName);

            if(parenthesisItems.size() > 1){
                ArrayList<String> paramsList = new ArrayList<String>(parenthesisItems.subList(1, parenthesisItems.size()));
                for (String par : paramsList){

                    PDDLObject objectByName =  this.problem.getObjectByName(par);
                    if (objectByName == null && this.domain.constants != null && !this.domain.constants.isEmpty()) {
                        objectByName = Utils.getObjectByName(this.domain.constants, par);
                    }

                    actionParams.add(objectByName);
                }
            }

            if (action == null) {
                throw new Exception("Action not found in the domain theory: " + actionName);
            }

            GroundAction grAction = action.ground(actionParams, null, problem);
            grAction.time = time;
            grAction.generateAffectedNumFluents();
            this.rawPlan.add(grAction);

        }

        this.simplePlan.addAll(this.rawPlan);

    }


    public boolean validate() throws Exception {

        this.validationProblem.groundingActionProcessesConstraints();
        this.validationProblem.setDeltaTimeVariable(params.deltaVal.toString());
        this.validationProblem.simplifyAndSetupInit(false);

        PDDLState init = (PDDLState) validationProblem.getInit();
        AndCond constraints = validationProblem.globalConstraints;
        Set<GroundProcess> processes = validationProblem.getProcessesSet();
        Set<GroundEvent> events = validationProblem.getEventsSet();
        ComplexCondition goals = problem.getGoals();
        float time = rawPlan.get(rawPlan.size() - 1).time;

        PDDLState lastState = simplePlan.execute(init, constraints, processes, events, params.deltaT, params.deltaVal, time);

        boolean goalReached = lastState.satisfy(goals);

        if(goalReached){
            System.out.println("Plan is valid");
        }

        return goalReached;
    }

}

package entities.step;

import DM.StateMachine;
import entities.action.Action;
import exception.NoMatchConditionException;

import java.util.List;

/**
 * 需要执行一段js代码的step
 */
public class jsStep extends Step{

    String script;
    /**
     * description: 执行一段js代码，判断condition，执行满足condition条件的action
     * @Param: 状态机
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        onEnter(stateMachine);
       //执行script中的内容
        stateMachine.getJsEngine().eval(script,stateMachine.getBindings());
        //根据condition跳转
        List<Action> actions = null;
        if (transitions.size() != 0) {
            for (StepTransition transition : transitions) {
                if ((boolean) stateMachine.getJsEngine().eval(transition.getCondition(), stateMachine.getBindings())) {
                    actions = transition.getActions();
                    break;
                }
            }
            if (actions == null)
                throw new NoMatchConditionException();
            //执行action
            for (Action action : actions)
                action.run(stateMachine);
        }
        onExit(stateMachine);
    }



    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}

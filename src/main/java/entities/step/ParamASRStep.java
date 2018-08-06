package entities.step;

import DM.StateMachine;
import entities.Reply;
import entities.action.Action;
import exception.NoMatchConditionException;

import java.util.ArrayList;
import java.util.List;

/**
 * 需要填充变量值的asrStep
 */
public class ParamASRStep extends ASRStep{

    String bindParam;

    /**
     * description: 查看该step中依赖的变量值是否为空，肉为空，则暂停线程，等待用户输入，否则的话判断condition，执行满足condition条件的action
     * @Param: 状态机
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            onEnter(stateMachine);
            if(stateMachine.getStatementParamMap().get(bindParam).getValue().isEmpty()){
                interrupt(stateMachine);
                run(stateMachine);
            }
            else {
                //绑定变量
                stateMachine.getBindings().put(bindParam,stateMachine.getStatementParamMap().get(bindParam).getValue());
                //根据condition跳转
                List<Action> actions = null;
                if (transitions.size() != 0){
                    for (StepTransition transition : transitions){
                        if ((boolean)stateMachine.getJsEngine().eval(transition.getCondition(),stateMachine.getBindings())){
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
            }
        }finally {
            onExit(stateMachine);
        }
    }



    public String getBindParam() {
        return bindParam;
    }

    public void setBindParam(String bindParam) {
        this.bindParam = bindParam;
    }



}

package entities.step;

import DM.StateMachine;
import entities.action.Action;
import exception.NoMatchConditionException;

import java.util.List;

/**
 * 需要按键输入的asrStep
 */
public class KeypadStep extends ASRStep{


    String keypadInputParam;



    public KeypadStep(){
        super();
    }

    @Override
    public void onExit(StateMachine stateMachine){
        stateMachine.setKeypad(false);
        logger.info(name+"step结束执行");
        stateMachine.setCurrentStep(null);
    }

    @Override
    public void onEnter(StateMachine stateMachine){
        stateMachine.setKeypad(true);
        logger.info(name+"step开始执行");
        stateMachine.setCurrentStep(this);
    }

    /**
     * description: 查看是否需要按键输入，若需要，则暂停线程，等待用户输入，否则的话判断condition，执行满足condition条件的action
     * @Param: 状态机
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            onEnter(stateMachine);
            if(stateMachine.getStatementParamMap().get(keypadInputParam) == null || stateMachine.getStatementParamMap().get(keypadInputParam).getValue().isEmpty()){
                interrupt(stateMachine);
                run(stateMachine);
            }
            else {
                //绑定变量

                stateMachine.getBindings().put(keypadInputParam,stateMachine.getStatementParamMap().get(keypadInputParam).getValue());
                //根据condition跳转
                stateMachine.setKeypad(false);
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

    public String getKeypadInputParam() {
        return keypadInputParam;
    }

    public void setKeypadInputParam(String keypadInputParam) {
        this.keypadInputParam = keypadInputParam;
    }


}

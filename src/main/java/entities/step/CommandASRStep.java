package entities.step;

import DM.StateMachine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import entities.action.Action;
import exception.NoMatchConditionException;


import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;

/**
 * 需要按键输入的asrStep
 */
public class CommandASRStep extends ASRStep{

    private List<String> commandList = new ArrayList<>();

   // @JsonIgnore
   // private String matchedCommand = "";

    @Override
    public void onExit(StateMachine stateMachine){
        logger.info(name + "step结束执行");
        stateMachine.setCurrentStep(null);
       // matchedCommand = "";
        stateMachine.getBindings().remove("command");
    }

    @Override
    public void onEnter(StateMachine stateMachine){
        logger.info(name + "step开始执行");
        stateMachine.setCurrentStep(this);
        stateMachine.getBindings().remove("command");
    }


    /**
     * description: 查看是否需要命令输入，若需要，则暂停线程，等待用户输入，否则的话判断condition，执行满足condition条件的action
     * @Param: 状态机
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            onEnter(stateMachine);
            while (stateMachine.getBindings().get("command")==null){
                interrupt(stateMachine);
            }
            {
                //绑定变量
                //stateMachine.getBindings().put("command", matchedCommand);
                //根据condition跳转
                List<Action> actions = null;
                if (transitions.size() != 0){
                    for (StepTransition transition : transitions){
                        if ((boolean)stateMachine.getJsEngine().eval(transition.getCondition(),stateMachine.getBindings())){
                            actions = transition.getActions();
                            break;
                        }
                    }
                    if (actions == null){
                        logger.info(commandList.toString());
                        throw new NoMatchConditionException();
                    }
                    //执行action
                  //  stateMachine.getBindings().remove("command");

                    for (Action action : actions)
                        action.run(stateMachine);
                }
            }
        }finally {
            onExit(stateMachine);
        }

    }


    public List<String> getCommandList() {
        return commandList;
    }

    public void setCommandList(List<String> commandList) {
        this.commandList = commandList;
    }

   /* public String getMatchedCommand() {
        return matchedCommand;
    }

    public void setMatchedCommand(String matchedCommand) {
        this.matchedCommand = matchedCommand;
    }*/
}

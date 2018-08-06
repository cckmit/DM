package entities.action;

import DM.StateMachine;
import entities.StateNode;
import exception.BackToMainMenuException;

/**
 * 转到特殊的服务的action类，如转到ivr、挂机等
 */
public class BackToServices extends Action{

    /**
     * * description: 转到特殊的服务要执行的动作
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine) throws BackToMainMenuException{
        try {
            onEnter(stateMachine);
            new BackToMainMenu().run(stateMachine);
            //stateMachine.transitServices(actionParams.get(0));
        }finally {
            StateNode stateNode = new StateNode();
            stateNode.setId(actionParams.get(0));
            stateMachine.setCurrentState(stateNode);
            onExit(stateMachine);
        }
    }
}

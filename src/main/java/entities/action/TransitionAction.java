package entities.action;

import DM.StateMachine;
import entities.Reply;
import entities.SLUResult;
import entities.StateNode;

import java.util.HashMap;

/**
 * 跳转节点的action类
 */
public class TransitionAction extends Action{

    public TransitionAction(){
        super();
    }
    /**
     * * description: 跳转到另一个节点
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            onEnter(stateMachine);
            StateNode stateNode = stateMachine.model.stateNodeMap.get(actionParams.get(0));
            if (stateNode == null)
                throw new Exception("跳转的节点不存在。");
            if (stateNode!=null){
                stateMachine.transitToStateWithData(new SLUResult(actionParams.get(0),1.0,new HashMap<>()),null,false);
            }
        }finally {
            onExit(stateMachine);
        }
    }

}

package entities.action;

import DM.StateMachine;
import entities.Reply;

/**
 * 生成一个播报回复的action类
 */
public class BroadcastReplyAction extends Action{

    public BroadcastReplyAction(){
        super();
    }
    /**
     * * description: 生成一个播报回复，将回复添加到对话的回复列表中
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine){
        onEnter(stateMachine);
        Reply reply = new Reply(new Boolean(actionParams.get(0)),"broadcast",actionParams.get(1));
        stateMachine.addDialogReply(reply);
        onExit(stateMachine);
    }


}

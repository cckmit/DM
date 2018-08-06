package entities.action;

import DM.StateMachine;
import entities.Reply;

import java.util.List;


/**
 * 生成一个合成回复的action类
 */
public class SynthesisReplyAction extends Action{

    public SynthesisReplyAction(){
        super();
    }
    /**
     * * description: 生成一个合成回复，将回复添加到对话的回复列表中
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        onEnter(stateMachine);
        try {
            //String content = stateMachine.escapeOption(actionParams.get(1));
            List<Reply> replies = stateMachine.synthesisReplyNumberControl(actionParams.get(1),Boolean.parseBoolean(actionParams.get(0)));
            for (Reply reply : replies)
                stateMachine.addDialogReply(reply);
        }finally {
            onExit(stateMachine);
        }

    }

}

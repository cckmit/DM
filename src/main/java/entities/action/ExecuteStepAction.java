package entities.action;

import DM.StateMachine;
import entities.*;
import entities.step.Step;

import java.util.Map;

/**
 * 执行某一个step的action类
 */
public class ExecuteStepAction extends Action{

    public ExecuteStepAction(){
        super();
    }
    /**
     * * description: 执行一个step
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            entities.Process process = null;
            onEnter(stateMachine);
            //拿到参数别表中指定的名字的step，然后run,//先从CurrentProcess找step，然后从init_process中找
            if (stateMachine != null && stateMachine.getCurrentProcess() != null)
                process = stateMachine.getCurrentProcess();
            else if (stateMachine != null && stateMachine.getCurrentState() != null && !stateMachine.getCurrentState().getInit_process().isEmpty())
                process = stateMachine.getStatementProcessMap().get(stateMachine.getCurrentState().getInit_process());
            if (process == null )
                throw new Exception("当前的process为空！");
            Map<String,Step> stepMap = process.getStepMap();
            if (stepMap == null)
                throw new Exception("当前的process没有stepMap");
            Step step = stepMap.get(actionParams.get(0));
            if (step == null)
                throw new Exception("当前的process没有要跳转的step");
            step.run(stateMachine);

        }finally {
            onExit(stateMachine);
        }

    }


}

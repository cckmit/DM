package entities.action;

import DM.StateMachine;
import entities.*;
import entities.Process;

/**
 * 执行某一个process的action类
 */
public class ExecuteProcessAction extends Action{

    /**
     * * description: 执行一个process
     * @Param: 状态机对象
     */
      public void run (StateMachine stateMachine) throws Exception{
          try {
              onEnter(stateMachine);
              if (stateMachine == null || stateMachine.getStatementProcessMap() == null)
                  throw new Exception("process列表为空！");
              Process process = stateMachine.getStatementProcessMap().get(actionParams.get(0));
              if (process == null)
                  throw new Exception("找不到要跳转的process");
              //调用该process的api
              stateMachine.bindAllApiReturn(actionParams.get(0));
              process.run(stateMachine,stateMachine.getCurrentProcess());
          }finally {
              onExit(stateMachine);
          }

    }
}

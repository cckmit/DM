package entities.action;

import DM.StateMachine;
import entities.Reply;
import exception.BackToMainMenuException;

import java.util.HashMap;

/**
 * 返回主菜单的类，主要工作是结束等待的线程，返回到主菜单
 */
public class BackToMainMenu extends Action {
    public BackToMainMenu(){
        super();
    }

    /**
     * * description: 返回主菜单要执行的动作，结束等待的线程，返回到主菜单，抛出返回主菜单异常
     * @Param: 状态机对象
     */
    @Override
     public void run(StateMachine stateMachine) throws BackToMainMenuException{
        try {
            stateMachine.setCurrentState(stateMachine.root);
            stateMachine.setNoMatchOrInputCount(0);
//            stateMachine.setAction("");
//            stateMachine.setTarget("");
//            stateMachine.setSlots(new HashMap<>());
            //stateMachine.setRelistenReply(null);
            onEnter(stateMachine);
            //有处于终端的线程，杀死该线程
            if (stateMachine.getProcessThread() != null && stateMachine.getProcessThread().isWhetherInterrupt() == true){
                //stateMachine.setProcessThread(null);
                stateMachine.getProcessThread().interrupt(); //抛出InterruptedException异常
            }
            //有正在运行的线程，抛出异常，停止该process线程
            if (stateMachine.getProcessThread() != null && stateMachine.getProcessThread().isAlive())
                throw new BackToMainMenuException("返回主菜单，抛出异常，结束线程！");
        }finally {
            stateMachine.setCurrentProcess(null);
            logger.info("backtomenu action turn CurrentProcess to null");
            stateMachine.setCurrentStep(null);
            onExit(stateMachine);
        }
    }
}

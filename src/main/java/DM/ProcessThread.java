package DM;

import entities.*;
import entities.Process;
import exception.BackToMainMenuException;
import exception.BossException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * process线程类：对于对话流程图中的流程，我们给它抽象为process。对于process的定义和处理工作，我们是新开了一个线程进行处理
 */
public class ProcessThread extends Thread{

    final Logger logger = LoggerFactory.getLogger(this.getClass());
    StateMachine stateMachine;
    private final ReentrantLock locker = new ReentrantLock(false);
    Condition condition;
    entities.Process process;
    //线程是否在终端状态
    boolean whetherInterrupt = false;

    public ProcessThread( StateMachine stateMachine, Process process){
        //this.model = model;
        this.stateMachine = stateMachine;
        this.condition = locker.newCondition();
        this.process = process;
    }

    /**
     * 线程的run函数
     */
    public void run() {

        try {
            logger.info("process线程开启");
            process.run(stateMachine,null);
            logger.info("process线程结束");
            stateMachine.setProcessThread(null);
        }catch (BackToMainMenuException backToMainMenuException){
            backToMainMenuException.printStackTrace();

        } catch (InterruptedException interruptedException){

        }catch (SocketTimeoutException | BossException e){ //boss异常处理，转人工
            stateMachine.exceptionHandling("boss异常", e);
        } catch(Exception e){ //对话管理器异常处理，转ivr
            stateMachine.exceptionHandling("对话管理器异常", e);
        }finally {
            stateMachine.setProcessThread(null);
            //如果放在try中，中断如果通过catch到异常来返回，那么唤醒主线程的工作就不执行，就会导致主线程一直在等待
            stateMachine.getLocker().lock();
            try {
                stateMachine.getStateMachineCondition().signal();
            }finally {
                stateMachine.getLocker().unlock();
            }
        }

    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public ReentrantLock getLocker() {
        return locker;
    }

    public boolean isWhetherInterrupt() {
        return whetherInterrupt;
    }

    public void setWhetherInterrupt(boolean whetherInterrupt) {
        this.whetherInterrupt = whetherInterrupt;
    }
}

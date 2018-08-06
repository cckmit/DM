package entities.step;

import DM.StateMachine;
import entities.JsonCommand;
import entities.NoMatchOrInput;
import entities.Reply;

import java.util.ArrayList;
import java.util.List;

/**
 * 需要跟外界交互的step，有CommandASRStep、KeypadStep、ParamASRStep三种类型
 * 该种类型的step会使process线程暂停，主线程开启，等待用户的再次输入
 */
public class ASRStep extends Step{

    NoMatchOrInput noMatch;
    NoMatchOrInput noInput;

    boolean jumpOut = true;

    protected  Reply noParamReply;

    String checkParamJs;

    List<JsonCommand> commands = new ArrayList<>();



    public ASRStep(){
        super();
    }

    @Override
    public void run(StateMachine stateMachine) throws Exception{

    }
    /**
     * description: 线程管理，process线程暂停，主线程开启
     * @Param: 状态机
     */
    public void interrupt(StateMachine stateMachine) throws Exception{
        stateMachine.getProcessThread().setWhetherInterrupt(true);
        //唤醒主线程
        stateMachine.getLocker().lock();
        try {
            stateMachine.addDialogReply(noParamReply);
            stateMachine.getStateMachineCondition().signal();
        }finally {
            stateMachine.getLocker().unlock();
        }
        //让process线程等待
        stateMachine.getProcessThread().getLocker().lock();
        try {
            stateMachine.getProcessThread().getCondition().await();
        }finally {
            stateMachine.getProcessThread().getLocker().unlock();
        }
    }

    @Override
    public void onExit(StateMachine stateMachine){
        logger.info(name+"step结束执行");
        stateMachine.setCurrentStep(null);
    }

    @Override
    public void onEnter(StateMachine stateMachine){
        logger.info(name + "step开始执行");
        stateMachine.setCurrentStep(this);
    }

    public NoMatchOrInput getNoMatch() {
        return noMatch;
    }

    public void setNoMatch(NoMatchOrInput noMatch) {
        this.noMatch = noMatch;
    }

    public NoMatchOrInput getNoInput() {
        return noInput;
    }

    public void setNoInput(NoMatchOrInput noInput) {
        this.noInput = noInput;
    }

    public boolean isJumpOut() {
        return jumpOut;
    }

    public void setJumpOut(boolean jumpOut) {
        this.jumpOut = jumpOut;
    }

    public Reply getNoParamReply() {
        return noParamReply;
    }

    public void setNoParamReply(Reply noParamReply) {
        this.noParamReply = noParamReply;
    }

    public String getCheckParamJs() {
        return checkParamJs;
    }

    public void setCheckParamJs(String checkParamJs) {
        this.checkParamJs = checkParamJs;
    }

    public List<JsonCommand> getCommands() {
        return commands;
    }

    public void setCommands(List<JsonCommand> commands) {
        this.commands = commands;
    }
}

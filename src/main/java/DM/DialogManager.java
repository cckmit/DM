package DM;


import ch.qos.logback.core.joran.conditional.ElseAction;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import entities.*;
import entities.Process;
import entities.step.ApiParamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * Created by xuejunzhang on 2017/1/13.
 */

/**
 * 状态机的包装类型，并且记录了该状态机的最近一次访问时间，
 */
class StateMachineWrapper{
    public StateMachine stateMachine;

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    private long lastAccessTime;

    public StateMachineWrapper(long lastAccessTime,
                               StateMachine stateMachine) {
        this.lastAccessTime = lastAccessTime;
        this.stateMachine = stateMachine;
    }
}

/**
 * 对话管理对外接口类，主要有两个对外接口，feedUserInput和getFirstQusetion
 */
public class DialogManager {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    //protected ScriptEngine jsEngine;
    protected  StateMachineModel stateMachineModel= null;
    protected int sessionTimeOutSec = 1;
    protected LoadingCache<String, StateMachineWrapper> stateMachineCache;
    protected final boolean invalidateUponSessionEnded;


    public static String url = "http://localhost:3847/test/csf_";
    public static int  timeout = 3000;



    //boolean isUseContext = false;

    /**
     * description: 类的构造函数，主要是用CacheBuilder来管理状态机，生成状态机管理的cache
     * @Param: 超时时间、session结束后是否使缓存的状态机无效
     */

    public DialogManager(int sessionTimeOutSec, boolean invalidateUponSessionEnded) {
        this.sessionTimeOutSec = sessionTimeOutSec;
        this.invalidateUponSessionEnded = invalidateUponSessionEnded;
        stateMachineCache = CacheBuilder
                .newBuilder()
                .expireAfterAccess(sessionTimeOutSec * 2, TimeUnit.SECONDS)
                .build(new CacheLoader<String, StateMachineWrapper>() {
                    public StateMachineWrapper load(String userid) {
                        logger.debug("Build state machine for " + userid);
                        return getNewStateMachineWrapper(userid);
                    }
                });
    }


    /**
     * description: 根据用户id，获取该用户的状态机
     * @Param: 根据用户id
     * @return 该用户的状态机
     */
    public StateMachine getStateMachineOf(String userID) {
        StateMachineWrapper lastSMWrapper = null;
        try {
            lastSMWrapper = stateMachineCache.get(userID);
        } catch (ExecutionException e) {
            lastSMWrapper = getNewStateMachineWrapper(userID);
            e.printStackTrace();
        }
        long currentTime = System.currentTimeMillis();
        StateMachineWrapper currentSMWrapper = lastSMWrapper;
        if (currentTime - lastSMWrapper.getLastAccessTime() > sessionTimeOutSec * 1000 ||
                (lastSMWrapper.stateMachine.isSessionEnd() && invalidateUponSessionEnded)) {
            logger.debug("Build state machine for " + userID);
            StateMachineWrapper newSMWrapper = getNewStateMachineWrapper(userID);
            stateMachineCache.asMap().put(userID, newSMWrapper);
            currentSMWrapper = newSMWrapper;
        } else {
            lastSMWrapper.setLastAccessTime(currentTime);
        }
        return currentSMWrapper.stateMachine;
    }

    /**
     * description: 新建一个状态机
     * @Param: 根据用户id
     * @return 新状态机
     */
    protected StateMachine getNewStateMachine(String userID) {
        try {
            StateMachine stateMachine = new StateMachine(stateMachineModel);
//            stateMachine.setUseContext(isUseContext);
            return stateMachine;

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }



//    /**
//     * description: 对外接口函数，输入为理解结果，经过对话流程，经过查询判断等，返回最后给客户的回复、action等内容
//     * @Param: 参数为用户id、理解结果、nomatch和noinput标记、冲突回复，电话号码，透传参数
//     * @return 对话返回的回复内容、action等信息
//     */
//    public  SysAction feedUserInput(String userID, List<SLUResult> userInput,
//                                    String note,String conflictReply,String telephoneNumber,Map<String,String> crsParam) throws Exception{
//
//
//        StateMachine stateMachine = getStateMachineOf(userID);
//        stateMachine.newApiParamEntity(userID, crsParam);
//
//
//        stateMachine.setClearNoMatchOrInputCount(true);
//
//
//        if (stateMachine.deleteNullInput(userInput) && userInput.size()==0){
//            stateMachine.noMatchOrInput(note);
//            return getSysAction(stateMachine);
//        }
//
//        List<SLUResult> stateIDInput = new ArrayList<>();
//        List<SLUResult> commandInput = new ArrayList<>();
//        for (SLUResult sluResult : userInput){
//            if (!sluResult.stateId.isEmpty())
//                stateIDInput.add(sluResult);
//            else if (!sluResult.command.isEmpty())
//                commandInput.add(sluResult);
//            else ;
//        }
//
//        if (commandInput.size() != 0 && stateMachine.determineNextCommand(commandInput))
//            return getSysAction(stateMachine);
//
//
//        if (stateMachine.deleteNullInput(stateIDInput) && stateIDInput.size() ==0){
//            stateMachine.noMatchOrInput("noMatch");
//            return getSysAction(stateMachine);
//        }
//
//        SLUResult determineSLU = stateMachine.determineNextState(stateIDInput,conflictReply);
//        if(determineSLU!=null){
//            stateMachine.setFinalSluResult(determineSLU);
//            stateMachine.transitToStateWithData(determineSLU,telephoneNumber,true);
//        }
//
//        else if(!conflictReply.equals("")){
//            List<Reply> replies = new ArrayList<>();
//            replies.add(new Reply(false,"synthesis",conflictReply));
//            stateMachine.setDialogReply(replies,null,null,stateMachine.isKeypad());
//        }
//
//
//        return  getSysAction(stateMachine);
//
//    }

    /**
     * description: 对外接口函数，输入为理解结果，经过对话流程，经过查询判断等，返回最后给客户的回复、action等内容
     * @Param: 参数为用户id、理解结果、nomatch和noinput标记、冲突回复，电话号码，透传参数
     * @return 对话返回的回复内容、action等信息
     */
    public  SysAction feedUserInput(String userID, SLUResult userInput,
                                    String note,String conflictReply,String telephoneNumber,Map<String,String> crsParam) throws Exception{


        StateMachine stateMachine = getStateMachineOf(userID);
        stateMachine.newApiParamEntity(userID, crsParam);


        stateMachine.setClearNoMatchOrInputCount(true);


        if (stateMachine.isNullInput(userInput)){
            stateMachine.noMatchOrInput(note);
            return getSysAction(stateMachine);
        }

        sluResultPretreatEntity sluResultPretreatEntity = stateMachine.sluResultPretreat(userInput);

        if (sluResultPretreatEntity.getCommandInput().size() != 0 && stateMachine.determineNextCommand(sluResultPretreatEntity.getCommandInput()))
            return getSysAction(stateMachine);


        if (stateMachine.deleteNullInput(sluResultPretreatEntity.getStateIDInput()) && sluResultPretreatEntity.getStateIDInput().size() ==0){
            stateMachine.noMatchOrInput("noMatch");
            return getSysAction(stateMachine);
        }

        SLUResult determineSLU = stateMachine.determineNextState(sluResultPretreatEntity.getStateIDInput(),conflictReply);

        if(determineSLU!=null){
            stateMachine.setFinalSluResult(determineSLU);
            stateMachine.transitToStateWithData(determineSLU, telephoneNumber, true);
        }

        else if(!conflictReply.equals("")){
            List<Reply> replies = new ArrayList<>();
            replies.add(new Reply(false,"synthesis",conflictReply));
            stateMachine.setDialogReply(replies,null,null,stateMachine.isKeypad());
        }


        return  getSysAction(stateMachine);

    }

    /**
     * description: 首问语接口函数
     * @Param: 根据用户id、电话号码、透传参数
     * @return 状态机返回的action状态
     */

    public SysAction getFirstQusetion(String userID,String tel,Map<String,String> crsParam){
        StateMachine stateMachine = getStateMachineOf(userID);
        stateMachine.bindings.put("tel", tel);
        stateMachine.newApiParamEntity(userID, crsParam);
        stateMachine.apiParamEntity.setBusiness_name("greeting");
        stateMachine.greeting();
        return getSysAction(stateMachine);
    }


    /**
     * description: 重置某个用户的状态机状态
     * @Param: 根据用户id
     * @return 重置后的状态机返回的action状态
     */
    public SysAction resetState(String userID){
        StateMachine stateMachine = getStateMachineOf(userID);
        stateMachine.reset();
        return getSysAction(stateMachine);
    }



    /**
     * description: 根据状态机状态，返回给用户所需要action实体，包括当前节点、reply等内容
     * @Param: 状态机
     * @return 返回给用户所需要的action、reply等内容；
     */
    public SysAction getSysAction(StateMachine stateMachine){
        logger.info(stateMachine.getCurrentState().getId());
        if (stateMachine.isClearNoMatchOrInputCount())
            stateMachine.setNoMatchOrInputCount(0);
        if (stateMachine.getCurrentState().getConflictList() != null && !stateMachine.getCurrentState().getConflictList().isEmpty() && stateMachine.conflictList.isEmpty()) //处理冲突节点是语料导进来的情况
            stateMachine.conflictList.addAll(stateMachine.getConflictSluListFromString(stateMachine.getCurrentState().getConflictList()));
        return new SysAction(stateMachine.getReply(),stateMachine.getCurrentState().getId(),stateMachine.getCurrentState(), null,stateMachine.getQuery(), stateMachine.getParam(),
                stateMachine.isNodeTransited,stateMachine.keypad,stateMachine.finalSluResult,stateMachine.getAction(),stateMachine.getTarget(),stateMachine.slots);
    }
    /**
     * description: 配置对话管理的一些参数
     * @Param: 是否用上下文、调用业务服务器的网址、超时时间
     */
    public void setConfig(String url,int timeout){
//        this.isUseContext = isUseContext;
        this.url = url;
        this.timeout = timeout;
    }

    public StateMachineModel getStateMachineModel() {
        return stateMachineModel;
    }

    public void setStateMachineModel(StateMachineModel stateMachineModel) {
        this.stateMachineModel = stateMachineModel;
    }

    protected StateMachineWrapper getNewStateMachineWrapper(String userID) {
        return new StateMachineWrapper(System.currentTimeMillis(),
                getNewStateMachine(userID));
    }


}

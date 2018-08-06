package entities.action;

import DM.StateMachine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import entities.step.APIStep;
import entities.step.ParamASRStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

/**
 * json解析中的action对象，对应着配置文件中的action字段，利用jackson实现自动解析。
 * 有九种类型的action，通过配置文件中action的property属性，自动解析为对应的action对象
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "property")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransitionAction.class, name = "transitionAction"),
        @JsonSubTypes.Type(value = ExecuteStepAction.class, name = "executeStepAction"),
        @JsonSubTypes.Type(value = BroadcastReplyAction.class, name = "broadcastReply"),
        @JsonSubTypes.Type(value = SynthesisReplyAction.class, name = "synthesisReply"),
        @JsonSubTypes.Type(value = BackToMainMenu.class, name = "backToMainMenu"),
        @JsonSubTypes.Type(value = ClearParamValue.class, name = "clearParamValue"),
        @JsonSubTypes.Type(value = ExecuteProcessAction.class, name = "executeProcessAction"),
        @JsonSubTypes.Type(value = BackToServices.class, name = "backToServices"),
        @JsonSubTypes.Type(value = JsAction.class, name = "jsAction")})
public abstract class Action {

    List<String> actionParams = new ArrayList<>();
    @JsonIgnore
    final Logger logger = LoggerFactory.getLogger(this.getClass());


    abstract  public void run (StateMachine stateMachine) throws Exception;
    /**
     * description: 结束该action要执行的动作
     * @Param: 状态机对象
     * @return:
     */
    public void onExit(StateMachine stateMachine){
        logger.info("action执行结束");
    }
    /**
     * description: 进入该action首先要执行的动作
     * @Param: 状态机对象
     * @return:
     */
    public void onEnter(StateMachine stateMachine){
        //不能设置当前step，因为只有asrstep才行
        logger.info("action开始执行");
    }



    public List<String> getActionParams() {
        return actionParams;
    }

    public void setActionParams(List<String> actionParams) {
        this.actionParams = actionParams;
    }
}

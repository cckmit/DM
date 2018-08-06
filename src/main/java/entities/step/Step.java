package entities.step;

import DM.StateMachine;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import entities.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

/**
 * json解析中的step对象，对应着配置文件中的step字段，利用jackson实现自动解析。
 * 有五种类型的step，通过配置文件中step的property属性，自动解析为对应的step对象
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "property")
@JsonSubTypes({
        @JsonSubTypes.Type(value = jsStep.class, name = "jsStep"),
        @JsonSubTypes.Type(value = APIStep.class, name = "APIStep"),
        @JsonSubTypes.Type(value = ParamASRStep.class, name = "paramASRStep"),
        @JsonSubTypes.Type(value = CommandASRStep.class, name = "commandASRStep"),
        @JsonSubTypes.Type(value = KeypadStep.class, name = "keypadStep")})
abstract public class Step {
    String name;

    List<StepTransition> transitions = new ArrayList<>();

    Reply helpReply;

    Reply reListenReply;


    final Logger logger = LoggerFactory.getLogger(this.getClass());


    abstract public void run(StateMachine stateMachine) throws Exception;
    /**
     * description: 结束该step要执行的动作
     * @Param: 状态机对象
     * @return:
     */
    public void onExit(StateMachine stateMachine){
        logger.info(name+"step执行结束");
    }
    /**
     * description: 进入该step首先要执行的动作
     * @Param: 状态机对象
     * @return:
     */
    public void onEnter(StateMachine stateMachine){
        //不能设置当前step，因为只有asrstep才行
        logger.info(name+"step开始执行");
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<StepTransition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<StepTransition> transitions) {
        this.transitions = transitions;
    }

    public Reply getHelpReply() {
        return helpReply;
    }

    public void setHelpReply(Reply helpReply) {
        this.helpReply = helpReply;
    }

    public Reply getReListenReply() {
        return reListenReply;
    }

    public void setReListenReply(Reply reListenReply) {
        this.reListenReply = reListenReply;
    }
}

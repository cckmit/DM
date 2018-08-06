package entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 配置文件中对应的跳转实体，主要定义跳转条件，跳转目标和跳转动作
 */
public class Transition {


    //跳转条件
    protected String condition;

    //跳转节点
    protected String target;

    //动作
    protected String act;


    @JsonCreator
    public Transition(@JsonProperty("condition") String condition,
                      @JsonProperty("target") String target,
                      @JsonProperty("act") String act){
        this.condition = condition != null ? condition : "";
        this.target = target != null ? target : "";
        this.act = act != null ? act : "";
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAction() {
        return act;
    }

    public void setAction(String action) {
        this.act = action;
    }
}

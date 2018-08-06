package entities.step;

import com.fasterxml.jackson.annotation.JsonProperty;
import entities.action.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * step对象中的一个属性，用于描述跳转条件和对应的跳转动作
 */
public class StepTransition {

    String condition;

    List<Action> actions = new ArrayList<>();





    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }


    public String toString(){
        return "condition"+condition+";";
    }
}

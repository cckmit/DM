package entities;

import entities.step.StepTransition;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件中JsonCommand实体，主要是定义能接受的命令的名字以及该命令下的跳转条件和动作
 */
public class JsonCommand {
    String name;

    List<StepTransition> transitions = new ArrayList<>();

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
}

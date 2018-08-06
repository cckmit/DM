package entities;

import DM.StateMachine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.action.Action;
import entities.action.BackToMainMenu;
import entities.step.Step;
import exception.BackToMainMenuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 配置文件中process属性对应的实体对象
 */
public class Process {

    String name;

    String init_step;

    List<Step> steps = new ArrayList<>();

    List<Action> onEnterAction = new ArrayList<>();

    @JsonIgnore
    Map<String,  Step> stepMap = new HashMap<>();


    @JsonIgnore
    Process preProcess = null;

    @JsonIgnore
    final Logger logger = LoggerFactory.getLogger(this.getClass());





    public Process(){}

    public Process(@JsonProperty("name") String name,
                   @JsonProperty("onEnterAction") List<Action> onEnterAction,
                      @JsonProperty("init_step") String init_step,
                   @JsonProperty("steps") List<Step> steps) {
        this.name = name != null ? name : "";
        this.init_step = init_step != null ? init_step : "";
        this.onEnterAction = onEnterAction != null ? onEnterAction : new ArrayList<>();
        this.steps = steps;
        generateStepMap(this.steps);
    }

    private void generateStepMap(List< Step> steps){
        if (steps == null)
            return;
        for (Step step : steps){
            stepMap.put(step.getName(),step);
        }

    }

    public void run(StateMachine stateMachine,Process preProcess) throws Exception{
        try {
            onEnter(stateMachine,preProcess);
            //先run一下onEnterAction中的action
            for (Action action : onEnterAction){
                action.run(stateMachine);
            }
            if (init_step.isEmpty() || init_step == null)
                return;
            Step step = stepMap.get(init_step);
            if (step != null)
                step.run(stateMachine);
            else throw new Exception(init_step+"未能在process中找到");

        }finally {
            onExit(stateMachine);
        }
    }

    public void onExit(StateMachine stateMachine){
        logger.info("名字为"+name+"的process结束。");
        stateMachine.setCurrentProcess(preProcess);
    }

    public void onEnter(StateMachine stateMachine,Process preProcess){
        this.preProcess = preProcess;
        stateMachine.setCurrentProcess(this);
        logger.info("名字为" + name + "的process开始运行。");
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInit_step() {
        return init_step;
    }

    public void setInit_step(String init_step) {
        this.init_step = init_step;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Map<String, Step> getStepMap() {
        return stepMap;
    }

    public void setStepMap(Map<String, Step> stepMap) {
        this.stepMap = stepMap;
    }

    public List<Action> getOnEnterAction() {
        return onEnterAction;
    }

    public void setOnEnterAction(List<Action> onEnterAction) {
        this.onEnterAction = onEnterAction;
    }
}

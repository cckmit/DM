package entities;

import DM.DialogManager;
import DM.StateMachine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import entities.step.APIStep;
import exception.BossException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务服务器对应变量实体，主要包含调用业务服务器的安徽省农户名字，参数，和输入
 */
public class FunctionEntity extends BasicParam{

    Map<String,String> input;
    String functionName;
    List<String> output;

    @JsonIgnore
    String value = "";

    public FunctionEntity(@JsonProperty("name") String name,
                          @JsonProperty("input") Map<String,String> input,
                          @JsonProperty("functionName") String functionName,
                          @JsonProperty("output") List<String> output) {
        this.name = name != null ? name : "";
        this.functionName = functionName != null ? functionName : "";
        this.input = input != null ? input : new HashMap<>();
        this.output = output != null ? output : new ArrayList<>();
        String content = "";
        for (Map.Entry<String,String> entry : input.entrySet())
            content = entry.getKey()+ ":" + content +entry.getValue() + ";";
        generateChildrenName(content);
    }

    /**
     * description: 获得该变量的值
     * @Param: 状态机
     */
    public String getValue(StateMachine stateMachine) throws Exception{
        APIStep apiStep = new APIStep();
        if (children.size() == 0){
            value = apiStep.bindApiFunctionReturn(stateMachine, this,this.name).toString();
            return value;
        }
        else {
            for (String paramName : children){
                BasicParam param = stateMachine.getParamFromName(paramName);
                if (param != null){
                    if (param.getValue(stateMachine) == null)
                        return null;
                }
            }
        }
        value = apiStep.bindApiFunctionReturn(stateMachine, this,this.name).toString();
        return value;
    }





    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String,String> getInput() {
        return input;
    }

    public void setInput(Map<String,String> input) {
        this.input = input;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

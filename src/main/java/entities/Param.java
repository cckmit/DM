package entities;

import DM.StateMachine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体变量实体
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Param  extends BasicParam implements Serializable{

    //缺少该参数时的静态回复
    List<Reply> param_reply;
//    //缺少该变量时动态回复函数的名称
//    String param_query="";
//    //缺少该变量时动态回复函数的参数
//    List<String> param_query_params=null;

    @JsonIgnore
    private String value = "";
    String type = "";
    List<String> defaultFunction=new ArrayList<String>();

    @JsonIgnore
    Map<String,BasicParam> dependParams = new HashMap<>();

    String scope = "";

    //变量的默认值
    String defaultValue = "";

    String runtime = "local";

    Reply helpReply;


    Reply reListenReply;

    List<JsonCommand> commands = new ArrayList<>();

    NoMatchOrInput noMatch;

    NoMatchOrInput noInput;




    public Param(){

    }

    public Param clone(boolean withValue){

        Param newParam = new Param(this.name,this.param_reply,this.defaultFunction,this.defaultValue,this.scope,this.runtime,
        this.helpReply,this.reListenReply,this.noMatch,this.noInput,this.commands);

        if(withValue)
            newParam.setValue(value);

        return newParam;

    }

    public Param(@JsonProperty("name") String name,
                 @JsonProperty("param_reply") List<Reply> param_reply,
                 @JsonProperty("defaultFunction") List<String> defaultFunction,
                 @JsonProperty("defaultValue") String defaultValue,
                 @JsonProperty("scope") String scope,
                 @JsonProperty("runtime") String runtime,
                 @JsonProperty("helpReply") Reply  helpReply,
                 @JsonProperty("reListenReply") Reply  reListenReply,
                 @JsonProperty("noMatch") NoMatchOrInput noMatch,
                 @JsonProperty("noInput") NoMatchOrInput noInput,
                 @JsonProperty("commands") List<JsonCommand> commands) {
        this.name = name != null ? name : "";
        this.param_reply = param_reply != null ? param_reply : new ArrayList<>();
		this.defaultFunction = defaultFunction;
        this.defaultValue = defaultValue;
        this.scope = scope != null ? scope : "";
        this.runtime = runtime != null ? runtime : "local";
        this.noMatch = noMatch;
        this.noInput = noInput;
        this.commands = commands != null ? commands : new ArrayList<>();
        this.helpReply = helpReply;
        this.reListenReply = reListenReply;
        setValueFromDefaultValue(defaultValue);
        for (Reply reply : param_reply)
            generateChildrenName(reply.getContent());

    }



    private void setValueFromDefaultValue(String defaultValue){
        this.value = defaultValue;
    }
    /**
     * description: 获得该变量的值
     * @Param: 状态机
     */
    public String getValue(StateMachine stateMachine) throws Exception{
        dependParams.put(this.getName(),this);
        if (value == null || value.isEmpty()){
            if (children.size() == 0)
                stateMachine.setCurrentParm(this);
            else {
                for (String paramName : children){
                    BasicParam param = stateMachine.getParamFromName(paramName);
                    if (param != null){
                        if (dependParams.get(param.getName()) != null)
                            throw new Exception("变量循环依赖");
                        else dependParams.put(param.getName(),param);
                        if (param.getValue(stateMachine) == null)
                            break;
                    }
                }
                stateMachine.setCurrentParm(this);
            }
            return null;
        }
        return value;
    }



    public String toString(){
        return String.format("{\"name\": \"%s\", " +
                "\"param_query_params\": \"%s\", " ,
                name,param_reply);
    }



    public void setDefaultFunction(List<String> defaultFunction) {
        this.defaultFunction = defaultFunction;
    }
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public String getType(){ return type; }

    public void setType(String type){ this.type = type;}

    public List<String> getDefaultFunction(){ return defaultFunction; }

    public String getDefaultValue(){ return defaultValue; }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public List<Reply> getParam_reply() {
        return param_reply;
    }

    public void setParam_reply(List<Reply> param_reply) {
        this.param_reply = param_reply;
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

    public List<JsonCommand> getCommands() {
        return commands;
    }

    public void setCommands(List<JsonCommand> commands) {
        this.commands = commands;
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
}

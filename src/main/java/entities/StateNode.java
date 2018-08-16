package entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 配置文件中节点属性对应的实体，主要是定义了该节点的相关属性
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateNode {

    @JsonIgnore
    public StateNode parent;


    //节点id，必须指定
    protected String id="";


    //节点对应的回复
    protected List<Reply> reply=new ArrayList<>();


    NoMatchOrInput noMatch;
    NoMatchOrInput noInput;

    //
    protected List<StateNode> subStateNode=new ArrayList<StateNode>();

    //填充信息型时生成动态回复时的函数名
    protected String query="";

    //填充信息型时生成动态回复时的函数参数

    protected List<String> params=new ArrayList<String>();

    //protected List<Transition> transitions = new ArrayList<>();
    protected List<JsonCommand> commandsTransitions = new ArrayList<>();

    String init_process;


    Reply helpReply;


    Reply reListenReply;

    List<String> conflictList;

    List<String> actionList;
    List<String> targetList;
    List<Slot> slots;



    public StateNode(){

    }


    @JsonCreator
    public StateNode(@JsonProperty("id") String id,
                     @JsonProperty("reply") List<Reply> reply,
                     @JsonProperty("noMatch") NoMatchOrInput noMatch,
                     @JsonProperty("noInput") NoMatchOrInput noInput,
                     @JsonProperty("subStateNode") List<StateNode> children,
                     @JsonProperty("query") String query,
                     @JsonProperty("commands") List<JsonCommand> commandsTransitions,
                     @JsonProperty("param") List<String> params,
                     @JsonProperty("init_process") String  init_process,
                     @JsonProperty("helpReply") Reply  helpReply,
                     @JsonProperty("reListenReply") Reply  reListenReply,
                     @JsonProperty("conflictList") List<String>  conflictList,
                     @JsonProperty("actionList") List<String>  actionList,
                     @JsonProperty("targetList") List<String>  targetList,
                     @JsonProperty("slots") List<Slot>  slots) {
        this.id = id != null ? id : "";
        this.reply = reply != null ? reply : new ArrayList<>();
        this.query = query != null ? query : "";
        this.noMatch = noMatch;
        this.noInput = noInput;
        this.subStateNode = children != null ? children : new ArrayList<>();
        this.params=params != null ? params : new ArrayList<>() ;
        this.conflictList = conflictList != null ? conflictList : new ArrayList<>() ;
        this.actionList = actionList != null ? actionList : new ArrayList<>() ;
        this.targetList = targetList != null ? targetList : new ArrayList<>() ;
        this.commandsTransitions = commandsTransitions != null ? commandsTransitions : new ArrayList<>();
        this.slots = slots != null ? slots : new ArrayList<>();
        this.init_process = init_process!= null ? init_process : "";
        this.helpReply = helpReply;
        this.reListenReply = reListenReply;
        for (StateNode c:subStateNode){
            c.setParent(this);
        }

    }

    //是否是祖先
    public boolean isAncestor(StateNode stateNode){
        boolean ret = false;

        if (this.getId().equals("root"))
            return true;

        if (stateNode == null || stateNode.getParent() == null)
            return ret;
        if (stateNode.getId().equals(this.getId()))
            ret = true;
        else
            ret = this.isAncestor(stateNode.getParent());

        return ret;
    }

    //是否是祖先
    public boolean isAncestor(List<StateNode> stateNodeList){
        boolean ret = false;
        if (stateNodeList == null)
            return ret;
        for (StateNode stateNode : stateNodeList){
            ret = this.isAncestor(stateNode);
            if (ret == true)
                break;
        }
        return ret;
    }

    //是否是子孙
    public boolean isDescendant(StateNode stateNode){

        boolean ret = false;

        if (stateNode == null)
            return false;
        if (stateNode.getId().equals(this.getId()))
            return true;
        if (stateNode.getSubStateNode() == null)
            return false;
        else {
            for (StateNode child : stateNode.getSubStateNode()){
                    ret = this.isDescendant(child);
                    if (ret == true)
                        break;
            }
        }
        return ret;
    }


    public String getInit_process() {
        return init_process;
    }

    public void setInit_process(String init_process) {
        this.init_process = init_process;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public List<Reply> getReply() {
        return reply;
    }

    public void setReply(List<Reply> reply) {
        this.reply = reply;
    }

    public List<StateNode> getSubStateNode() {
        return subStateNode;
    }

    public void setSubStateNode(List<StateNode> subStateNode) {
        this.subStateNode = subStateNode;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setParent(StateNode parent) {
        this.parent = parent;
    }

    public StateNode  getParent() {
        return parent;
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

    public List<String> getConflictList() {
        return conflictList;
    }

    public void setConflictList(List<String> conflictList) {
        this.conflictList = conflictList;
    }

    public List<JsonCommand> getCommandsTransitions() {
        return commandsTransitions;
    }

    public void setCommandsTransitions(List<JsonCommand> commandsTransitions) {
        this.commandsTransitions = commandsTransitions;
    }

    public List<String> getActionList() {
        return actionList;
    }

    public void setActionList(List<String> actionList) {
        this.actionList = actionList;
    }

    public List<String> getTargetList() {
        return targetList;
    }

    public void setTargetList(List<String> targetList) {
        this.targetList = targetList;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(List<Slot> slots) {
        this.slots = slots;
    }

    public String toString(){
        return String.format("{\"id\": \"%s\", " +
                        "\"reply\": \"%s\", " +
                "\"query\": \"%s\", " +
                "\"params\": \"%s\", " +
                        "\"sub-states\": [%s]}",
                id,reply,query,params,subStateNode != null? Joiner.on(", ").join(subStateNode): "null");

    }
}

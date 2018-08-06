package entities;


import DM.StateMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 对话管理器中的变量实体，有FunctionEntity（业务服务器对应变量）和Param（实体变量）两种类型
 * 其中变量具有依赖关系
 */
abstract public class BasicParam {

    String name;

    abstract public String getValue(StateMachine stateMachine) throws Exception;

    List<String> children = new ArrayList<>();

    List<BasicParam> allChildren = new ArrayList<>();

    /**
     * description: 根据变量配置的回复内容，获得变量依赖的一级变量
     * @Param: 变量配置的回复内容
     */
    public void generateChildrenName(String replyContent){
        Pattern pattern = Pattern.compile("\\$([^\\$]*?)\\$");
        Matcher matcher = pattern.matcher(replyContent);
        while (matcher.find()) {
            String paramName = "";
            if (matcher.group(1).contains("."))
                paramName = matcher.group(1).substring(0, matcher.group(1).lastIndexOf("."));
            else paramName = matcher.group(1);
            children.add(paramName);
        }
    }
    /**
     * description: 根据变量配置的回复内容，递归找到该变量依赖的所有变量
     * @Param: 变量配置的回复内容
     */
    public BasicParam generateAllChildren(StateMachine stateMachine) throws Exception{
        if (children.isEmpty())
            allChildren.add(this);
        else{
            for (String str : children){
                if (stateMachine.getParamFromName(str) == null)
                    throw new Exception("参数未在params或者apiList中定义");
                allChildren.add(stateMachine.getParamFromName(str).generateAllChildren(stateMachine));
                allChildren.add(this);
            }
        }
        return this;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BasicParam> getAllChildren() {
        return allChildren;
    }

    public void setAllChildren(List<BasicParam> allChildren) {
        this.allChildren = allChildren;
    }
}

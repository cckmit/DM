package entities;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话管理器最后返回的结果实体
 */
public class SysAction {
    public final List<Reply> reply;
    public  final String stateID;
    public final StateNode currentState;
    public final List<StateNode> stateHierarchy;
    public final String query;
    public final Map<String, String> params;
    public final boolean ifTransited;
    public boolean keypad;
    public List<String> finalSluResult;
    public final String action;
    public final String target;
    public final Map<String,String> slots;




/*
    public SysAction(String reply,
                     String stateID,
                     StateNode currentState,
                     List<StateNode> stateHierarchy,
                     String query,
                     Map<String, String> params) {
        this.reply = reply;
        this.stateID = stateID;
        this.currentState = currentState;
        this.stateHierarchy = stateHierarchy;
        this.query = query;
        this.params = params;
        this.ifTransited=true;
    }

    public SysAction(String reply,
                     String stateID,
                     StateNode currentState,
                     List<StateNode> stateHierarchy,
                     String query,
                     Map<String, String> params,
                     boolean ifTransited) {
        this.reply = reply;
        this.stateID = stateID;
        this.currentState = currentState;
        this.stateHierarchy = stateHierarchy;
        this.query = query;
        this.params = params;
        this.ifTransited=ifTransited;
    }*/

    public SysAction(List<Reply> reply,
                     String stateID,
                     StateNode currentState,
                     List<StateNode> stateHierarchy,
                     String query,
                     Map<String, String> params,
                     boolean ifTransited,
                     boolean keypad,
                     List<String> finalSluResult,
                     String action,
                     String target,
                     Map<String,String> slots) {
        this.reply = reply;
        this.stateID = stateID;
        this.currentState = currentState;
        this.stateHierarchy = stateHierarchy;
        this.query = query;
        this.params = params;
        this.ifTransited=ifTransited;
        this.keypad = keypad;
        this.finalSluResult = finalSluResult;
        this.action = action;
        this.target = target;
        this.slots = slots;
    }





    public String toString() {
        return String.format("Reply: %s, 当前节点: %s,query: %s,param : %s", reply, stateID, query == null ? null : query,params != null ? params : "null");
    }


}

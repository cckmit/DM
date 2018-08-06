package entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import entities.action.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件中noMatchOrInput字段对应的实体
 */
public class NoMatchOrInput {

    List<Reply> noMatchorinputlist = new ArrayList<>();

    List<Action> actionlist = new ArrayList<>();

    public NoMatchOrInput() {
    }

    @JsonCreator
    public NoMatchOrInput(@JsonProperty("noMatchorinputlist") List<Reply> noMatchorinputlist,
                 @JsonProperty("actionlist") List<Action> actionlist){
        this.actionlist = actionlist;

        this.noMatchorinputlist = noMatchorinputlist ;
    }


    public List<Reply> getNoMatchorinputlist() {
        return noMatchorinputlist;
    }

    public void setNoMatchorinputlist(List<Reply> noMatchorinputlist) {
        this.noMatchorinputlist = noMatchorinputlist;
    }

    public List<Action> getActionlist() {
        return actionlist;
    }

    public void setActionlist(List<Action> actionlist) {
        this.actionlist = actionlist;
    }
}

package entities;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * 配置文件中reply属性对应的实体
 */
public class Reply {
    Boolean interrupt;
    String property;
    String content;

    public Reply() {
    }

    @JsonCreator
    public Reply(@JsonProperty("interrupt") Boolean isInterrupt,
                      @JsonProperty("property") String property,
                      @JsonProperty("content") String content){
        this.interrupt = isInterrupt;
        this.property = property != null ? property : "";
        this.content = content != null ? content : "";
    }


    public Boolean isInterrupt() {
        return interrupt;
    }

    public void setInterrupt(Boolean interrupt) {
        this.interrupt = interrupt;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toString(){
        return "interrupt:"+ interrupt + ";property : "+ property + ";content:"+ content;
    }
}

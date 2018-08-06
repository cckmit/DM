package entities;


import java.util.List;
/**
 * 配置文件中对应的greeting字段对应的实体
 */
public class Greeting {
    List<Reply> reply;
    String init_process;


    public String getInit_process() {
        return init_process;
    }

    public void setInit_process(String init_process) {
        this.init_process = init_process;
    }

    public List<Reply> getReply() {
        return reply;
    }

    public void setReply(List<Reply> reply) {
        this.reply = reply;
    }
}

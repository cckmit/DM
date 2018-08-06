package entities;

/**
 * 配置文件中对应的exceptionEntity字段对应的实体
 */
public class ExceptionEntity {
    String name;
    Reply reply;
    String action;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Reply getReply() {
        return reply;
    }

    public void setReply(Reply reply) {
        this.reply = reply;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}

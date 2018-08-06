package entities;


import java.util.List;

/**
 * 配置文件中对应的config字段对应的实体
 */
public class Config {
    Greeting greeting;
    List<ExceptionEntity> exceptions;

    public Greeting getGreeting() {
        return greeting;
    }

    public void setGreeting(Greeting greeting) {
        this.greeting = greeting;
    }

    public List<ExceptionEntity> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<ExceptionEntity> exceptions) {
        this.exceptions = exceptions;
    }
}

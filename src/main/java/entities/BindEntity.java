package entities;



/**
 * 异步调用时用到的变量实体
 */
public class BindEntity {
    FunctionEntity functionEntity;
    String bindParamName;
    String json;

    public FunctionEntity getFunctionEntity() {
        return functionEntity;
    }

    public void setFunctionEntity(FunctionEntity functionEntity) {
        this.functionEntity = functionEntity;
    }

    public String getBindParamName() {
        return bindParamName;
    }

    public void setBindParamName(String bindParamName) {
        this.bindParamName = bindParamName;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}

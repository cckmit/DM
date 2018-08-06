package entities.step;

import DM.DialogManager;
import DM.StateMachine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import entities.APIReturnEntity;
import entities.BasicParam;
import entities.FunctionEntity;
import entities.action.Action;
import exception.BossException;
import exception.NoMatchConditionException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 需要调用业务服务器的步骤
 */
public class APIStep extends Step{



    FunctionEntity functionEntity;

    String bindParam = "";



    public APIStep(){
        super();
    }

    /**
     * description: 根据配置的函数名字，调用相应的函数，获得返回结果；然后判断condition条件，执行满足条件下边的action
     * @Param: 状态机对象
     * @return:
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        onEnter(stateMachine);
        try {
            if (!bindParam.isEmpty()){
                BasicParam param = stateMachine.getParamFromName(bindParam);
                if (param != null && param.getClass().getSimpleName().equals("FunctionEntity")){
                    FunctionEntity tempt = (FunctionEntity)param;
                    if (tempt.getValue(stateMachine)!=null && !tempt.getValue().isEmpty()){
                        APIReturnEntity returnEntity = APIReturnEntity.getModelFromInternelJson(tempt.getValue(stateMachine));
                        logger.info(returnEntity.toString());
                        stateMachine.getBindings().put(name, bool2int(returnEntity.getResult()));
                    }
                }
                else throw new Exception(bindParam + "这个变量未在apiList中定义");
            }
            else
                bindApiFunctionReturn(stateMachine,functionEntity,name);
            //根据condition跳转
            List<Action> actions = null;
            if (transitions.size() != 0){
                for (StepTransition transition : transitions){
                    if ((boolean)stateMachine.getJsEngine().eval(transition.getCondition(),stateMachine.getBindings())){
                        actions = transition.getActions();
                        break;
                    }
                }
                if (actions == null){
                    logger.info(name);
                    Map<String,String> result = new HashMap<>();
                    result = (Map<String,String>)stateMachine.getJsEngine().eval(name);
                    Iterator<Map.Entry<String, String>> iterator=result.entrySet().iterator();//创建iterator对象
                    while(iterator.hasNext()){//用while循环，判断是否有下一个
                        Map.Entry<String, String> entry=iterator.next();//声明entry，并用它来装载字符串
                        logger.info("key="+entry.getKey()+";value="+entry.getValue());
                    }
                    logger.info(transitions.toString());
                    throw new NoMatchConditionException();
                }

                //执行action
                for (Action action : actions)
                    action.run(stateMachine);
            }
        } finally {
            onExit(stateMachine);
        }

    }

    public  APIReturnEntity bindApiFunctionReturn(StateMachine stateMachine,FunctionEntity functionEntity,String bindParamName) throws Exception {
        APIReturnEntity ret = new APIReturnEntity();
        //根据functionEntity，发送post请求
        String postUrl = DialogManager.url+functionEntity.getFunctionName();
        //判断变量是否不存在或者为空
        if(stateMachine.getBindings().get(bindParamName)==null){
            ApiParamEntity postParam = new ApiParamEntity();
            postParam.setMethod(functionEntity.getFunctionName());
            postParam.setInter_idx(stateMachine.apiParamEntity.getInter_idx());
            postParam.setUserid(stateMachine.apiParamEntity.getUserid());
            postParam.setBusiness_name(stateMachine.apiParamEntity.getBusiness_name());
            postParam.setTime(stateMachine.apiParamEntity.getTime());
            postParam.setInterface_idx(stateMachine.apiParamEntity.getInterface_idx());
            stateMachine.apiParamEntity.setInterface_idx(Integer.toString(Integer.parseInt(stateMachine.apiParamEntity.getInterface_idx()) + 1));
            for (Map.Entry<String,String> entry : functionEntity.getInput().entrySet()){
                String content = stateMachine.escapeOption(entry.getValue());
                if (content.isEmpty())
                    throw new Exception("bindings为空或者需要的变量未定义");
                postParam.getParams().put(entry.getKey().replaceAll("\\$", ""),content);
            }
            logger.info(ApiParamEntity.entityToJson(postParam));
            String tempt = post(postUrl, ApiParamEntity.entityToJson(postParam));
            ret = APIReturnEntity.getModelFromInternelJson(tempt);
            if (!ret.getCode().equals("0"))
                throw new BossException();
            //将返回值绑定
            stateMachine.getBindings().put(bindParamName, bool2int(ret.getResult()));
        }
        else {
            ret.setCode("0");
            ret.setMessage("");
            ret.setResult((Map<String, String>) stateMachine.getBindings().get(bindParamName));
        }
        logger.info(ret.toString());
        BasicParam param = stateMachine.getParamFromName(functionEntity.getName());
        if (param != null && param.getClass().getSimpleName().equals("FunctionEntity")){ //更新apiList中的变量值
            FunctionEntity tempt_param = (FunctionEntity)param;
            tempt_param.setValue(ret.toString());
        }
        return ret;
    }

    private Map<String,String> bool2int(Map<String,String> result){
        if (result == null)
            return null;
        for (Map.Entry<String,String> entry : result.entrySet()){
            String tempt = entry.getValue().trim().toLowerCase();
            if (tempt.equals("true"))
                entry.setValue("1");
            else if (tempt.equals("false"))
                entry.setValue("0");
        }
        return result;
    }



    //POST提交Json数据
    public String post(String url, String json) throws Exception{
        final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(DialogManager.timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(DialogManager.timeout, TimeUnit.MILLISECONDS).readTimeout(DialogManager.timeout, TimeUnit.MILLISECONDS).build();

        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String resultStr="";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()){
                resultStr = response.body().string();
                System.out.println(resultStr);
            }else throw new BossException();
        } finally {
            if (response != null)
                response.body().close();
//            else
//                throw new BossException();
        }
        return resultStr;
    }


//    public static  String entityToJson(postParam results) throws JsonProcessingException{
//
//        String json = "{}";
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
//        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
//        json = mapper.writeValueAsString(results);
//        return json;
//    }

//    private APIReturnEntity getModelFromInternelJson(String json) throws Exception{
//ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//return mapper.readValue(json, APIReturnEntity.class);
//
//        }



public FunctionEntity getFunctionEntity() {
        return functionEntity;
        }

public void setFunctionEntity(FunctionEntity functionEntity) {
        this.functionEntity = functionEntity;
        }

    public String getBindParam() {
        return bindParam;
    }

    public void setBindParam(String bindParam) {
        this.bindParam = bindParam;
    }
}




//class postParam{
//
//    String method;
//    Map<String,String> params = new HashMap<>();
//
//    public String getMethod() {
//        return method;
//    }
//
//    public void setMethod(String method) {
//        this.method = method;
//    }
//
//    public Map<String, String> getParams() {
//        return params;
//    }
//
//    public void setParams(Map<String, String> params) {
//        this.params = params;
//    }
//
//}







package DM;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import entities.APIReturnEntity;
import entities.BasicParam;
import entities.BindEntity;
import entities.FunctionEntity;
import exception.BossException;
import okhttp3.*;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


/**
 * 异步调用业务服务器函数的类
 */
public class ApiPost {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    final String CONTENT_TYPE_TEXT_JSON = "text/json";
    final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(DialogManager.timeout).setConnectTimeout(DialogManager.timeout).build();
    final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).setMaxConnTotal(20).setMaxConnPerRoute(20).build();

    StateMachine stateMachine;

    public ApiPost(StateMachine stateMachine){
        this.stateMachine = stateMachine;
    }


    /**
     * description: 异步调用多个http的post请求，并且将返回值绑定到状态机的js变量管理binding环境中
     * @Param: 请求的url，多个异步请求参数
     */
    public  void bindAllApiReturn(String url, List<BindEntity> bindEntities) throws Exception{
        if (bindEntities == null)
            return;
        Map<String,HttpEntity> httpEntities= post(url,bindEntities);
        for (Map.Entry<String,HttpEntity> entry : httpEntities.entrySet()) {
            String tempt = EntityUtils.toString(entry.getValue(), "UTF-8");
            logger.info(tempt);
            APIReturnEntity returnEntity = APIReturnEntity.getModelFromInternelJson(tempt);
            if (!returnEntity.getCode().equals("0")){
                logger.info("url为"+ entry.getKey()+ "的api函数调用异常");
                throw new BossException();
            }
            for (BindEntity bindEntity : bindEntities){
                if (("csf_"+ bindEntity.getFunctionEntity().getFunctionName()).equals(entry.getKey().substring(entry.getKey().lastIndexOf("/")+1))){
                    stateMachine.getBindings().put(bindEntity.getBindParamName(), bool2int(returnEntity.getResult()));
                    BasicParam param = stateMachine.getParamFromName(bindEntity.getFunctionEntity().getName());
                    if (param != null && param.getClass().getSimpleName().equals("FunctionEntity")){ //更新apiList中的变量值
                        FunctionEntity tempt_param = (FunctionEntity)param;
                        tempt_param.setValue(returnEntity.toString());
                    }
                    break;
                }
            }
        }
    }

    /**
     * description: 异步请求函数
     * @Param: 请求的url，多个异步请求参数
     * @return: 返回的请求结果
     */
    public Map<String,HttpEntity> post(String url, List<BindEntity> bindEntitie) throws Exception{

        Map<String,HttpEntity> httpEntities = new HashMap<>();
        final CountDownLatch latch = new CountDownLatch(bindEntitie.size());
        try {
            httpclient.start();
            List<HttpPost> requests = new ArrayList<>();


            for (BindEntity bindEntity : bindEntitie){
                HttpPost httpPost = new HttpPost(url+bindEntity.getFunctionEntity().getFunctionName());
                httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
                StringEntity se = new StringEntity(bindEntity.getJson(),"utf-8");
                se.setContentType(CONTENT_TYPE_TEXT_JSON);
                httpPost.setEntity(se);
                requests.add(httpPost);
            }
            for (HttpPost request : requests) {
                //System.out.println(request.getURI().getPath());
                httpclient.execute(request, new FutureCallback<HttpResponse>(){
                    //无论完成还是失败都调用countDown()
                    @Override
                    public void completed(final HttpResponse response){
                        latch.countDown();
                        HttpEntity resEntity = response.getEntity();
                        if(resEntity != null){
                            //System.out.println(resEntity);
                            httpEntities.put(request.getURI().getPath(), resEntity);
                        }
                    }
                    @Override
                    public void failed(final Exception ex) {
                        latch.countDown();
                        System.out.println(request.getRequestLine() + "->" + ex);
                    }
                    @Override
                    public void cancelled() {
                        latch.countDown();
                        System.out.println(request.getRequestLine()
                                + " cancelled");
                    }
                });
            }
            latch.await();
        } finally {
            httpclient.close();
        }
        return httpEntities;
    }

    /**
     * description: 将string类型的true、false、转化为int类型的1、0
     * @Param: 需要转回的string类型输入
     * @return: 转化后的结果
     */
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
}




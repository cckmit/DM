package entities.step;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.Map;
/**
 * 透传参数的类对象
 */
public class ApiParamEntity {
    String method;
    String userid;
    String inter_idx;
    String time;
    String business_name;
    String interface_idx;
    Map<String,String> params = new HashMap<>();

    public static String entityToJson(ApiParamEntity postParam) throws JsonProcessingException {

        String json = "{}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        json = mapper.writeValueAsString(postParam);
        return json;

    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getInter_idx() {
        return inter_idx;
    }

    public void setInter_idx(String inter_idx) {
        this.inter_idx = inter_idx;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getBusiness_name() {
        return business_name;
    }

    public void setBusiness_name(String business_name) {
        this.business_name = business_name;
    }

    public String getInterface_idx() {
        return interface_idx;
    }

    public void setInterface_idx(String interface_idx) {
        this.interface_idx = interface_idx;
    }
}

package entities;

import DM.DialogManager;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.Resources;
import entities.JsonCommand;



import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 整个配置文件对应的实体对象
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateMachineModel{

    //root state
    String stateNodePath = "";
    @JsonIgnore
     StateNode root;

    //process list
    String processesPath = "";  //文件夹路径
    @JsonIgnore
     List<Process> processes = new ArrayList<>();

    //params list
    String paramsPath = "";
    @JsonIgnore
     List<Param> params=new ArrayList<Param>();




    //config
    String configPath = "";
    @JsonIgnore
    Config config;


    //api
    String apiPath = "";
    @JsonIgnore
    List<FunctionEntity> apis = new ArrayList<>();

    @JsonIgnore
    public static Map<String ,StateNode > stateNodeMap = new HashMap<String, StateNode>();

    @JsonIgnore
    public static Map<String ,FunctionEntity > apiMap = new HashMap<>();





    public Param getParamFromParamListByName(String paramName){
        if (params==null)
            return null;
        for (Param param : params){
            if (param.getName().equals(paramName))
                return param;
        }
        return null;
    }

    public List<String> getAssignedVariableByParamName(String paramName){
        List<String> ret = new ArrayList<>();
        if (params==null)
            return null;
        for (Param param : params){
            List<String> defautlFunctions = param.getDefaultFunction();
            for (String defautlFunction : defautlFunctions)
                if (defautlFunction.contains(paramName)){
                    ret.add(param.getName());
                    continue;
                }
        }
        return ret;
    }

    public StateMachineModel(){

    }

    private void checkStateParamMap(StateNode stateNode){

        Map<String, Param> statementParamMap=new HashMap<String, Param>();

        for (Param param:params){
            Param paramNew = param;
            statementParamMap.put(param.getName(),paramNew);
        }

        for (String param:stateNode.getParams()){

            Param paramAdd = statementParamMap.get(param);
            if(paramAdd==null){
                System.err.println("Param "+param+" in node"+stateNode.getId()+" did not exist in Model");
                System.exit(-1);
            }

        }

    }


    private void generateStateNodeMap(StateNode stateNode){
        stateNodeMap.put(stateNode.getId(), stateNode);
        checkStateParamMap(stateNode);

        if (stateNode.getSubStateNode() != null){
            for (StateNode substate:stateNode.getSubStateNode())
                generateStateNodeMap(substate);
        }
    }

    private void generateApiMap(List<FunctionEntity> apis){
        if (apis == null)
            return;
        for (FunctionEntity functionEntity : apis){
            apiMap.put(functionEntity.getName(), functionEntity);
        }

    }



    @JsonCreator
    public StateMachineModel(
            @JsonProperty("stateNodePath") String stateNodePath,
            @JsonProperty("processesPath") String processesPath,
            @JsonProperty("paramsPath") String paramsPath,
            @JsonProperty("configPath") String configPath,
            @JsonProperty("apiPath") String apiPath) throws Exception{
        this.stateNodePath = stateNodePath != null ? stateNodePath : "";
        this.processesPath = processesPath != null ? processesPath : "";
        this.paramsPath = paramsPath != null ? paramsPath : "";
        this.configPath = configPath != null ? configPath : "";
        this.apiPath = apiPath != null ? apiPath : "";
        this.root = getModelFromInternelJson(stateNodePath,StateNode.class);
        this.params = getListModelFromJson(paramsPath, Param.class);
        this.processes = generateProcesses(processesPath);
        this.config = getModelFromInternelJson(configPath, Config.class);
        this.apis = getListModelFromJson(apiPath, FunctionEntity.class);
        stateNodeMap.clear();
        generateStateNodeMap(root);
        generateApiMap(apis);
    }

    private List<Process> generateProcesses(String processesPath) throws Exception{
        List<Process> ret = new ArrayList<>();
        if (processesPath.isEmpty())
            return ret;
        File file = new File(processesPath);
        if (file.exists() && file.isDirectory()){
            File[] files = file.listFiles();
            for (File file1 : files){
                if (file1.getName().endsWith(".json"))
                    ret.add(getModelFromInternelJson(processesPath+file1.getName(),Process.class));
            }
        }
        return ret;
    }


    public static String readResource(ClassLoader cl, String path) {
        return readResource(cl, path, StandardCharsets.UTF_8);
    }

    public static String readResource(ClassLoader cl, String path, Charset charset) {
        try {
            //return Resources.toString(cl.getResource(path), charset);
            File file = new File(path);
            URL url = file.toURL();
//            URL url1 = cl.getResource(path);
//            String t = Resources.toString(url, charset);
            return Resources.toString(url, charset);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return "";
    }



    public static <T> List<T> deepCopy(List<T> src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        List<T> dest = (List<T>) in.readObject();
        return dest;
    }




//    public  static StateMachineModel getModelFromInternelJson(String jsonName) {
//        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        try {
//            return mapper.readValue(readResource(mapper.getClass().getClassLoader(), jsonName), StateMachineModel.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(1);
//            return null;
//        }
//
//    }

    public  static <T>  T getModelFromInternelJson(String jsonName,Class<T> t) throws Exception{
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //String path = readResource(mapper.getClass().getClassLoader(), jsonName);
        return mapper.readValue(readResource(mapper.getClass().getClassLoader(), jsonName), t);

    }

    public  static <T>  List<T> getListModelFromJson(String jsonName,Class<T> t) throws Exception{
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //try {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(ArrayList.class, t);
            return mapper.readValue(readResource(mapper.getClass().getClassLoader(), jsonName), javaType);
        //} catch (Exception e) {
            //e.printStackTrace();
            //System.exit(1);
            //return null;
        }
    //}

    public StateNode getRoot() {
        return root;
    }

    public void setRoot(StateNode root) {
        this.root = root;
    }

    public List<Param> getParams() {
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }


    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }



    public String getParamsPath() {
        return paramsPath;
    }

    public void setParamsPath(String paramsPath) {
        this.paramsPath = paramsPath;
    }

    public String getProcessesPath() {
        return processesPath;
    }

    public void setProcessesPath(String processesPath) {
        this.processesPath = processesPath;
    }

    public String getStateNodePath() {
        return stateNodePath;
    }

    public void setStateNodePath(String stateNodePath) {
        this.stateNodePath = stateNodePath;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public List<FunctionEntity> getApis() {
        return apis;
    }

    public void setApis(List<FunctionEntity> apis) {
        this.apis = apis;
    }

    public String toString() {
        return "StateMachineModel" + root.toString();

    }


    public static void main(String[] args) throws IOException {
        try {
            StateMachineModel model = StateMachineModel.getModelFromInternelJson("C:\\Users\\fxb\\Documents\\nodeInfo\\nodeInfo.json", StateMachineModel.class);
            System.out.println(model);
        }catch (Exception e){
            e.printStackTrace();
        }

    }












}

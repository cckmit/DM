package DM;

import ch.qos.logback.core.joran.conditional.ElseAction;
import ch.qos.logback.core.net.SocketConnector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import entities.*;
import entities.Process;
import entities.action.Action;
import entities.action.BackToMainMenu;
import entities.step.*;

import exception.BackToMainMenuException;
import exception.BossException;
import exception.NoMatchConditionException;
import org.apache.commons.codec.language.Nysiis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 状态机类
 */
public class StateMachine {

    final Logger logger = LoggerFactory.getLogger(this.getClass());
    public final StateMachineModel model;
    public final StateNode root;
    // 当前节点
    protected StateNode currentState;
    //当前提问的参数id,
    private Param currentParm = null;
    //当前执行的process
    private Process currentProcess = null;
    //当前执行的step
    private ASRStep currentStep = null;

    //private Map<String ,StateNode > stateNodeMap=new HashMap<String, StateNode>();

    private Map<String, Param> statementParamMap=new HashMap<String, Param>();
    private Map<String, Process> statementProcessMap = new HashMap<>();

    // 表征对话状态的相关变量
    //public Map<String, String> dataMap=new HashMap<String, String>();
    //返回的内容
    //protected String reply = "";
    protected List<Reply> reply = new ArrayList<>();
    //true表示一轮已经结束，false表示一轮未结束
    //private boolean replyType = true;
    protected String query="";
    protected Map<String,String> actionParam=new HashMap<String, String>();
    boolean keypad = false;
    //当前状态机的状态变量
    protected boolean sessionEnd = false;
    public boolean isNodeTransited = false;
    //冲突列表
    List<SLUResult> conflictList = new ArrayList<SLUResult>();
    //正确的理解结果
    public SLUResult correctSLUResult = null;

    //计数器
    int noMatchCount=0;
    int noInputCount=0;
    int noMatchOrInputCount = 0;
    public boolean clearNoMatchOrInputCount = true;

    boolean isUseContext = false;

    //用于多线程交互的condition
    private final ReentrantLock locker = new ReentrantLock(false);
    private Condition stateMachineCondition = locker.newCondition();
    private ProcessThread processThread;

    //js解析器
    protected final ScriptEngine jsEngine;
    protected Bindings bindings;

    //最终的语义理解结果，可以是command或者stateNode，stateNode冲突时，赋值为冲突列表。noMatch的时候为空
    List<String> finalSluResult = new ArrayList<>();

    //api调用需要记录的参数，一通电话不变
    public ApiParamEntity apiParamEntity;





    /**
     * description: 构造函数
     * @Param: 配置文件映射出来的model对象，其中model配置了节点信息、process过程等，所有的用户的状态机共用model
     */
    public StateMachine(StateMachineModel model) throws Exception{

        this.model = model;
        this.root = model.getRoot();
        currentState = root;
        noMatchCount=0;
        noInputCount=0;
        noMatchOrInputCount = 0;
        for (Param param:model.getParams()){
            Param paramNew = param.clone(false);
            statementParamMap.put(param.getName(),paramNew);
        }
		for (Process process:model.getProcesses()){
            Process ProcessNew = process;
            statementProcessMap.put(process.getName(),ProcessNew);
        }
        //generateStatementProcessMap(this.model.getProcesses());
        //generateStateNodeMap(root);
        long beginTime = System.currentTimeMillis();
        jsEngine = (new ScriptEngineManager()).getEngineByName("JavaScript");
        logger.info("Initialize js engine " + jsEngine);
        logger.info("init js  time " + (System.currentTimeMillis() - beginTime) + " ms");
        reset();
    }


    /**
     * description: 根据节点id，获得真个节点信息
     * @Param: 节点id
     * @return: 节点id对应的StateNode节点
     */
    public StateNode getStateFormID(String stateID) {
        StateNode ret = null;

        for (String id : StateMachineModel.stateNodeMap.keySet()) {
            if (id.equals(stateID))
                ret = StateMachineModel.stateNodeMap.get(id);
        }

        return ret;
    }





    /**
     * description: 重置各个状态变量并对js运行环境进行初始化
     */
    public void reset(){
        try {
            currentState = root;
            //将param的值清空，保留有defaultValue的值和用有defaultValue的值可以赋值的值
            for (String str : statementParamMap.keySet()){
                if (statementParamMap.get(str).getValue() == null)
                    statementParamMap.get(str).setValue("");
            }
            conflictList.clear();
            correctSLUResult = null;
            noMatchCount=0;
            noInputCount=0;
            noMatchOrInputCount = 0;
            //setDialogReply(currentState.getReply(), currentState.getQuery(), currentState.getParams(), keypad);
            newjsBindings(null);
            apiParamEntity = new ApiParamEntity();
            apiParamEntity.setInterface_idx("1");
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * description: 根据输入的string类型的内容，用正则化的方法，提取出其中包含的变量的名字
     * @Param: 要提取变量名字的内容
     * @return: 变量名字列表
     */
    private List<BasicParam> ContainParams(String content){
        Pattern pattern = Pattern.compile("\\$([^\\$]*?)\\$");
        Matcher matcher = pattern.matcher(content);
        List<BasicParam> ret = new ArrayList<>();
        while (matcher.find()) {
            String paramName = "";
            if (matcher.group(1).contains("."))
                paramName = matcher.group(1).substring(0, matcher.group(1).lastIndexOf("."));
            else paramName = matcher.group(1);
            BasicParam param = getParamFromName(paramName);
            if(param != null){
                ret.add(param);
            }
        }
        return ret;
    }


    /**
     * description: 新建一个bindings，用于管理每个用户的变量值
     * @Param: 电话号码
     * @return: 给类变量bindings赋值
     */
    public void newjsBindings(String telephoneNumber) throws Exception{
        Bindings oldBindings = this.bindings;
        this.bindings = (new SimpleScriptContext()).getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("_stateMachine", this);
        bindings.put("currentMenu", currentState.getId().split("_")[0]);
        if (telephoneNumber != null && !telephoneNumber.isEmpty())
            statementParamMap.get("tel").setValue(telephoneNumber);
        for (Param param : model.getParams()){
            if (param.getRuntime().equals("global")){
                if (param.getName().equals("tel") &&  telephoneNumber != null && !telephoneNumber.isEmpty())
                    bindings.put("tel", telephoneNumber);
                else if (oldBindings != null && oldBindings.get(param.getName())!=null)
                    bindings.put(param.getName(),oldBindings.get(param.getName()));
            }
        }
        jsEngine.setBindings(this.bindings, ScriptContext.ENGINE_SCOPE);
        long beginTime = System.currentTimeMillis();
        try {
            jsEngine.eval(Resources.toString(this.getClass().getClassLoader().getResource("test.js"), StandardCharsets.UTF_8),bindings);
            logger.info("init test.js  time " + (System.currentTimeMillis() - beginTime) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * description: 节点跳转函数
     * @Param: 最终的要跳转节点信息、电话号码、是否清空回复
     * @return: 是否跳转
     */
    public boolean transitToStateWithData(SLUResult input,String telephoneNumber,boolean isClearReply){

        boolean isNodeTransited = false;
        StateNode lastState = getCurrentState();
        if (!input.stateId.equals("##")){
            isNodeTransited = lastState.getId().equals(input.stateId) ? false : true;
            if (input.stateId.isEmpty() || !StateMachineModel.stateNodeMap.containsKey(input.stateId))
                isNodeTransited = false;
        }
        if (!isUseContext)//清空上一个状态参数
            if (isNodeTransited){
                clearStateData(lastState);
                currentParm = null;
            }

        if (input.slots != null){
            for (String str : input.slots.keySet()){
                Param param = statementParamMap.get(str);
                if(param!=null)
                    param.setValue(input.slots.get(str));
            }
        }
        try {
            //节点没有跳转并且有等待的process也要清空回复列表
            if (!isNodeTransited && processThread != null && processThread.isWhetherInterrupt() &&
                    currentStep != null && !currentStep.getClass().getSimpleName().equals("CommandASRStep")){
                    //清空回复
                if (isClearReply)
                    reply.clear();
                continueProcess();
            }else {
                //如果processThread还alive，强制杀死processThread，并且将processThread设置为null
                if (processThread != null && processThread.isWhetherInterrupt()==true)
                    processThread.interrupt();
                //跳转
                if (!input.stateId.equals("##"))
                    currentState = StateMachineModel.stateNodeMap.get(input.stateId);
                //新建一个bindings，并且将全局变量绑定
                newjsBindings(telephoneNumber);
                if (currentState.getId().split("_").length>=2)
                    apiParamEntity.setBusiness_name(currentState.getId().split("_")[1]);
                else apiParamEntity.setBusiness_name(currentState.getId());
                if (isClearReply)
                    reply.clear();
                //开启线程
                if (!currentState.getInit_process().isEmpty()){
                    bindAllApiReturn(currentState.getInit_process());
                    startProcess(currentState.getInit_process());
                }
            }
            if ((currentState.getReply() != null && currentState.getReply().size()!= 0) && (processThread == null || !processThread.isWhetherInterrupt())) { //确保没有线程在等待了
                for (Reply reply : currentState.getReply()){
                    List<Reply> generateReplys = generateReply(reply);
                    for (Reply reply1 : generateReplys)
                        addDialogReply(reply1);
                    if (currentParm != null)
                        break;
                }
            }
        }catch (SocketTimeoutException | BossException e){ //boss异常处理，转人工
            exceptionHandling("boss异常",e);
        } catch(Exception e){ //对话管理器异常处理，转ivr
            exceptionHandling("对话管理器异常", e);
        }
        return isNodeTransited;
    }

    /**
     * description: 异常处理函数
     * @Param: 异常的名字，具体的异常
     */
    public void exceptionHandling(String exceptionName,Exception e){
        reset();
        List<ExceptionEntity> exceptions =  model.getConfig().getExceptions();
        boolean isDefinedException = false;
        StateNode stateNode = new StateNode();
        for (ExceptionEntity exceptionEntity : exceptions){
            if (exceptionEntity.getName().equals(exceptionName)){
                setDialogReply(exceptionEntity.getReply(),null,null);
                stateNode.setId(exceptionEntity.getAction());
                isDefinedException = true;
                break;
            }
        }
        if (!isDefinedException)
            setDialogReply(new Reply(false, "synthesis", "未定义异常"),null,null);
        stateNode.setId("IVR MENU");
        currentState = stateNode;
        e.printStackTrace();

    }

    /**
     * description: 根据reply，生成回复
     * @Param: 输入的reply
     * @return: 生成的具体的回复列表
     */
    private List<Reply> generateReply(Reply reply) throws Exception{
        List<Reply> ret = new ArrayList<>();
        currentParm = null;
        updataBindingsFromParamMap();  //将stateMap中的变量同步到了binding中
        List<BasicParam> params = ContainParams(reply.getContent());
        for (BasicParam param : params){
            if (param.getValue(this) == null)
                if (currentParm == null)
                    throw new Exception("参数依赖有问题");
                else return currentParm.getParam_reply();
        }
        Reply reply1 = new Reply();
        reply1.setInterrupt(reply.isInterrupt());
        reply1.setProperty(reply.getProperty());
        reply1.setContent(escapeOption(reply.getContent()));
        ret.add(reply1);
        return ret;
    }

    /**
     * description: 根据变量的名字，获得该变量的对象
     * @Param: 变量名称
     * @return: 变量对象
     */
    public BasicParam getParamFromName(String paramName){
        BasicParam param = statementParamMap.get(paramName);
        if(param != null)
            return param;
        param = model.apiMap.get(paramName);
        if(param != null)
            return param;
        return null;
    }



    /**
     * description: 异步调用一个process中所有的apistep中涉及的业务服务器函数
     * @Param: process的名字
     * @return: 调用是否成功
     */
    public boolean bindAllApiReturn(String processName){
        boolean ret = true;
        try {
            ApiPost apiPost = new ApiPost(this);
            Process process = statementProcessMap.get(processName);
            if (process == null)
                return false;
            List<BindEntity> bindEntities = new ArrayList<>();
            if (process.getSteps() == null)
                return true;
            ApiParamEntity postParam = new ApiParamEntity();
            postParam.setInter_idx(apiParamEntity.getInter_idx());
            postParam.setUserid(apiParamEntity.getUserid());
            postParam.setBusiness_name(apiParamEntity.getBusiness_name());
            postParam.setTime(apiParamEntity.getTime());
            for (Step step : process.getSteps()){
                if (step.getClass().getSimpleName().equals("APIStep")){
                    APIStep apiStep = (APIStep) step;
                    boolean ispost = true;
                    String content = "";
                    for (Map.Entry<String,String> entry : apiStep.getFunctionEntity().getInput().entrySet()){
                        content = escapeOption(entry.getValue());
                        if(content.isEmpty()){
                            ispost = false;
                            break;
                        }
                        postParam.getParams().put(entry.getKey().replaceAll("\\$", ""),content);
                    }
                    if (ispost){
                        postParam.setMethod(apiStep.getFunctionEntity().getFunctionName());
                        postParam.setInterface_idx(apiParamEntity.getInterface_idx());
                        apiParamEntity.setInterface_idx(Integer.toString(Integer.parseInt(apiParamEntity.getInterface_idx())+1));
                        BindEntity bindEntity = new BindEntity();
                        bindEntity.setJson(ApiParamEntity.entityToJson(postParam));
                        bindEntity.setBindParamName(apiStep.getName());
                        bindEntity.setFunctionEntity(apiStep.getFunctionEntity());
                        bindEntities.add(bindEntity);
                        logger.info(ApiParamEntity.entityToJson(postParam));
                    }
                }
            }
            apiPost.bindAllApiReturn(DialogManager.url,bindEntities);

        }catch (Exception e){
            e.printStackTrace();
        }


        return ret;
    }

    /**
     * description: 控制子线程的一个函数，主要是讲主线程等待，继续process线程
     */
    private void continueProcess(){
        try {
            if (processThread == null)
                throw new Exception("要重启动的线程为空");
            processThread.setWhetherInterrupt(false);
            //获取锁
            processThread.getLocker().lock();
            try {
                processThread.getCondition().signal();
            } finally {
                processThread.getLocker().unlock();
            }
            //获取主线程锁
            locker.lock();
            try {
                logger.info("continueProcess+main process start to wait");
                stateMachineCondition.await();
            }
            finally {
                locker.unlock();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * description: 命令处理的一个重要函数，根据输入的命令列表，决定是否有可接受的唯一的不冲突的命令
     * @Param: 输入的命令列表、候选命令列表、后弦重听回复、候选帮助回复
     * @return: 输入的命令类别是否有一个可接受的唯一的命令
     */
    public boolean getTargetCommand(List<SLUResult> commandInput,List<JsonCommand> commandsCandidate,Reply candidateRelistenReply,Reply candidateHelpReply) {
        boolean ret = false;
        try {
            if (commandInput == null || commandsCandidate == null)
                return false;
            for (SLUResult sluResult : commandInput){
                for (JsonCommand jsonCommand : commandsCandidate){
                    if (sluResult.command.equals(jsonCommand.getName())){
                        //根据condition跳转
                        List<Action> actions = null;
                        if (jsonCommand.getTransitions() != null && jsonCommand.getTransitions().size() != 0){
                            for (StepTransition transition : jsonCommand.getTransitions()){
                                if ((boolean)jsEngine.eval(transition.getCondition(),bindings)){
                                    actions = transition.getActions();
                                    break;
                                }
                            }
                            if (actions!=null){
                                ret = true;
                                reply.clear();
                                for (Action action : actions)
                                    action.run(this);
                            }
                        }
                        break;
                    }
                }
            }
            //处理隐藏的帮助和重听
            if (dealHiddenCommand(commandInput,candidateRelistenReply,candidateHelpReply))
                return true;
        }catch (BackToMainMenuException ex){  //异常处理
        } catch(Exception e){
            reset();
            setDialogReply(new Reply(false, "broadcast", "nx_sys_error_01"), null, null);
            //transitServices("IVR MENU");
            StateNode stateNode = new StateNode();
            stateNode.setId("IVR MENU");
            currentState = stateNode;
            e.printStackTrace();
        }
        return ret;
    }


    /**
     * description: 处理隐藏的重听和帮助命令
     * @Param: 输入的命令列表、重听回复、帮助回复
     * @return: 是否命中了重听或者帮助命令
     */
    private boolean dealHiddenCommand(List<SLUResult> commandList,Reply reListenReply,Reply helpReply){
        if (commandList == null)
            return false;
        for (SLUResult sluResult : commandList){
            if(sluResult.command.equals("重听") && reListenReply != null){
                this.reply.clear();
                addDialogReply(translateReply(reListenReply));
                return true;
            }
            else if (sluResult.command.equals("帮助") && helpReply != null){
                this.reply.clear();
                addDialogReply(translateReply(helpReply));
                return true;
            }
        }
        return false;

    }

    public boolean transitToState(String stateId) {
        boolean flag = currentState.getId().equals(stateId) ? false : true;
        if (stateId.isEmpty() || !model.stateNodeMap.containsKey(stateId))
            return false;
        currentState = model.stateNodeMap.get(stateId);
        setDialogReply(currentState.getReply(), currentState.getQuery(), currentState.getParams(), keypad);
        return flag;
    }

    public boolean transitServices(String serviceName) throws BackToMainMenuException{
        try {
            new BackToMainMenu().run(this);
        }finally {
            StateNode stateNode = new StateNode();
            stateNode.setId(serviceName);
            currentState = stateNode;
            return true;
        }

    }



    /**
     * description: nomatch和noinput的处理，实现自动计数
     * @Param: nomatch或者noinput的标记
     */
    public void noMatchOrInput(String note){
        clearNoMatchOrInputCount = false;
        //setFinalSluResult(note);
        NoMatchOrInput noMatchOrInput = null;
        if(note!=null&&note.equals("noMatch")){
            if(currentStep!=null && currentStep.getNoMatch()!= null)
                noMatchOrInput = currentStep.getNoMatch();
            else if (currentParm != null && currentParm.getNoMatch() != null)
                noMatchOrInput = currentParm.getNoMatch();
            else if(currentState!=null && currentState.getNoMatch() != null)
                noMatchOrInput = currentState.getNoMatch();
            if(noMatchOrInput==null)
                noMatchOrInput = root.getNoMatch();
            if(noMatchOrInput==null){
                noMatchOrInput = new NoMatchOrInput();
                List<Reply> replies = new ArrayList<>();
                replies.add(new Reply(false,"synthesis","没有理解您的意思，请重新输入"));
                noMatchOrInput.setNoMatchorinputlist(replies);
            }
        }else {
            if(currentStep!=null && currentStep.getNoInput()!= null)
                noMatchOrInput = currentStep.getNoInput();
            else if (currentParm != null && currentParm.getNoInput() != null)
                noMatchOrInput = currentParm.getNoInput();
            else if(currentState!=null && currentState.getNoInput() != null)
                noMatchOrInput = currentState.getNoInput();
            if(noMatchOrInput==null)
                noMatchOrInput = root.getNoInput();
            if(noMatchOrInput==null){
                noMatchOrInput = new NoMatchOrInput();
                List<Reply> replies = new ArrayList<>();
                replies.add(new Reply(false,"synthesis","没有听到您的声音，请重新输入"));
                noMatchOrInput.setNoMatchorinputlist(replies);
            }
        }
        if (noMatchOrInputCount+1>=noMatchOrInput.getNoMatchorinputlist().size()){
            noMatchOrInputCount = 0;
            setDialogReply(noMatchOrInput.getNoMatchorinputlist().get(noMatchOrInput.getNoMatchorinputlist().size() - 1), null, null);
            try {
                //执行动作
                boolean isNeedBackToMainMenu = true;
                if (noMatchOrInput.getActionlist() != null){
                    for (Action action : noMatchOrInput.getActionlist()){
                        action.run(this);
                        if (action.getClass().getSimpleName().equals("BackToServices"))
                            isNeedBackToMainMenu = false;
                    }
                }
                if (isNeedBackToMainMenu)
                    new BackToMainMenu().run(this);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            setDialogReply(noMatchOrInput.getNoMatchorinputlist().get(noMatchOrInputCount),null,null);
            noMatchOrInputCount++;
        }
    }

    /**
     * description: 控制子线程的一个函数，主要是将主线程等待，启动process线程
     */
    private void startProcess(String processName) throws Exception{

        Process process = statementProcessMap.get(processName);
        if (processThread != null)
        {
            processThread = null;
            throw new Exception("要启动的线程不为空");
        }
        if (process == null)
            throw new Exception("初始化流程找不到");
        processThread = new ProcessThread(this,process);
        processThread.start();
        //获取锁
        locker.lock();
        try {
            logger.info("startProcess+main process start to wait");
            stateMachineCondition.await();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            logger.info("mainThread returned!");
            locker.unlock();
        }

    }



    /**
     * description: 节点之间的匹配
     * @Param: 新的节点的名称、候选节点的名称
     * @return: 两个节点是否match
     */
    private boolean stateMatch(String newSelect, String candicate){

        StateNode newSelectState = getStateFormID(newSelect);
        StateNode candicateState = getStateFormID(candicate);

        if (candicate.equals("##") || newSelect.equals("##"))
            return true;

        if(newSelectState==null||candicateState==null)
            return false;

        if(newSelectState.isAncestor(candicateState))
            return true;

        return false;

    }

    /**
     * description: 节点之间数据的匹配
     * @Param: 新节点的slot的值，候选节点slot的值
     * @return: 两个节点的数据是否match
     */
    private boolean dataMatch(Map<String, String> slotsNewSelect, Map<String, String> slotsCandicate){

        for (String key:slotsCandicate.keySet()){
            String valueNewSelect = slotsNewSelect.get(key);
            String valueCandicate = slotsCandicate.get(key);

            if(valueCandicate!=null&&valueNewSelect!=null
                    &&!valueCandicate.equals("")&&!valueNewSelect.equals("")
                    &&!valueCandicate.equals(valueNewSelect))
                return false;
        }

        return true;
    }

    /**
     * description: 合并连个理解结果
     * @Param: 新的理解结果、候选理解结果
     * @return: 合并后的新的理解结果
     */
    private SLUResult mergeSLU(SLUResult newSelect, SLUResult candicate){
        Map<String,String> slots = new HashMap<String,String>();

        SLUResult newSLU = new SLUResult(candicate.stateId.equals("##") ? newSelect.stateId : candicate.stateId,candicate.score,
                slots);

        Map<String, String> slotsNewSelect = newSelect.slots;
        Map<String, String> slotsCandicate = candicate.slots;



        for (String key:slotsCandicate.keySet()){

            String valueNewSelect = slotsNewSelect.get(key);
            String valueCandicate = slotsCandicate.get(key);

            if(valueCandicate.equals("")&&valueNewSelect!=null)
                slots.put(key,valueNewSelect);
            else
                slots.put(key,valueCandicate);

        }

        for (String key:slotsNewSelect.keySet()){

            String valueNewSelect = slotsNewSelect.get(key);
            String valueCandicate = slotsCandicate.get(key);

            if(valueNewSelect.equals("")&&valueCandicate!=null)
                slots.put(key,valueCandicate);
            else
                slots.put(key,valueNewSelect);

        }

        return newSLU;
    }
    /**
     * description: 理解结果列表和一个理解结果匹配，然后merge，返回最终的merge结果，若不能merge，则返回空
     * @Param: 理解结果列表、期望的理解结果
     * @return: 最终得到的理解结果
     */
    private SLUResult determinedbySLUList(List<SLUResult> candicateList, SLUResult select){

        SLUResult determineSLU = null;

        for (SLUResult candicate:candicateList){

            if(stateMatch(select.stateId,candicate.stateId)
                    &&dataMatch(select.slots, candicate.slots)){

                if(determineSLU!=null)
                    return null;
                determineSLU = mergeSLU(select,candicate);

            }
        }
        return determineSLU;

    }
    /**
     * description: 理解结果列表进行merge
     * @Param: 理解结果列表
     * @return: merge后的理解结果
     */
    private SLUResult mergeSLUList(List<SLUResult> candicateList){

        SLUResult determineSLU = null;

        for (SLUResult candicate:candicateList){

            if(determineSLU==null) {
                determineSLU = candicate;
                continue;
            }

            if(stateMatch(determineSLU.stateId,candicate.stateId)
                    &&dataMatch(determineSLU.slots, candicate.slots)){
                determineSLU = mergeSLU(determineSLU,candicate);
            }
            else if(stateMatch(candicate.stateId,determineSLU.stateId)
                    &&dataMatch(candicate.slots,determineSLU.slots)){
                determineSLU = mergeSLU(candicate,determineSLU);
            }
            else
                return null;

        }
        return determineSLU;

    }
    /**
     * description: 将节点信息转化为理解结果对象格式
     * @Param: 节点对象
     * @return: 理解结果对象
     */
    private SLUResult makeSLUfromStateNode(StateNode state){

        Map<String,String> slots = new HashMap<String, String>();
        try {
            //添加当前的param
            if (currentParm != null)
                slots.put(currentParm.getName(),"");
            //ASRStep中的要绑定变量
            if (currentStep!=null && processThread != null && processThread.isWhetherInterrupt()==true){ //有等待的
                if (currentStep.getClass().getSimpleName().equals("ParamASRStep")){
                    String paramKey = ((ParamASRStep) currentStep).getBindParam();
                    slots.put(paramKey,statementParamMap.get(paramKey).getValue());
                }
                else if (currentStep.getClass().getSimpleName().equals("KeypadStep")){
                    String paramKey = ((KeypadStep) currentStep).getKeypadInputParam();
                    slots.put(paramKey,statementParamMap.get(paramKey).getValue());
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }


        return new SLUResult(state.getId(),1.0,slots);
    }

    /**
     * description: 根据冲突列表过滤理解结果，生成最终的一个理解结果，输入的理解结果不能解决冲突列表中的冲突，生成唯一的最终的理解结果，则返回为空
     * @Param: 理解结果列表
     * @return: 最终得到的理解结果
     */
    private SLUResult determineByConflictList(List<SLUResult> userInput){

        SLUResult nextSelect = null;

        if(!conflictList.isEmpty()){
            List<SLUResult> matchConflictList = new ArrayList<SLUResult>();
            for (SLUResult newSelect:userInput){
                SLUResult determineSLU = determinedbySLUList(conflictList,newSelect);
                if(determineSLU!=null)
                    matchConflictList.add(determineSLU);
            }
            conflictList.clear();
            nextSelect = mergeSLUList(matchConflictList);
        }

        return nextSelect;

    }
    /**
     * description: 根据当前状态过滤理解结果，生成最终的一个理解结果，若当前状态不能决定理解结果列表中哪一个是唯一合理的，则返回为空
     * @Param: 理解结果列表
     * @return: 最终得到的理解结果
     */
    private List<SLUResult> generateMatchListByCurrentState(List<SLUResult> userInput){
        List<SLUResult> matchuserInput = new ArrayList<SLUResult>();
        SLUResult currentStateSLU = makeSLUfromStateNode(currentState);
        for (SLUResult input: userInput){
            if(stateMatch(currentStateSLU.stateId,input.stateId)
                    &&dataMatch(currentStateSLU.slots,input.slots)){
                matchuserInput.add(mergeSLU(currentStateSLU,input));
            }
        }
        return matchuserInput;

    }


    /**
     * description: 根据冲突列表过滤理解结果，但是当前状态不考虑变量的matche，生成最终的一个理解结果，若当前状态不能决定理解结果列表中哪一个是唯一合理的，则返回为空
     * @Param: 理解结果列表
     * @return: 最终得到的理解结果
     */
    private List<SLUResult> generateMatchListByCurrentStateWithoutData(List<SLUResult> userInput){
        List<SLUResult> matchuserInputID = new ArrayList<SLUResult>();
        SLUResult currentStateSLU = makeSLUfromStateNode(currentState);
        for (SLUResult input:userInput){
            if(stateMatch(currentStateSLU.stateId,input.stateId)){
                matchuserInputID.add(mergeSLU(currentStateSLU,input));
            }
        }
        return matchuserInputID;
    }

    /**
     * description: 根据过个理解结果，设置冲突回复
     * @Param: 理解结果列表
     */
    private void setConflictReply(List<SLUResult> matchConflictList){
        conflictList = matchConflictList;
        String broadcastReply = getBroadcastReply();
        List<Reply>  broadcastReplys = synthesisReplyNumberControl(broadcastReply, true);
        setDialogReply(broadcastReplys, null, null,keypad);
    }



    //控制合成的回复内容的字数，不超过150个字。超过了变为下一条
//    public List<Reply> synthesisReplyNumberControl(String synthesisReply,boolean interrupt){
//        final int limitLen = 150;
//        List<Reply> replyContents = new ArrayList<>();
//        if (synthesisReply.length()<limitLen)
//            replyContents.add(new Reply(interrupt,"synthesis",synthesisReply));
//        else {
//
//            String[] puncs = {"，","。","；","？","！",",",";","\\?","!"};
//            List<String> sentences = new ArrayList<>();
//            sentences.add(synthesisReply);
//
//            for (int i=0;i<puncs.length;i++) {
//                List<String> tmp = new ArrayList<>();
//                for (String str:sentences) {
//                    String[] sentences1 = str.split(puncs[i]);
//                    for (int j=0;j<sentences1.length-1;j++){
//                        tmp.add(sentences1[j]+puncs[i]);
//                    }
//                    tmp.add(sentences1[sentences1.length-1]);
//                }
//                sentences = tmp;
//            }
//
//            List<String> sentences2 = new ArrayList<>();
//            for (String str : sentences) {
//                if(str.length()>limitLen) {
//                    int i=0;
//                    while (i<str.length()) {
//                        sentences2.add(str.substring(i,Math.min(i+limitLen,str.length())));
//                        i+=limitLen;
//                    }
//                }
//                else
//                    sentences2.add(str);
//            }
//
//            String content = "";
//            for (String str : sentences2){
//                if(content.length()+str.length()>limitLen){
//                    replyContents.add(new Reply(interrupt,"synthesis",content));
//                    content = str;
//                }else {
//                    content+=str;
//                }
//            }
//
//            if(!content.equals(""))
//                replyContents.add(new Reply(interrupt,"synthesis",content));
//        }
//        return replyContents;
//    }

    /**
     * description: 将合成回复内容拆分成多个回复的list，每个限制在一定的字数
     * @Param: 回复内容、是否可打断
     * @return: 回复列表
     */
    public List<Reply> synthesisReplyNumberControl(String synthesisReply,boolean interrupt){

        List<Reply> replyContents = new ArrayList<>();
        replyContents.add(new Reply(interrupt,"synthesis",synthesisReply));

        return replyContents;
    }


    /**
     * description: 将多个理解结果进行merge
     * @Param: 理解结果列表
     * @return: 合并后的结果
     */
    private SLUResult determineByMatchList(List<SLUResult> matchuserInput){
        if (matchuserInput == null || matchuserInput.isEmpty())
            return null;
        SLUResult mergeMatch = mergeSLUList(matchuserInput);
        if(mergeMatch!=null)
            return mergeMatch;
        else {
            //添加一个步骤，即冲突列表是否命中了某个解决冲突节点
            SLUResult conflictSlu = getConflictSlu(matchuserInput);
            if(conflictSlu != null){
                conflictList.addAll(getConflictSluList(matchuserInput, conflictSlu)); //去除冲突节点，将冲突列表中的节点添加到conflictList
                //setFinalSluResult(conflictSlu);
                return conflictSlu;
            }

            for (SLUResult sluResult : matchuserInput){
                if (sluResult.stateId.equals("路况查询")){
                    setFinalSluResult(sluResult);
                    return sluResult;
                }
            }
            //setFinalSluResult(matchuserInput);
            setFinalSluResult("歧义流程");
            setConflictReply(matchuserInput);
            return null;
        }

    }
    /**
     * description: 从理解结果中去除掉一个
     * @Param: 理解结果列表、要去除的理解结果
     * @return: 去除后的理解结果列表
     */
    private List<SLUResult> getConflictSluList(List<SLUResult> userInput,SLUResult conflictState){


        List<SLUResult> ret = new ArrayList<>();
        for (SLUResult sluResult : userInput){
            if (!sluResult.stateId.equals(conflictState.stateId))
                ret.add(new SLUResult(sluResult.stateId,1.0,new HashMap<>()));
        }
        return ret;

    }
    /**
     * description: 将string类型的列表，转为SLUResult的对象的列表（即理解结果列表）
     * @Param: string类型的列表
     * @return: SLUResult的对象的列表（即理解结果列表）
     */
    public List<SLUResult> getConflictSluListFromString(List<String> userInput){


        List<SLUResult> ret = new ArrayList<>();
        for (String str : userInput){
            ret.add(new SLUResult(str,1.0,new HashMap<>()));
        }
        return ret;

    }
    /**
     * description:对于输入的多个命令类型的理解结果，根据当前状态进行过滤，看能否解决命令的冲突，确定唯一一个命令作为最终理解结果
     * @Param: 多个命令类型的理解结果
     * @return: 看能否解决命令的冲突，确定唯一一个命令作为最终理解结果
     */
    public boolean determineNextCommand(List<SLUResult> commandInput){

        //有正在等待的ASRStep
        if (commandInput.size() != 0 && processThread != null && processThread.isWhetherInterrupt() == true){
            if (currentStep != null && currentStep.getClass().getSimpleName().equals("CommandASRStep")) {
                List<String> matchedCommandList = new ArrayList<>();
                CommandASRStep step = (CommandASRStep) currentStep;
                for (SLUResult sluResult : commandInput) {
                    if (step.getCommandList().contains(sluResult.command))
                        matchedCommandList.add(sluResult.command);
                }
                if (matchedCommandList.size() == 1) {
                   // step.setMatchedCommand(matchedCommandList.get(0));
                    bindings.put("command", matchedCommandList.get(0));
                    this.reply.clear();
                    continueProcess();
                    return true;
                }
            }
            //处理asrStep中的command,处理隐含的帮助和重听command
            if (currentStep != null && ASRStep.class.isAssignableFrom(currentStep.getClass())) {
                ASRStep step = (ASRStep) currentStep;
                if (getTargetCommand(commandInput,step.getCommands(),step.getReListenReply(),step.getHelpReply())){
                    clearNoMatchOrInputCount = false;
                    return true;
                }
            }
        }

        //如果当前有正在提问的param
        if (currentParm != null){
            if (getTargetCommand(commandInput,currentParm.getCommands(),currentParm.getReListenReply(),currentParm.getHelpReply())){
                clearNoMatchOrInputCount = false;
                return true;
            }
        }

        //根据节点下的command命令强制跳转,处理隐含的帮助和重听command
        if (getTargetCommand(commandInput,currentState.getCommandsTransitions(),currentState.getReListenReply(),currentState.getHelpReply())){
            clearNoMatchOrInputCount = false;
            return true;
        }

        //全局命令,处理隐含的帮助和重听command
        if (getTargetCommand(commandInput,root.getCommandsTransitions(),root.getReListenReply(), root.getHelpReply())){
            clearNoMatchOrInputCount = false;
            return true;
        }




        return false;

    }
    /**
     * description:对于理解结果的过滤，去除掉id=“##”，slot为空的无效节点
     * @Param: 理解结果列表
     * @return: 是否成功
     */
    public boolean deleteNullInput(List<SLUResult> userInput){
        if (userInput == null)
            return true;
        for (Iterator<SLUResult> it = userInput.iterator(); it.hasNext();) {
            SLUResult sluResult = it.next();
            if (sluResult.stateId.equals("##") && sluResult.slots.isEmpty())
                it.remove();
        }
        return true;
    }



    /**
     * description: 对于输入的多个节点型的理解结果，根据优先级一层一层的过滤，解决冲突，得到最终的唯一的理解结果
     * @Param: 理解结果列表、冲突回复内容
     * @return: 最终的唯一的理解结果
     */
    public SLUResult determineNextState(List<SLUResult> userInput,String conflictReply){

        //stateIDInput，去掉不需要的param
        pretreatment(userInput);
        if (deleteNullInput(userInput) && userInput.size() ==0){
            noMatchOrInput("noMatch");
            return null;
        }

        SLUResult nextSelect = determineByConflictList(userInput);
        if(nextSelect!=null)
            return nextSelect;

        if(userInput.size()==1)
            return userInput.get(0);
        List<SLUResult> matchuserInput = genarateMatchListByCurrentStep(userInput);
        if (!matchuserInput.isEmpty()){
            if (determineByMatchList(matchuserInput)!=null)
                return determineByMatchList(matchuserInput);
        }
        if (currentStep!= null && currentStep.isJumpOut() == false) {
            noMatchOrInput("noMatch"); //配置回复
            return null;
        }
        if (currentParm != null)
            matchuserInput = genarateMatchListByCurrentParam(userInput);

        if(!matchuserInput.isEmpty()){
            return determineByMatchList(matchuserInput);
        }
        matchuserInput = generateMatchListByCurrentState(userInput);

        if(!matchuserInput.isEmpty()){
            return determineByMatchList(matchuserInput);
        }

        matchuserInput = generateMatchListByCurrentStateWithoutData(userInput);
        if(!matchuserInput.isEmpty()){
           return determineByMatchList(matchuserInput);
        }

        return determineByMatchList(userInput);

    }
    /**
     * description: 遍历所有的节点，看是否有冲突节点，其中定义的冲突list可以包含输入的理解结果，有则返回该冲突节点的理解对象
     * @Param: 理解结果列表
     * @return: 冲突节点的理解对象
     */
    private SLUResult getConflictSlu(List<SLUResult> userInput){


        List<String> userInputStateidList = new ArrayList<>();
        for (SLUResult sluResult : userInput){
            userInputStateidList.add(sluResult.stateId);
        }
        for (Map.Entry<String,StateNode> entry : model.stateNodeMap.entrySet()){
            List<String> tempt = new ArrayList<>();
            tempt.addAll(userInputStateidList);
            if (tempt.contains(entry.getKey()))
                tempt.remove(entry.getKey());
            if (entry.getValue().getConflictList() != null && entry.getValue().getConflictList().containsAll(tempt))
               return new SLUResult(entry.getValue().getId(),1.0,new HashMap<>());
        }
        return null;

    }
    /**
     * description: 根据当前的step过滤理解结果
     * @Param: 理解结果列表
     * @return: 跟当前step能够matche的理解结果列表
     */
    private List<SLUResult> genarateMatchListByCurrentStep(List<SLUResult> userInput){

        List<SLUResult> matchuserInput = new ArrayList<SLUResult>();
        if (currentStep == null)
            return matchuserInput;
        SLUResult currentStateSLU = makeSLUfromStateNode(currentState);
        for (SLUResult input: userInput){
            if(stateMatch(currentStateSLU.stateId,input.stateId)
                    &&dataMatch(currentStateSLU.slots, input.slots)){
                matchuserInput.add(mergeSLU(currentStateSLU,input));
            }
        }
        return matchuserInput;
    }
    /**
     * description: 根据当前的变量过滤理解结果
     * @Param: 理解结果列表
     * @return: 跟当前变量能够matche的理解结果列表
     */
    public List<SLUResult> genarateMatchListByCurrentParam(List<SLUResult> userInput){
        List<SLUResult> ret = new ArrayList<>();
        ret.addAll(userInput);
        //userInput处理，针对上一轮是提问的参数。将结果中不包含上一轮参数的节点去掉。
        for (Iterator<SLUResult> it = ret.iterator(); it.hasNext();) {
            SLUResult sluResult = it.next();
            if (!sluResult.slots.keySet().contains(currentParm.getName())){
                it.remove();
                continue;
            }
        }
        return ret;
    }
    /**
     * description: 根据冲突列表生成冲突回复
     * @Param:
     * @return:
     */
    public String getBroadcastReply(){
        //生成播报reply
        String broadcastReply = "您需要哪项业务呢：";
        List<String> result = new ArrayList<>();
        List<StateNode> conflictStateNode = new ArrayList<StateNode>();
        List<SLUResult> arrange = arrangeResult(root,conflictList);
        for (SLUResult sluResult : arrange)
            conflictStateNode.add(getStateFormID(sluResult.stateId));
        for (StateNode stateNode : conflictStateNode){
            List<StateNode> tempt = new ArrayList<StateNode>();
            tempt.addAll(conflictStateNode);
            tempt.remove(stateNode);
            if (!stateNode.isAncestor(tempt)){
                String name = stateNode.getId().replaceAll("nx\\d+_", "");
                result.add(name);
            }

        }
        return result.isEmpty()? "null" : (broadcastReply+Joiner.on(",").join(result));

    }


    /**
     * description: 清空临时变量的值
     * @Param: 节点对象
     * @return: 是否清理成功
     */
    public boolean clearStateData(StateNode stateNode){
//       //节点下的参数值清空
//        if (stateNode != null && stateNode.getParams() != null){
//            for (String str : stateNode.getParams())
//                statementParamMap.get(str).setValue("");
//        }
        //清空runtime是local的清空
        for (Map.Entry<String,Param> entry: statementParamMap.entrySet()){
            String tempt = entry.getValue().getScope().trim().toLowerCase();
            if (entry.getValue().getRuntime().trim().toLowerCase().equals("local"))
                entry.getValue().setValue("");
        }
        return true;
    }

    /**
     * description: 设置对话回复
     * @Param: 回复对象、动态函数名字、参数列表
     */
    public void setDialogReply(Reply reply,String query,List<String> params){
        List<Reply> replies = new ArrayList<>();
        replies.add(reply);
        setDialogReply(replies, null, null,keypad);
    }

    /**
     * description: 设置对话回复
     * @Param: 回复对象、动态函数名字、参数列表、是否按键输入
     * @return:
     */
    public void setDialogReply(List<Reply> reply,String query,List<String> params,boolean keypad){
        this.actionParam.clear();
        this.reply.clear();
        List<Reply> newReplies = new ArrayList<>();
        for (Reply oneReply : reply){
            Reply newReply = new Reply(oneReply.isInterrupt(),oneReply.getProperty(),oneReply.getContent());
            String content = newReply.getContent();
            if (content.matches(".*\\$.*\\$.*"))
                newReply.setContent(escapeOption(newReply.getContent()));
            newReplies.add(newReply);
        }
        if (newReplies != null)
            this.reply.addAll(newReplies);
        this.query=query;
        if (!(params == null))
            for (String str:params)
                this.actionParam.put(str,statementParamMap.get(str).getValue());
        this.keypad = keypad;
    }
    /**
     * description: 添加一个对话回复
     * @Param: 回复对象
     */
    public void addDialogReply(Reply reply){
        if (reply != null) {
            Reply newReply = new Reply(reply.isInterrupt(),reply.getProperty(),reply.getContent());
            newReply.setContent(escapeOption(newReply.getContent()));
            this.reply.add(newReply);
        }
    }
    /**
     * description: 设置对话回复
     * @Param: 回复对象、动态函数名字、参数列表、是否按键输入
     * @return:
     */
    private Set<String> getParamCandidate(SLUResult sluResult) throws Exception{
        Set<String> ret = new HashSet<>();
        StateNode state = model.stateNodeMap.get(sluResult.stateId);
        if (state != null){
            //reply中依赖的变量（递归查找）
            for (Reply reply : state.getReply()){
                List<BasicParam> params = ContainParams(reply.getContent());
                for (BasicParam basicParam : params){
                    ret.add(basicParam.getName());
                    basicParam.generateAllChildren(this);
                    for (BasicParam basicParam1 : basicParam.getAllChildren())
                        ret.add(basicParam1.getName());
                }
            }
            //将sluResult对应的节点的process中对应
            if (statementProcessMap.get(state.getInit_process()) != null){
                List<Step> steps = statementProcessMap.get(state.getInit_process()).getSteps();
                if (steps != null){
                    for (Step step : steps){
                        if (step.getClass().getSimpleName().equals("ParamASRStep"))
                            ret.add(((ParamASRStep)step).getBindParam());
                    }
                }
            }
        }
        return ret;
    }
    /**
     * description: 理解结果列表的预处理，去掉无效的理解结果和理解结果中无效的slot
     * @Param: 理解结果列表
     */
    public void pretreatment(List<SLUResult> userInput){

        SLUResult currentSluResult = makeSLUfromStateNode(currentState);
        Map<String,String> currentSlots = currentSluResult.slots;
        for (Iterator<SLUResult> it = userInput.iterator(); it.hasNext();) {
            SLUResult sluResult = it.next();
            if (!sluResult.stateId.isEmpty()){
                //去掉没有的节点
                if (!sluResult.stateId.equals("##") && getStateFormID(sluResult.stateId) == null){
                    it.remove();
                    continue;
                }
                try {
                    Set<String> candidateParams = getParamCandidate(sluResult);
                    //去掉currentSlots中没有的变量
                    if (sluResult.slots != null){
                        Set<Map.Entry<String,String>> entrys = sluResult.slots.entrySet();
                        for(Iterator i = entrys.iterator();i.hasNext();) {
                            Map.Entry entry = (Map.Entry)i.next();
                            if (!currentSlots.containsKey(entry.getKey()) && !candidateParams.contains(entry.getKey()))
                                i.remove();
                        }
                    }
                }catch (Exception e){
                    logger.info("参数未在params或者apiList中定义.");
                    e.printStackTrace();
                }
            }
        }
        //重新遍历，userInput，去掉checkParamJs=false的输入结果
        for (Iterator<SLUResult> it = userInput.iterator();it.hasNext();){
            try {
                SLUResult sluResult = it.next();
                if (sluResult.stateId.equals("##") && currentStep != null && currentStep.getCheckParamJs() != null && !currentStep.getCheckParamJs().isEmpty()){
                    for (Map.Entry<String,String> entry : sluResult.slots.entrySet())
                        bindings.put(entry.getKey(),entry.getValue());
                    boolean checkResult = (boolean)jsEngine.eval(currentStep.getCheckParamJs(),bindings);
                    for (Map.Entry<String,String> entry : sluResult.slots.entrySet())
                        bindings.remove(entry.getKey());
                    if (!checkResult)
                        it.remove();
                }
            }catch (Exception e){
                logger.info("根据CheckParamJs过滤结果时报错！");
                e.printStackTrace();
            }
        }
    }





    public List<SLUResult> arrangeResult(StateNode stateNode,List<SLUResult> results){


        if (stateNode == null)
            return results;

        List<SLUResult> ret = new ArrayList<SLUResult>();

        if(stateNode.getSubStateNode()!=null){
            for (StateNode child : stateNode.getSubStateNode()){
                List<SLUResult> childret = arrangeResult(child,results);
                ret.addAll(childret);
            }
        }


        for(SLUResult slu:results){
            if(slu.stateId.equals(stateNode.getId()))
                ret.add(slu);
        }

        return ret;


    }
    /**
     * description: 首问语的处理流程
     */
    public void greeting(){
        try {
            String processName = model.getConfig().getGreeting().getInit_process();
            if (!processName.trim().equals("")) startProcess(processName);
            if ((model.getConfig().getGreeting().getReply() != null && model.getConfig().getGreeting().getReply().size()!= 0) && (processThread == null || !processThread.isWhetherInterrupt())) { //确保没有线程在等待了
                for (Reply reply : model.getConfig().getGreeting().getReply()){
                    List<Reply> generateReplys = generateReply(reply);
                    for (Reply reply1 : generateReplys)
                        addDialogReply(reply1);
                    if (currentParm != null)
                        break;
                }
            }
        } catch (SocketTimeoutException | BossException e){ //boss异常处理，转人工
            reset();
            setDialogReply(new Reply(false, "broadcast", "nx_sys_error_02"), null, null);
            StateNode stateNode = new StateNode();
            stateNode.setId("IVR MENU");
            currentState = stateNode;
            e.printStackTrace();
        } catch(Exception e){ //对话管理器异常处理，转ivr
            reset();
            setDialogReply(new Reply(false, "broadcast", "nx_sys_error_01"), null, null);
            StateNode stateNode = new StateNode();
            stateNode.setId("IVR MENU");
            currentState = stateNode;
            e.printStackTrace();
        }

    }

    public String escapeOption(String input){
        String ret = "";
        try {
            if (bindings != null){
                bindings.put("moban", input);
                ret = (String)jsEngine.eval("synthesisReply(moban)", bindings);
            }
        }catch (Exception e){
//            List<BasicParam> params = ContainParams(ret);
//            if (params != null){
//                for (BasicParam basicParam : params){
//                    ret.replace("$"+basicParam.getName()+"$",statementParamMap.get(basicParam.getName()).getValue());
//                }
//            }
        }

        return ret;
    }

    public void updataParamInBindings() throws Exception{
        if (bindings != null){
            for (Map.Entry<String,Object> entry : bindings.entrySet()){
                if (statementParamMap.get(entry.getKey()) != null){
                    //String tempt = (String)jsEngine.eval(entry.getKey());
                    bindings.put(entry.getKey(),(String)jsEngine.eval(entry.getKey()));
                    statementParamMap.get(entry.getKey()).setValue((String)jsEngine.eval(entry.getKey()));
                }
            }
        }
    }

    public void updataBindingsFromParamMap() throws Exception{
        if (bindings != null){
            for (Map.Entry<String,Param> entry : statementParamMap.entrySet()){
                bindings.put(entry.getKey(),entry.getValue().getValue());
            }
        }
    }

    private Reply translateReply(Reply reply){

        Reply ret = new Reply();
        ret.setInterrupt(reply.isInterrupt());
        ret.setProperty(reply.getProperty());
        String content = reply.getContent();
        ret.setContent(content);
        if (content.matches(".*\\$.*\\$.*"))
            ret.setContent(escapeOption(content));
        return ret;

    }

    public void newApiParamEntity(String userID,Map<String,String> crsParam){
        ApiParamEntity oldApiParamEntity = apiParamEntity;
        apiParamEntity = new ApiParamEntity();
        apiParamEntity.setUserid(userID);
        apiParamEntity.setInter_idx(crsParam.get("inter_idx"));
        apiParamEntity.setTime(crsParam.get("start_time"));
        apiParamEntity.setBusiness_name(oldApiParamEntity.getBusiness_name());
        apiParamEntity.setInterface_idx(oldApiParamEntity.getInterface_idx());
    }



    public void setFinalSluResult(SLUResult finalSluResult) {
        if (finalSluResult == null)
            return;
        if (!finalSluResult.stateId.isEmpty()){
            if (!finalSluResult.stateId.equals("##")){
                this.finalSluResult.clear();
                if(finalSluResult.stateId.split("_").length >= 2)
                    this.finalSluResult.add(finalSluResult.stateId.split("_")[1]);
                else this.finalSluResult.add(finalSluResult.stateId);
            }
        }
        else if (!finalSluResult.command.isEmpty()){
            this.finalSluResult.clear();
            this.finalSluResult.add(finalSluResult.command);
        }

        else ;

    }








    public StateNode getCurrentState() {
        return currentState;
    }

    public void setCurrentState(StateNode currentState) {
        this.currentState = currentState;
    }




    public List<Reply> getReply() {
        return reply;
    }

    public void setReply(List<Reply> reply) {
        this.reply = reply;
    }

    public boolean isNodeTransited() {
        return isNodeTransited;
    }

    public void setIsNodeTransited(boolean isNodeTransited) {
        this.isNodeTransited = isNodeTransited;
    }

    public boolean isSessionEnd() {
        return sessionEnd;
    }

    public void setSessionEnd(boolean sessionEnd) {
        this.sessionEnd = sessionEnd;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String,String> getParam() {
        return actionParam;
    }

    public void setParam(Map<String,String> param) {
        this.actionParam = param;
    }

    public List<SLUResult> getConflictList() {
        return conflictList;
    }

    public void setConflictList(List<SLUResult> conflictList) {
        this.conflictList = conflictList;
    }

    public void setUseContext(boolean useContext) {
        isUseContext = useContext;
    }

    public Map<String, Param> getStatementParamMap() {
        return statementParamMap;
    }

    public void setStatementParamMap(Map<String, Param> statementParamMap) {
        this.statementParamMap = statementParamMap;
    }


    public Process getCurrentProcess() {
        return currentProcess;
    }

    public void setCurrentProcess(Process currentProcess) {
        this.currentProcess = currentProcess;
    }

    public ASRStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(ASRStep currentStep) {
        this.currentStep = currentStep;
    }

    public Map<String, Process> getStatementProcessMap() {
        return statementProcessMap;
    }

    public void setStatementProcessMap(Map<String, Process> statementProcessMap) {
        this.statementProcessMap = statementProcessMap;
    }

    public ProcessThread getProcessThread() {
        return processThread;
    }

    public void setProcessThread(ProcessThread processThread) {
        this.processThread = processThread;
    }

    public ReentrantLock getLocker() {
        return locker;
    }

    public Condition getStateMachineCondition() {
        return stateMachineCondition;
    }

    public void setStateMachineCondition(Condition stateMachineCondition) {
        this.stateMachineCondition = stateMachineCondition;
    }

    public ScriptEngine getJsEngine() {
        return jsEngine;
    }

    public Bindings getBindings() {
        return bindings;
    }

    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }

    public boolean isKeypad() {
        return keypad;
    }

    public void setKeypad(boolean keypad) {
        this.keypad = keypad;
    }

    public boolean isClearNoMatchOrInputCount() {
        return clearNoMatchOrInputCount;
    }

    public void setClearNoMatchOrInputCount(boolean clearNoMatchOrInputCount) {
        this.clearNoMatchOrInputCount = clearNoMatchOrInputCount;
    }

    public int getNoMatchOrInputCount() {
        return noMatchOrInputCount;
    }

    public void setNoMatchOrInputCount(int noMatchOrInputCount) {
        this.noMatchOrInputCount = noMatchOrInputCount;
    }

    public Param getCurrentParm() {
        return currentParm;
    }

    public void setCurrentParm(Param currentParm) {
        this.currentParm = currentParm;
    }

    public List<String> getFinalSluResult() {
        return finalSluResult;
    }

    public void setFinalSluResult(String finalSluResult) {
        this.finalSluResult.clear();
        this.finalSluResult.add(finalSluResult);

    }

}

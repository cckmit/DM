package entities.step;

import DM.StateMachine;
import entities.Reply;
import entities.action.Action;
import exception.NoMatchConditionException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * 需要填充变量值的asrStep
 */
public class ParamASRStep extends ASRStep{

    String bindParam;

    /**
     * description: 查看该step中依赖的变量值是否为空，肉为空，则暂停线程，等待用户输入，否则的话判断condition，执行满足condition条件的action
     * @Param: 状态机
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            onEnter(stateMachine);

            //如果是以下几个流程，当用户未说出月份时，默认为当月进行处理
            HashSet<String> businesSet = new HashSet<String>() {{
                add("nx005_查询账单");
                add("nx120_核对话费去向");
                add("nx138_核对通讯费产生");
                add("nx139_核对流量额外收费");
                add("nx140_核对增值业务费");
                add("nx141_核对其他费用");
            }};
            String processName = stateMachine.getCurrentProcess().getName();
            if (businesSet.contains(processName) && bindParam.equals("month") &&
                    stateMachine.getStatementParamMap().get(bindParam).getValue().isEmpty()){
                Calendar cale = Calendar.getInstance();
                int year = cale.get(Calendar.YEAR);
                int currentMonth = cale.get(Calendar.MONTH) + 1;
                String month = "";
                if(currentMonth >= 1 && currentMonth <= 9){
                    month = String.valueOf(year) + "0" + String.valueOf(currentMonth);
                    stateMachine.getStatementParamMap().get(bindParam).setValue(String.valueOf(month));
                }
                if(currentMonth >= 10){
                    month = String.valueOf(year) + String.valueOf(currentMonth);
                    stateMachine.getStatementParamMap().get(bindParam).setValue(String.valueOf(month));
                }
            }//如果是以上业务设置默认参数month

            if(stateMachine.getStatementParamMap().get(bindParam).getValue().isEmpty()){
                interrupt(stateMachine);
                run(stateMachine);
            }
            else {
                //绑定变量
                stateMachine.getBindings().put(bindParam,stateMachine.getStatementParamMap().get(bindParam).getValue());
                //根据condition跳转
                List<Action> actions = null;
                if (transitions.size() != 0){
                    for (StepTransition transition : transitions){
                        if ((boolean)stateMachine.getJsEngine().eval(transition.getCondition(),stateMachine.getBindings())){
                            actions = transition.getActions();
                            break;
                        }
                    }
                    if (actions == null)
                        throw new NoMatchConditionException();
                    //执行action
                    for (Action action : actions)
                        action.run(stateMachine);
                }
            }
        }finally {
            onExit(stateMachine);
        }
    }



    public String getBindParam() {
        return bindParam;
    }

    public void setBindParam(String bindParam) {
        this.bindParam = bindParam;
    }



}

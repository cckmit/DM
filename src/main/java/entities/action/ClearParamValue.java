package entities.action;

import DM.StateMachine;


/**
 * 清空变量的值的action类
 */
public class ClearParamValue extends Action {

    public ClearParamValue(){
        super();
    }
    /**
     * * description: 清空变量的值
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try{
            onEnter(stateMachine);
            for (String str : actionParams){
                if (stateMachine.getStatementParamMap().get(actionParams.get(0)) != null)
                    stateMachine.getStatementParamMap().get(actionParams.get(0)).setValue("");
                if(stateMachine.getBindings().get(str) !=null )
                    stateMachine.getBindings().remove(str);
                
            }

        }finally {
            onExit(stateMachine);
        }

    }
}

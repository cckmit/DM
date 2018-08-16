package entities.action;


import DM.StateMachine;

/**
 * 执行一段js代码的action类
 */
public class JsAction extends Action{
    /**
     * * description: 执行一段js代码
     * @Param: 状态机对象
     */
    @Override
    public void run(StateMachine stateMachine) throws Exception{
        try {
            onEnter(stateMachine);
            //System.out.print(stateMachine.getJsEngine().eval("password"));
            stateMachine.getJsEngine().eval(actionParams.get(0), stateMachine.getBindings());
            System.out.print(stateMachine.getJsEngine().eval("password"));
            stateMachine.updataParamInBindings();
            //stateMachine.getBindings().
            //System.out.print(stateMachine.getBindings().get("password"));
        }finally {
            onExit(stateMachine);
        }

    }

}

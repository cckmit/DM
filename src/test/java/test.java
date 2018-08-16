import DM.DialogManager;
import DM.StateMachine;
import entities.SLUResult;
import entities.StateMachineModel;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xjzhang on 2017/2/21.
 */
public class test {

    //String[][]每行是一个slot
    private static List<SLUResult> simpleSLURes(String[] stateID,String[][] slotInput) {
        ArrayList<SLUResult> res = new ArrayList<SLUResult>();

        int i=0;
        for (String e : stateID) {
            Map<String, String> slot=new HashMap<String, String>();
            for (int j=0;j<slotInput[i].length;j=j+2)
                slot.put(slotInput[i][j], slotInput[i][j + 1]);
           // Map<String,String> controversy=new HashMap<String, String>();
            res.add(new SLUResult(e, 1.0,slot));
            i++;
        }
        return res;
    }

    public static void main(String[] args) throws IOException {

//        try {
//            StateMachineModel model = StateMachineModel.getModelFromInternelJson("lib/nodeInfo/nodeInfo.json",StateMachineModel.class);
//            DialogManager dialogManager=new DialogManager(50,true);
//            dialogManager.setStateMachineModel(model);
//            String userID = "567";
//            long start = System.currentTimeMillis();
//            List<SLUResult> test1 = new ArrayList<SLUResult>();
//            Map<String,String> slot1 = new HashMap<String, String>();
//            SLUResult a = new SLUResult("nx053_上网流量办理","stateNode",1.0,slot1);
//            test1.add(a);
//            //System.out.println(dialogManager.feedUserInput("1", test1, "", "", "1046"));
//
//            List<SLUResult> test2 = new ArrayList<SLUResult>();
//            Map<String,String> slot2 = new HashMap<String, String>();
//            //slot2.put("password","123456");
//            SLUResult b = new SLUResult("重听","command",1.0,slot2);
//            test2.add(b);
//            //System.out.println(dialogManager.feedUserInput("1", test2, "", "","1046"));
//
//            List<SLUResult> test3 = new ArrayList<SLUResult>();
//            Map<String,String> slot3 = new HashMap<String, String>();
//            SLUResult  c= new SLUResult("nx001_话费查询","stateNode",1.0,slot3);
//            test3.add(c);
//            //System.out.println(dialogManager.feedUserInput("1", test3, "", "", "7005"));
//
//            List<SLUResult> test4 = new ArrayList<SLUResult>();
//            Map<String,String> slot4 = new HashMap<String, String>();
//            c= new SLUResult("查询天气","stateNode",1.0,slot4);
//            test4.add(c);
//            System.out.println(dialogManager.feedUserInput("1", test4, "", "", "7005",new HashMap<>()));
//
//            List<SLUResult> test5 = new ArrayList<SLUResult>();
//            Map<String,String> slot5 = new HashMap<String, String>();
//            slot5.put("month","2018.4.24");
//            c= new SLUResult("##","stateNode",1.0,slot5);
//            test5.add(c);
//            System.out.println(dialogManager.feedUserInput("1", test5, "", "","7005",new HashMap<>()));
//        }catch (Exception e){
//            e.printStackTrace();
//        }

    }

}

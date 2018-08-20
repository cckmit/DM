package entities;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储SLU结果的简单类型，可使用List<SLUResult>表达多个候选
 */
public class SLUResult {

    public  String ruleName;

    public String ruleType;    // stateID或者是command

    public String stateId = "";

    public String command = "";

    public String algorithmType = "";    //neuralNetwork或者match

    public String matchedAction = "";

    public String matchedTarget = "";


    // slots用于存储语义槽的名称-值,主要包含，event、userSelect、和相关的语义槽
    public  Map<String, String> slots = new HashMap<String, String>();
    public  double score;

    public SLUResult(){

    }


    @Override
    public String toString() {
        return "StateName: " + ruleName + ", Slots: " + slots;
    }

    public SLUResult(String stateId,
                     double score,
                     Map<String, String> slots
                     ) {
        this.stateId = stateId != null ? stateId : "";
        this.slots = slots != null ? slots : new HashMap<String, String>();
        this.score = score;

    }


//    public SLUResult(String ruleName,
//                     String ruleType,
//                     double score,
//                     Map<String, String> slots
//    ) {
//        this.ruleName = ruleName != null ? ruleName : "";
//        this.ruleType = ruleType != null ? ruleType : "";
//        if (this.ruleType.equals("stateNode"))
//            stateId = ruleName;
//        else if(this.ruleType.equals("command"))
//            command = ruleName;
//        this.slots = slots != null ? slots : new HashMap<String, String>();
//        this.score = score;
//
//    }

    public SLUResult(String algorithmType,
                     String ruleName,
                     String ruleType,
                     String action,
                     String target,
                     double score,
                     Map<String, String> slots
    ) {
        this.algorithmType = algorithmType != null ? algorithmType : "";
        this.ruleName = ruleName != null ? ruleName : "";
        this.ruleType = ruleType != null ? ruleType : "";
        if (this.algorithmType.equals("match")){
            if (this.ruleType.equals("stateNode"))
                stateId = ruleName;
            else if(this.ruleType.equals("command"))
                command = ruleName;
        }else if (this.algorithmType.equals("neuralNetwork")){
            if (this.ruleType.equals("命令"))
                command = target;
            else if(this.ruleType.equals("业务"))
            {
                if (action.equals("空"))
                    matchedAction = "";
                else matchedAction = action;
                if (target.equals("空"))
                    matchedTarget = "";
                else matchedTarget = target;
            }
        }else{
            if (ruleName.equals("##") && this.algorithmType.isEmpty()){
                this.stateId = ruleName;
            }
        }

        this.slots = slots != null ? slots : new HashMap<String, String>();
        this.score = score;

    }


    @Override
    public int hashCode(){
        // hash code based on type, id, start and end offsets (which should never
        // change once the annotation has been created).
        int hashCodeRes = 17;
        hashCodeRes = 31*hashCodeRes
                + ((ruleName == null) ? 0 : ruleName.hashCode());
        hashCodeRes = 31*hashCodeRes
                + ((ruleType == null) ? 0 : ruleType.hashCode());
        for (String key:slots.keySet()){
            hashCodeRes = 31*hashCodeRes
                    + ((key == null) ? 0 : key.hashCode());
            String value = slots.get(key);
            hashCodeRes = 31*hashCodeRes
                    + ((value == null) ? 0 : value.hashCode());
        }

        return  hashCodeRes;
    }// hashCode

    /** Returns true if two annotation are Equals.
     *  Two Annotation are equals if their offsets, types, id and features are the
     *  same.
     */
    @Override
    public boolean equals(Object obj){
        if(obj == null)
            return false;
        SLUResult other;
        if(obj instanceof SLUResult){
            other = (SLUResult) obj;
        }else return false;

        // If their types are not equals then return false
        if((ruleName == null) ^ (other.ruleName == null))
            return false;
        if((ruleType == null) ^ (other.ruleType == null))
            return false;
        if(score != other.score)
            return false;

        // If their featureMaps are not equals then return false
        if((slots == null) ^ (other.slots == null))
            return false;
        if(slots != null && (!slots.equals(other.slots)))
            return false;
        return true;
    }// equals



}

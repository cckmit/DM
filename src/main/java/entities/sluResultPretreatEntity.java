package entities;

import java.util.ArrayList;
import java.util.List;

public class sluResultPretreatEntity {

    List<SLUResult> stateIDInput = new ArrayList<>();
    List<SLUResult> commandInput = new ArrayList<>();

    public List<SLUResult> getStateIDInput() {
        return stateIDInput;
    }

    public void setStateIDInput(List<SLUResult> stateIDInput) {
        this.stateIDInput = stateIDInput;
    }

    public List<SLUResult> getCommandInput() {
        return commandInput;
    }

    public void setCommandInput(List<SLUResult> commandInput) {
        this.commandInput = commandInput;
    }
}

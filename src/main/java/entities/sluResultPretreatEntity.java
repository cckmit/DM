package entities;

import java.util.ArrayList;
import java.util.List;

public class sluResultPretreatEntity {

    List<SLUResult> stateIDInput = new ArrayList<>();
    List<SLUResult> commandInput = new ArrayList<>();

    public List<entities.SLUResult> getStateIDInput() {
        return stateIDInput;
    }

    public void setStateIDInput(List<entities.SLUResult> stateIDInput) {
        this.stateIDInput = stateIDInput;
    }

    public List<entities.SLUResult> getCommandInput() {
        return commandInput;
    }

    public void setCommandInput(List<entities.SLUResult> commandInput) {
        this.commandInput = commandInput;
    }
}

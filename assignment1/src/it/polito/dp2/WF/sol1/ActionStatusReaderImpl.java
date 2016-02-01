package it.polito.dp2.WF.sol1;

import it.polito.dp2.WF.ActionStatusReader;
import it.polito.dp2.WF.Actor;

import java.util.Calendar;

public class ActionStatusReaderImpl implements ActionStatusReader {

    private final String actionName;
    private final Actor actor;
    private final Calendar terminationTime;

    public ActionStatusReaderImpl(String actionName, Actor actor, Calendar terminationTime) {
        this.actionName = actionName;
        this.actor = actor;
        this.terminationTime = terminationTime;
    }

    @Override
    public String getActionName() {
        return this.actionName;
    }

    @Override
    public boolean isTakenInCharge() {
        if (this.actor != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isTerminated() {
        if (this.terminationTime != null) {
            return true;
        }
        return false;
    }

    @Override
    public Actor getActor() {
        return this.actor;
    }

    @Override
    public Calendar getTerminationTime() {
        return this.terminationTime;
    }
}

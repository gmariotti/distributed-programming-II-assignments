package it.polito.dp2.WF.sol2;

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
        return this.actor != null ? true : false;
    }

    @Override
    public boolean isTerminated() {
        return this.terminationTime != null ? true : false;
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

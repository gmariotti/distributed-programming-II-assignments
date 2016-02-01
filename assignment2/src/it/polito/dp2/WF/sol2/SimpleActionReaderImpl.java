package it.polito.dp2.WF.sol2;

import it.polito.dp2.WF.ActionReader;
import it.polito.dp2.WF.SimpleActionReader;
import it.polito.dp2.WF.WorkflowReader;

import java.util.LinkedHashSet;
import java.util.Set;

public class SimpleActionReaderImpl implements SimpleActionReader {
    private final String name;
    private final String role;
    private final boolean autIns;
    private final WorkflowReader workflow;
    private Set<ActionReader> possibleNextActions;

    public SimpleActionReaderImpl(String name, String role, boolean autIns, WorkflowReader workflow) {
        this.name = name;
        this.role = role;
        this.autIns = autIns;
        this.workflow = workflow;
        possibleNextActions = new LinkedHashSet<>();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public WorkflowReader getEnclosingWorkflow() {
        return this.workflow;
    }

    @Override
    public String getRole() {
        return this.role;
    }

    @Override
    public boolean isAutomaticallyInstantiated() {
        return this.autIns;
    }

    @Override
    public Set<ActionReader> getPossibleNextActions() {
        return this.possibleNextActions;
    }
}

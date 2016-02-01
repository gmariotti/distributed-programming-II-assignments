package it.polito.dp2.WF.sol3;

import it.polito.dp2.WF.ActionReader;
import it.polito.dp2.WF.SimpleActionReader;
import it.polito.dp2.WF.WorkflowReader;

import java.util.HashSet;
import java.util.Set;

public class SimpleActionReaderImpl implements SimpleActionReader {
    private final String name;
    private final String role;
    private final boolean automaticallyInstantiated;
    private final WorkflowReader currentWorkflow;
    private Set<ActionReader> possibleNextActions;

    public SimpleActionReaderImpl(String name, String role, boolean automaticallyInstantiated,
                                  WorkflowReader currentWorkflow) {
        this.name = name;
        this.role = role;
        this.automaticallyInstantiated = automaticallyInstantiated;
        this.currentWorkflow = currentWorkflow;
        this.possibleNextActions = new HashSet<>();
    }

    @Override
    public Set<ActionReader> getPossibleNextActions() {
        return this.possibleNextActions;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public WorkflowReader getEnclosingWorkflow() {
        return this.currentWorkflow;
    }

    @Override
    public String getRole() {
        return this.role;
    }

    @Override
    public boolean isAutomaticallyInstantiated() {
        return this.automaticallyInstantiated;
    }
}

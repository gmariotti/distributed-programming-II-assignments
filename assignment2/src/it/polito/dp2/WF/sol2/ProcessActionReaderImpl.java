package it.polito.dp2.WF.sol2;

import it.polito.dp2.WF.ProcessActionReader;
import it.polito.dp2.WF.WorkflowReader;

public class ProcessActionReaderImpl implements ProcessActionReader {
    private final String name;
    private final String role;
    private final boolean autIns;
    private final WorkflowReader enclosingWorkflow;
    private final WorkflowReader actionWorkflow;

    public ProcessActionReaderImpl(String name, String role, boolean autIns,
                                   WorkflowReader enclosingWorkflow, WorkflowReader actionWorkflow) {
        this.name = name;
        this.role = role;
        this.autIns = autIns;
        this.enclosingWorkflow = enclosingWorkflow;
        this.actionWorkflow = actionWorkflow;
    }

    @Override
    public WorkflowReader getActionWorkflow() {
        return this.actionWorkflow;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public WorkflowReader getEnclosingWorkflow() {
        return this.enclosingWorkflow;
    }

    @Override
    public String getRole() {
        return this.role;
    }

    @Override
    public boolean isAutomaticallyInstantiated() {
        return this.autIns;
    }
}

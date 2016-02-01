package it.polito.dp2.WF.sol1;

import it.polito.dp2.WF.ProcessActionReader;
import it.polito.dp2.WF.WorkflowReader;

public class ProcessActionReaderImpl implements ProcessActionReader {
    private final String name;
    private final String role;
    private final boolean autIns;
    private final WorkflowReader workflowReader;
    private final WorkflowReader actionWorkflow;

    public ProcessActionReaderImpl(String name, String role, boolean autIns, WorkflowReader workflowReader, WorkflowReader actionWorkflow) {
        this.name = name;
        this.role = role;
        this.autIns = autIns;
        this.workflowReader = workflowReader;
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
        return this.workflowReader;
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

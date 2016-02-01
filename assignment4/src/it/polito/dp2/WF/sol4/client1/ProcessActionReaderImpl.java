package it.polito.dp2.WF.sol4.client1;

import it.polito.dp2.WF.ProcessActionReader;
import it.polito.dp2.WF.WorkflowReader;

public class ProcessActionReaderImpl implements ProcessActionReader {
    private final String name;
    private final String role;
    private final boolean automaticallyInstantiated;
    private final WorkflowReader currentWorkflow;
    private final WorkflowReader nextWorkflow;

    public ProcessActionReaderImpl(String name, String role, boolean automaticallyInstantiated,
                                   WorkflowReader currentWorkflow, WorkflowReader nextWorkflow) {
        this.name = name;
        this.role = role;
        this.automaticallyInstantiated = automaticallyInstantiated;
        this.currentWorkflow = currentWorkflow;
        this.nextWorkflow = nextWorkflow;
    }

    @Override
    public WorkflowReader getActionWorkflow() {
        return this.nextWorkflow;
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

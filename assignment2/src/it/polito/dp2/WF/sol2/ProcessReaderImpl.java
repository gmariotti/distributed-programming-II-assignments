package it.polito.dp2.WF.sol2;

import it.polito.dp2.WF.ActionStatusReader;
import it.polito.dp2.WF.ProcessReader;
import it.polito.dp2.WF.WorkflowReader;

import java.util.Calendar;
import java.util.List;

public class ProcessReaderImpl implements ProcessReader {
    private final Calendar startTime;
    private final WorkflowReader workflow;
    private List<ActionStatusReader> status;

    public ProcessReaderImpl(Calendar startTime, List<ActionStatusReader> status, WorkflowReader workflow) {
        this.startTime = startTime;
        this.workflow = workflow;
        this.status = status;
    }

    @Override
    public Calendar getStartTime() {
        return this.startTime;
    }

    @Override
    public WorkflowReader getWorkflow() {
        return this.workflow;
    }

    @Override
    public List<ActionStatusReader> getStatus() {
        return this.status;
    }
}

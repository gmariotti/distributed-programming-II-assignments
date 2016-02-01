package it.polito.dp2.WF.sol4.server;

import it.polito.dp2.WF.ActionStatusReader;
import it.polito.dp2.WF.ProcessReader;
import it.polito.dp2.WF.WorkflowReader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class ProcessReaderImpl implements ProcessReader {
	private final Calendar startTime;
	private final WorkflowReader workflow;
	private List<ActionStatusReader> status;
    private final UUID id;

    public ProcessReaderImpl(UUID id, Calendar startTime,
			List<ActionStatusReader> status, WorkflowReader workflow) {
        this.id = id;
        this.startTime = startTime;
		this.workflow = workflow;
		this.status = status;
	}

    public ProcessReaderImpl(UUID id, ProcessReader processReader, WorkflowReader workflowReader) {
        this.id = id;
        this.startTime = processReader.getStartTime();
        this.workflow = workflowReader;
        this.status = new ArrayList<>();
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

    public UUID getID() {
        return this.id;
    }
}

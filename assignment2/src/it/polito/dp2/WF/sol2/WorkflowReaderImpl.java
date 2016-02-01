package it.polito.dp2.WF.sol2;

import it.polito.dp2.WF.ActionReader;
import it.polito.dp2.WF.ProcessReader;
import it.polito.dp2.WF.WorkflowReader;

import java.util.LinkedHashSet;
import java.util.Set;

public class WorkflowReaderImpl implements WorkflowReader {
    private final String name;
    private Set<ActionReader> actionReaders;
    private Set<ProcessReader> processReaders;

    public WorkflowReaderImpl(String name) {
        this.name = name;
        this.actionReaders = new LinkedHashSet<>();
        this.processReaders = new LinkedHashSet<>();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<ActionReader> getActions() {
        return this.actionReaders;
    }

    @Override
    public Set<ProcessReader> getProcesses() {
        return this.processReaders;
    }

    @Override
    public ActionReader getAction(String s) {
        for (ActionReader actionReader : this.actionReaders) {
            if (actionReader.getName().equals(s)) {
                return actionReader;
            }
        }
        return null;
    }
}

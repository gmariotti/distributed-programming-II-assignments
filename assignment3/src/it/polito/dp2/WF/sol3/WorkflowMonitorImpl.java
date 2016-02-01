package it.polito.dp2.WF.sol3;

import it.polito.dp2.WF.*;
import it.polito.dp2.WF.lab3.Refreshable;
import it.polito.dp2.WF.lab3.gen.Action;
import it.polito.dp2.WF.lab3.gen.UnknownNames_Exception;
import it.polito.dp2.WF.lab3.gen.Workflow;
import it.polito.dp2.WF.lab3.gen.WorkflowInfo;

import javax.xml.ws.Holder;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkflowMonitorImpl implements WorkflowMonitor, Refreshable {

    private WorkflowInfo port;
    private Calendar lastModTimeWorkflowNames;
    private Calendar lastModTimeWorkflowList;
    private List<String> workflowNames;
    private Set<WorkflowReader> workflowReaders;
    private Set<ProcessReader> processReaders;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z");

    public WorkflowMonitorImpl(WorkflowInfo port) throws WorkflowMonitorException {
        this.port = port;
        // empty because of Assignment3.pdf
        this.processReaders = new LinkedHashSet<>();

        // is used LinkedHashSet to mantain the order in which the elements are added in the list
        this.workflowReaders = new LinkedHashSet<>();
        this.lastModTimeWorkflowNames = null;
        this.lastModTimeWorkflowList = null;
        this.workflowNames = new LinkedList<>();

        contactServer(port);
    }

    private void contactServer(WorkflowInfo port) throws WorkflowMonitorException {
        Holder<List<String>> holderWorkflowNames = new Holder<>();
        holderWorkflowNames.value = new ArrayList<>();
        Holder<Calendar> holderLastModTime = new Holder<>();
        holderLastModTime.value = null;

        // get the list of Workflow in the server
        port.getWorkflowNames(holderLastModTime, holderWorkflowNames);
        if (holderLastModTime.value != null) {
            System.out.println("Last modification of WorkflowNames is "
                    + simpleDateFormat.format(holderLastModTime.value.getTime()));
        } else {
            throw new WorkflowMonitorException("Last modification time for workflow names is null");
        }
        if (holderWorkflowNames.value == null) {
            throw new WorkflowMonitorException("Workflow names holder is null");
        }

        // lastModTimeWorkflowNames is null only if the server is contacted for the first time.
        // if the timestamp received from the server is not after lastModTimeWorkflowNames, it means that the list of
        // Workflow has not been changed, so no need to process again the data.
        if (this.lastModTimeWorkflowNames == null || holderLastModTime.value.after(this.lastModTimeWorkflowNames)) {
            this.workflowNames = holderWorkflowNames.value;
            this.lastModTimeWorkflowNames = holderLastModTime.value;
        }

        // get the Workflow from the names
        holderLastModTime.value = null;
        Holder<List<Workflow>> holderWorkflowList = new Holder<>();
        holderWorkflowList.value = new ArrayList<>();
        try {
            port.getWorkflows(this.workflowNames, holderLastModTime, holderWorkflowList);
        } catch (UnknownNames_Exception exception) {
            throw new WorkflowMonitorException("UnknownNames_Exception in WorkflowMonitorImpl");
        }

        List<Workflow> workflowList;
        if (holderLastModTime.value != null) {
            System.out.println("Last modification of WorkflowList is "
                    + simpleDateFormat.format(holderLastModTime.value.getTime()));
        } else {
            throw new WorkflowMonitorException("Last modification time for workflow list is null");
        }
        if (holderWorkflowList.value == null) {
            throw new WorkflowMonitorException("Workflow list holder is null");
        }

        // lastModTimeWorkflowList is null only if the server is contacted for the first time.
        // if the timestamp received from the server is not after lastModTimeWorkflowList, it means that the list of
        // Workflow has not been changed, so no need to process again the data.
        if (this.lastModTimeWorkflowList == null || holderLastModTime.value.after(this.lastModTimeWorkflowList)) {
            workflowList = holderWorkflowList.value;
            this.lastModTimeWorkflowList = holderLastModTime.value;

            generateWorkflowSet(workflowList);
        }
    }

    private void generateWorkflowSet(List<Workflow> workflowList) throws WorkflowMonitorException {
        // create a Map -> WorkflowName-Workflow
        Map<String, WorkflowReader> workflowMap = new LinkedHashMap<>();
        for (Workflow workflow : workflowList) {
            WorkflowReader workflowReader = new WorkflowReaderImpl(workflow.getName());
            if (workflowMap.containsKey(workflow.getName())) {
                throw new WorkflowMonitorException("Workflow already present with name " + workflow.getName());
            }
            workflowMap.put(workflow.getName(), workflowReader);
        }

        for (Workflow workflow : workflowList) {
            initializeActions(workflow, workflowMap);
        }

        Set<String> keySet = workflowMap.keySet();
        this.workflowReaders = new LinkedHashSet<>();
        for (String key : keySet) {
            WorkflowReader workflow = workflowMap.get(key);
            this.workflowReaders.add(workflow);
        }
    }

    private void initializeActions(Workflow workflow, Map<String, WorkflowReader> workflowMap) throws WorkflowMonitorException {
        WorkflowReader currentWorkflow = workflowMap.get(workflow.getName());

        // generate the list of Actions in the Workflow
        List<Action> actions = workflow.getAction();
        if (actions != null) {
            Map<String, ActionReader> actionMap = new LinkedHashMap<>();

            for (Action action : actions) {
                // security check but should not happen because document has been validated
                if (actionMap.containsKey(action.getName())) {
                    throw new WorkflowMonitorException("Action already present with name " + action.getName());
                }

                List<String> nextActionList = action.getNextAction();
                if (nextActionList != null) {
                    ActionReader actionReader = new SimpleActionReaderImpl(
                            action.getName(),
                            action.getRole(),
                            action.isAutomaticallyInstantiated(),
                            currentWorkflow
                    );

                    actionMap.put(action.getName(), actionReader);
                } else {
                    // it must be a process because the document is validated server side
                    String nextWorkflow = action.getWorkflow();

                    // should never happen because document has been validated
                    if (nextWorkflow == null) {
                        throw new WorkflowMonitorException("ProcessAction with no Workflow to execute");
                    }

                    ActionReader actionReader = new ProcessActionReaderImpl(
                            action.getName(),
                            action.getRole(),
                            action.isAutomaticallyInstantiated(),
                            currentWorkflow,
                            workflowMap.get(nextWorkflow)
                    );
                    actionMap.put(action.getName(), actionReader);
                }
            }

            // set the list of next actions for each Action with a Simple element
            for (Action action : actions) {
                List<String> nextActionList = action.getNextAction();
                if (nextActionList != null) {
                    Set<ActionReader> nextActionSet = ((SimpleActionReader) actionMap.get(action.getName()))
                            .getPossibleNextActions();
                    for (String nextAction : nextActionList) {
                        // should never happen because document has been validated
                        if (!actionMap.containsKey(nextAction)) {
                            throw new WorkflowMonitorException("NextAction not present with name " + nextAction);
                        }

                        ActionReader next = actionMap.get(nextAction);
                        nextActionSet.add(next);
                    }
                }
            }

            // Create the set of ActionReader of the Workflow and set it
            Set<ActionReader> actionReaders = currentWorkflow.getActions();
            Set<String> keySet = actionMap.keySet();
            for (String key : keySet) {
                ActionReader actionReader = actionMap.get(key);
                actionReaders.add(actionReader);
            }
        }
    }

    @Override
    public Set<WorkflowReader> getWorkflows() {
        return this.workflowReaders;
    }

    @Override
    public WorkflowReader getWorkflow(String s) {
        for (WorkflowReader workflowReader : this.workflowReaders) {
            if (workflowReader.getName().equals(s)) {
                return workflowReader;
            }
        }
        return null;
    }

    @Override
    public Set<ProcessReader> getProcesses() {
        return this.processReaders;
    }

    @Override
    public void refresh() {
        try {
            contactServer(this.port);
        } catch (WorkflowMonitorException e) {
            System.err.println("WorkflowMonitorException in refresh");
            System.exit(1);
        }
    }
}

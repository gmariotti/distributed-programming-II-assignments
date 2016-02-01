package it.polito.dp2.WF.sol4.client1;

import it.polito.dp2.WF.*;
import it.polito.dp2.WF.sol4.client1.wsdl.*;

import javax.xml.ws.Holder;
import java.util.*;

public class WorkflowMonitorImpl implements WorkflowMonitor {

    private WorkflowServicePortType port;
    private Calendar lastModTimeWorkflowList;
    private Set<WorkflowReader> workflowReaders;
    private Set<ProcessReader> processReaders;

    public WorkflowMonitorImpl(WorkflowServicePortType port) throws WorkflowMonitorException {
        this.port = port;

        this.processReaders = new LinkedHashSet<>();
        this.workflowReaders = new LinkedHashSet<>();
        this.lastModTimeWorkflowList = null;

        contactServer(port);
    }

    private void contactServer(WorkflowServicePortType port) throws WorkflowMonitorException {
        Holder<WorkflowInfoResponse.Workflows> holderWorkflowList = new Holder<>();
        holderWorkflowList.value = null;
        Holder<Calendar> holderLastModTime = new Holder<>();
        holderLastModTime.value = null;

        try {
            port.workflowInfo(holderWorkflowList, holderLastModTime);
        } catch (FaultException e) {
            throw new WorkflowMonitorException("Error in contacting server " + e.getFaultInfo().getFaultMessage());
        }

        List<Workflow> workflowList = holderWorkflowList.value.getWorkflow();

        generateWorkflowSet(workflowList);
    }

    private void generateWorkflowSet(List<Workflow> workflowList) {
        // create a Map -> WorkflowName-Workflow
        Map<String, WorkflowReader> workflowMap = new LinkedHashMap<>();
        for (Workflow workflow : workflowList) {
            WorkflowReader workflowReader = new WorkflowReaderImpl(workflow.getName());
            workflowMap.put(workflow.getName(), workflowReader);
        }

        for (Workflow workflow : workflowList) {
            initializeActions(workflow, workflowMap);
            initializeProcesses(workflow, workflowMap);
        }

        Set<String> keySet = workflowMap.keySet();
        this.workflowReaders = new HashSet<>();
        for (String key : keySet) {
            WorkflowReader workflow = workflowMap.get(key);
            this.workflowReaders.add(workflow);
            Set<ProcessReader> processes = workflow.getProcesses();
            for (ProcessReader process : processes) {
                this.processReaders.add(process);
            }
        }
    }

    private void initializeActions(Workflow workflow, Map<String, WorkflowReader> workflowMap) {
        WorkflowReader currentWorkflow = workflowMap.get(workflow.getName());

        // generate the list of Actions in the Workflow
        List<Action> actions = workflow.getAction();
        if (actions == null) {
            return;
        }

        Map<String, ActionReader> actionMap = new LinkedHashMap<>();
        for (Action action : actions) {
            SimpleAction simpleAction = action.getSimple();
            if (simpleAction != null) {
                ActionReader actionReader = new SimpleActionReaderImpl(
                        action.getName(),
                        action.getRole(),
                        action.isAutIns(),
                        currentWorkflow
                );
                actionMap.put(action.getName(), actionReader);
            } else {
                // it must be a process because the document is validated
                ProcessAction processAction = action.getProcess();
                String nextWorkflow = processAction.getNextWorkflow();
                ActionReader actionReader = new ProcessActionReaderImpl(
                        action.getName(),
                        action.getRole(),
                        action.isAutIns(),
                        currentWorkflow,
                        workflowMap.get(nextWorkflow)
                );
                actionMap.put(action.getName(), actionReader);
            }
        }

        // set the list of next actions for each Action with a Simple element
        for (Action action : actions) {
            SimpleAction simpleAction = action.getSimple();
            if (simpleAction != null) {
                List<SimpleAction.NextAction> nextActionList = simpleAction.getNextAction();
                Set<ActionReader> nextActionSet = ((SimpleActionReader) actionMap.get(action.getName()))
                        .getPossibleNextActions();
                for (SimpleAction.NextAction nextAction : nextActionList) {
                    ActionReader next = actionMap.get(nextAction.getNext());
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

    private void initializeProcesses(Workflow workflow, Map<String, WorkflowReader> workflowMap) {
        WorkflowReader currentWorkflow = workflowMap.get(workflow.getName());

        // generates the list of processes for the current workflow
        Set<ProcessReader> processReaders = currentWorkflow.getProcesses();
        List<ProcessEl> processList = workflow.getProcess();
        for (ProcessEl process : processList) {
            Calendar startTime = process.getStartTime();

            // generates the list of Status of the Process
            List<ActionStatusReader> actionStatusList = new ArrayList<>();
            List<Status> statusList = process.getStatus();
            for (Status status : statusList) {
                String actionName = status.getActionName();
                String actorName = status.getActor();
                Actor actor = null;
                Calendar endTime = null;
                if (actorName != null) {
                    actor = new Actor(actorName, currentWorkflow.getAction(actionName).getRole());
                    endTime = status.getEndTime();
                }
                ActionStatusReader actionStatusReader = new ActionStatusReaderImpl(
                        actionName,
                        actor,
                        endTime
                );
                actionStatusList.add(actionStatusReader);
            }

            // initializes the Process and adds it to the workflow's set
            ProcessReader processReader = new ProcessReaderImpl(
                    startTime,
                    actionStatusList,
                    currentWorkflow
            );
            processReaders.add(processReader);
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

}

package it.polito.dp2.WF.sol1;

import it.polito.dp2.WF.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkflowMonitorImpl implements WorkflowMonitor {
    private Set<WorkflowReader> workflowReaders;
    private Set<ProcessReader> processReaders;
    private DateFormat dateFormat;

    public WorkflowMonitorImpl(String filename) throws WorkflowMonitorException {
        workflowReaders = new LinkedHashSet<>();
        processReaders = new LinkedHashSet<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm'z'z");

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // used to validate the document
        factory.setValidating(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);

        final DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            final OutputStreamWriter errorWrite = new OutputStreamWriter(System.err, "UTF-8");
            builder.setErrorHandler(new SAXErrorHandler(new PrintWriter(errorWrite, true)));
        } catch (ParserConfigurationException | UnsupportedEncodingException e) {
            throw new WorkflowMonitorException("Error in getting the builder");
        }

        // it parse doesn't generate any error, than the document is valid
        final Document document;
        try {
            document = builder.parse(new File(filename));
        } catch (SAXException | IOException e) {
            throw new WorkflowMonitorException("Error in parsing " + filename);
        }
        final Element rootElement = document.getDocumentElement();

        // generation of workflow list
        // document validated, there will always be just one <workflows> tag
        final Node workflows = rootElement.getElementsByTagName("workflows").item(0);
        final NodeList workflowList = workflows.getChildNodes();

        // generates a map with all the workflow in the xml document
        // checks the uniqueness of workflow name
        Map<String, WorkflowReader> workflowMap = new LinkedHashMap<>();
        for (int i = 0; i < workflowList.getLength(); i++) {
            Element workflowElement = (Element) workflowList.item(i);
            String name = workflowElement.getAttribute("name");
            WorkflowReader workflowReader = new WorkflowReaderImpl(name);
            if (workflowMap.get(name) == null) {
                workflowMap.put(name, workflowReader);
            } else {
                throw new WorkflowMonitorException("Workflow already present with name " + name);
            }
        }

        for (int i = 0; i < workflowList.getLength(); i++) {
            Element workflowElement = (Element) workflowList.item(i);
            NodeList actionsList = workflowElement.getElementsByTagName("action");
            createWorkflowActions(workflowMap.get(workflowElement.getAttribute("name")), actionsList, workflowMap);
        }

        // generation of process list
        // document validated, there will always be just one <processes> tag
        final Node processes = rootElement.getElementsByTagName("processes").item(0);
        final NodeList processList = processes.getChildNodes();
        for (int i = 0; i < processList.getLength(); i++) {
            Element processElement = (Element) processList.item(i);
            String workflowName = processElement.getAttribute("workflow");
            WorkflowReader processWorkflow = workflowMap.get(workflowName);
            if (processWorkflow == null) {
                throw new WorkflowMonitorException("Process workflow doesn't exist with name " + workflowName);
            }

            // get start time of the process
            Element startTimeElement = (Element) processElement.getElementsByTagName("starttime").item(0);
            Date date;
            try {
                date = this.dateFormat.parse(startTimeElement.getTextContent());
            } catch (ParseException e) {
                throw new WorkflowMonitorException("Inserted start time is not valid");
            }
            Calendar startTime = GregorianCalendar.getInstance();
            startTime.setTime(date);

            ProcessReader processReader = new ProcessReaderImpl(processWorkflow, startTime);

            createProcessStatusAction(processReader, processElement.getElementsByTagName("status"));

            this.processReaders.add(processReader);
            processWorkflow.getProcesses().add(processReader);
        }

        Set<String> keySet = workflowMap.keySet();
        for (String key : keySet) {
            this.workflowReaders.add(workflowMap.get(key));
        }
    }

    private void createWorkflowActions(WorkflowReader workflow, NodeList actionsList, Map<String, WorkflowReader> workflowMap)
            throws WorkflowMonitorException {
        // Map that associates a name with its action in the workflow
        Map<String, ActionReader> actionMap = new LinkedHashMap<>();
        for (int i = 0; i < actionsList.getLength(); i++) {
            Element actionElement = (Element) actionsList.item(i);
            String actionName = actionElement.getAttribute("name");
            String actionRole = actionElement.getAttribute("role");
            boolean actionAutIns = Boolean.getBoolean(actionElement.getAttribute("autins"));
            Element typeOfAction = (Element) actionElement.getChildNodes().item(0);

            // checks uniqueness of action name
            if (actionMap.get(actionName) != null) {
                throw new WorkflowMonitorException("Action already present with name " + actionName);
            }

            ActionReader actionReader;

            // Check if is a SimpleAction or a ProcessAction
            // Document is validated, so one of them must be present
            if (typeOfAction.getTagName().equals("simpleAc")) {
                actionReader = new SimpleActionReaderImpl(
                        actionName,
                        actionRole,
                        actionAutIns,
                        workflow
                );
            } else {
                // is a ProcessAction
                String workflowNext = typeOfAction.getAttribute("workflow");
                WorkflowReader actionWorkflow = workflowMap.get(workflowNext);
                if (actionWorkflow != null) {
                    actionReader = new ProcessActionReaderImpl(
                            actionName,
                            actionRole,
                            actionAutIns,
                            workflow,
                            actionWorkflow
                    );
                } else {
                    throw new WorkflowMonitorException("Workflow doesn't exist with name " + workflowNext);
                }
            }
            actionMap.put(actionName, actionReader);
        }

        // set the list of Action of each SimpleAction in the map
        // and at the same time creates the Set<Action> of the Workflow
        Set<ActionReader> workflowActionSet = workflow.getActions();
        for (int i = 0; i < actionsList.getLength(); i++) {
            Element actionElement = (Element) actionsList.item(i);
            String actionName = actionElement.getAttribute("name");
            Element typeOfAction = (Element) actionElement.getChildNodes().item(0);

            ActionReader actionReader = actionMap.get(actionName);
            if (actionReader instanceof SimpleActionReader) {
                Set<ActionReader> possibleNextAction = ((SimpleActionReader) actionReader).getPossibleNextActions();
                String nextActions = typeOfAction.getAttribute("next");
                String[] nextActionsArray = new String[0];
                if (!nextActions.isEmpty()) {
                    nextActionsArray = nextActions.split(" ");
                }
                for (String action : nextActionsArray) {
                    ActionReader nextAction = actionMap.get(action);
                    if (nextAction != null) {
                        possibleNextAction.add(nextAction);
                    } else {
                        throw new WorkflowMonitorException("Action not present in the document " + action);
                    }
                }
            }
            workflowActionSet.add(actionReader);
        }
    }

    private void createProcessStatusAction(ProcessReader process, NodeList statusChildren) throws WorkflowMonitorException {
        List<ActionStatusReader> statusList = process.getStatus();
        WorkflowReader processWorkflow = process.getWorkflow();

        for (int i = 0; i < statusChildren.getLength(); i++) {
            Element statusElement = (Element) statusChildren.item(i);
            String actionName = statusElement.getAttribute("action");
            ActionReader action = processWorkflow.getAction(actionName);
            if (action == null) {
                throw new WorkflowMonitorException("Action with name " + actionName + " doesn't " +
                        "exist in Workflow with name " + processWorkflow.getName());
            }

            // creation of the actor -> can be not present
            Actor actor = null;
            String actorName = statusElement.getAttribute("actor");
            if (!actorName.equals("")) {
                String actorRole = action.getRole();
                actor = new Actor(actorName, actorRole);
            }

            // creation of the termination time -> can be not present
            NodeList statusChild = statusElement.getChildNodes();
            Calendar termination = null;
            if (statusChild.getLength() > 0) {
                Element terminationTimeElement = (Element) statusChild.item(0);
                Date endTime;
                try {
                    endTime = dateFormat.parse(terminationTimeElement.getTextContent());
                } catch (ParseException e) {
                    throw new WorkflowMonitorException("Inserted end time is not valid");
                }
                termination = GregorianCalendar.getInstance();
                termination.setTime(endTime);

                // if start time happens after the termination time, then is an error
                if (process.getStartTime().after(termination)) {
                    throw new WorkflowMonitorException("Error, start time is after end time");
                }
            }

            if (actor == null && termination != null) {
                throw new WorkflowMonitorException("Actor is not present but there is a termination date");
            }
            ActionStatusReader statusReader = new ActionStatusReaderImpl(actionName, actor, termination);
            statusList.add(statusReader);
        }
    }

    @Override
    public Set<WorkflowReader> getWorkflows() {
        return workflowReaders;
    }

    @Override
    public WorkflowReader getWorkflow(String s) {
        for (WorkflowReader workflowReader : workflowReaders) {
            if (workflowReader.getName().equals(s)) {
                return workflowReader;
            }
        }
        return null;
    }

    @Override
    public Set<ProcessReader> getProcesses() {
        return processReaders;
    }
}

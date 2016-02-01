package it.polito.dp2.WF.sol4.server;

import it.polito.dp2.WF.*;
import it.polito.dp2.WF.sol4.server.wsdl.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// used to manage all the code regarding JAXB
public class JAXBBoss {
    private final String context = "it.polito.dp2.WF.sol4.server.wsdl";

    private ObjectFactory factory;
    private WorkflowInfoResponse.Workflows JAXBResult;
    SimpleDateFormat simpleDateFormatForFile = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss-z");

    public JAXBBoss() {
        factory = new ObjectFactory();
        JAXBResult = factory.createWorkflowInfoResponseWorkflows();
    }

    public WorkflowInfoResponse.Workflows getJAXBResult() {
        return this.JAXBResult;
    }

    public boolean checkValidity(Calendar timestamp, Logger logger) {
        WorkflowInfoResponse workflowInfoResponse = factory.createWorkflowInfoResponse();
        workflowInfoResponse.setWorkflows(JAXBResult);
        workflowInfoResponse.setTimestamp(timestamp);
        JAXBElement<WorkflowInfoResponse> workflowInfoResponseJAXBElement = factory.createWorkflowInfoResponse(workflowInfoResponse);

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            schema = sf.newSchema(new File("logs/tmp/schema.xsd"));
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "SAXException", e.getCause());
            return false;
        }

        String filename = "./logs/tmp/workflow" + simpleDateFormatForFile.format(Calendar.getInstance().getTime()) + ".xml";
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(context);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setSchema(schema);
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            marshaller.marshal(workflowInfoResponseJAXBElement, fileOutputStream);

            // delete temporary file used
            fileOutputStream.close();
            boolean result = Files.deleteIfExists(Paths.get(filename));
        } catch (JAXBException e) {
            logger.log(Level.SEVERE, "JAXBException", e.getCause());
            return false;
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "FileNotFoundException", e.getCause());
            return false;
        } catch (IOException e) {
            logger.log(Level.WARNING, "IOException " + e.getMessage(), e.getCause());
        }

        return true;
    }

    public void addWorkflow(WorkflowReader workflowReader) throws WorkflowMonitorException {
        Workflow workflow = initializeWorkflow(workflowReader);
        JAXBResult.getWorkflow().add(workflow);
    }

    private Workflow initializeWorkflow(WorkflowReader workflowReader) throws WorkflowMonitorException {
        Workflow workflow = factory.createWorkflow();
        workflow.setName(workflowReader.getName());

        // initialize the list of actions of the workflow
        List<Action> actionList = workflow.getAction();
        initializeActionList(actionList, workflowReader);

        // initialize the list of processes of the workflow
        List<ProcessEl> processList = workflow.getProcess();
        initializeProcessList(processList, workflowReader);

        return workflow;
    }

    private void initializeActionList(List<Action> actionList, WorkflowReader workflowReader) {
        Set<ActionReader> actionSet = workflowReader.getActions();
        for (ActionReader actionReader : actionSet) {
            Action action = factory.createAction();
            action.setName(actionReader.getName());
            action.setRole(actionReader.getRole());
            action.setAutIns(actionReader.isAutomaticallyInstantiated());

            // discriminate between a SimpleActionReader and a ProcessActionReader
            if (actionReader instanceof SimpleActionReader) {
                SimpleActionReader simpleActionReader = (SimpleActionReader) actionReader;
                Set<ActionReader> nextAction = simpleActionReader.getPossibleNextActions();
                SimpleAction simpleAction = factory.createSimpleAction();
                if (nextAction.size() > 0) {
                    List<SimpleAction.NextAction> nextActionList = simpleAction.getNextAction();
                    for (ActionReader nextActionReader : nextAction) {
                        SimpleAction.NextAction next = new ObjectFactory().createSimpleActionNextAction();
                        next.setNext(nextActionReader.getName());
                        nextActionList.add(next);
                    }
                }
                action.setSimple(simpleAction);
            } else if (actionReader instanceof ProcessActionReader) {
                ProcessActionReader processActionReader = (ProcessActionReader) actionReader;
                String workflowNext = processActionReader.getActionWorkflow().getName();
                ProcessAction processAction = factory.createProcessAction();
                processAction.setNextWorkflow(workflowNext);
                action.setProcess(processAction);
            }

            actionList.add(action);
        }
    }

    private void initializeProcessList(List<ProcessEl> processList, WorkflowReader workflowReader)
            throws WorkflowMonitorException {
        Set<ProcessReader> processReaders = workflowReader.getProcesses();
        for (ProcessReader processReader : processReaders) {
            ProcessEl process = factory.createProcessEl();

            process.setId(((ProcessReaderImpl) processReader).getID().toString());
            Calendar startTime = (Calendar) processReader.getStartTime().clone();
            process.setStartTime(startTime);

            // initialize the list of status of each process
            List<Status> statusList = process.getStatus();
            List<ActionStatusReader> statusReaders = processReader.getStatus();
            for (ActionStatusReader actionStatusReader : statusReaders) {
                Status status = new ObjectFactory().createStatus();
                // inside a Process, the only element that can change between different calls are the ActionStatus
                synchronized (actionStatusReader) {
                    status.setActionName(actionStatusReader.getActionName());
                    if (actionStatusReader.isTakenInCharge()) {
                        status.setActor(actionStatusReader.getActor().getName());
                        if (actionStatusReader.isTerminated()) {
                            Calendar endTime = actionStatusReader.getTerminationTime();
                            // used to avoid situation in which other methods modifies the endTime and also in the status
                            // object it will be modified
                            Calendar newEndTime = endTime != null ? (Calendar) endTime.clone() : null;
                            status.setEndTime(newEndTime);
                        }
                    }
                }
                statusList.add(status);
            }
            processList.add(process);
        }
    }
}

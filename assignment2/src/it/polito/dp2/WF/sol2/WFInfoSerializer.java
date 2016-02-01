package it.polito.dp2.WF.sol2;

import it.polito.dp2.WF.*;
import it.polito.dp2.WF.sol2.jaxb.*;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class WFInfoSerializer {
    private WorkflowMonitor monitor;
    private ObjectFactory objectFactory;
    private DateFormat dateFormat;

    public WFInfoSerializer() throws WorkflowMonitorException {
        it.polito.dp2.WF.WorkflowMonitorFactory factory = it.polito.dp2.WF.WorkflowMonitorFactory.newInstance();
        monitor = factory.newWorkflowMonitor();
        objectFactory = new ObjectFactory();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm'z'Z");
    }

    public WFInfoSerializer(WorkflowMonitor monitor) {
        this.monitor = monitor;
        objectFactory = new ObjectFactory();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm'z'Z");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Argument must be equal to the output file name");
            System.exit(1);
        }
        System.out.println("Output file is " + args[0]);

        WFInfoSerializer wfInfoSerializer;

        try {
            wfInfoSerializer = new WFInfoSerializer();
            wfInfoSerializer.generateXML(args[0], "it.polito.dp2.WF.sol2.jaxb");
        } catch (WorkflowMonitorException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateXML(String filePath, String context) throws WorkflowMonitorException {
        WFInfo wfInfo = objectFactory.createWFInfo();
        initializeWorkflowList(wfInfo.getWorkflow());

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try {
            schema = sf.newSchema(new File("./xsd/WFInfo.xsd"));
        } catch (SAXException e) {
            throw new WorkflowMonitorException("Error creating XML schema");
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(context);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "WFInfo.xsd");
            marshaller.setSchema(schema);
            marshaller.marshal(wfInfo, new FileOutputStream(filePath));
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initializeWorkflowList(List<Workflow> workflowList) throws WorkflowMonitorException {
        Set<WorkflowReader> workflowReaders = monitor.getWorkflows();
        for (WorkflowReader workflowReader : workflowReaders) {
            Workflow workflow = objectFactory.createWorkflow();
            workflow.setName(workflowReader.getName());

            // initialize the list of actions of the workflow
            List<Action> actionList = workflow.getAction();
            initializeActionList(actionList, workflowReader);

            // initialize the list of processes of the workflow
            List<ProcessEl> processList = workflow.getProcess();
            initializeProcessList(processList, workflowReader);

            workflowList.add(workflow);
        }
    }

    private void initializeActionList(List<Action> actionList, WorkflowReader workflowReader) {
        Set<ActionReader> actionSet = workflowReader.getActions();
        for (ActionReader actionReader : actionSet) {
            Action action = objectFactory.createAction();
            action.setName(actionReader.getName());
            action.setRole(actionReader.getRole());
            action.setAutIns(actionReader.isAutomaticallyInstantiated());

            // discriminate between a SimpleActionReader and a ProcessActionReader
            if (actionReader instanceof SimpleActionReader) {
                SimpleActionReader simpleActionReader = (SimpleActionReader) actionReader;
                Set<ActionReader> nextAction = simpleActionReader.getPossibleNextActions();
                SimpleAction simpleAction = objectFactory.createSimpleAction();
                if (nextAction.size() > 0) {
                    List<SimpleAction.NextAction> nextActionList = simpleAction.getNextAction();
                    for (ActionReader nextActionReader : nextAction) {
                        SimpleAction.NextAction next = objectFactory.createSimpleActionNextAction();
                        next.setNext(nextActionReader.getName());
                        nextActionList.add(next);
                    }
                }
                action.setSimple(simpleAction);
            } else if (actionReader instanceof ProcessActionReader) {
                ProcessActionReader processActionReader = (ProcessActionReader) actionReader;
                String workflowNext = processActionReader.getActionWorkflow().getName();
                ProcessAction processAction = objectFactory.createProcessAction();
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
            ProcessEl process = objectFactory.createProcessEl();

            Calendar startTime = processReader.getStartTime();
            process.setStartTime(startTime);

            // initialize the list of status of each process
            List<Status> statusList = process.getStatus();
            List<ActionStatusReader> statusReaders = processReader.getStatus();
            for (ActionStatusReader actionStatusReader : statusReaders) {
                Status status = objectFactory.createStatus();
                status.setActionName(actionStatusReader.getActionName());
                if (actionStatusReader.isTakenInCharge()) {
                    status.setActor(actionStatusReader.getActor().getName());
                    if (actionStatusReader.isTerminated()) {
                        Calendar endTime = actionStatusReader.getTerminationTime();
                        status.setEndTime(endTime);
                    }
                }
                statusList.add(status);
            }
            processList.add(process);
        }
    }
}

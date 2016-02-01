package it.polito.dp2.WF.sol1;

import it.polito.dp2.WF.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class WFInfoSerializer {
    private static String outputFilename;

    private WorkflowMonitor monitor;
    private DateFormat dateFormat;

    public WFInfoSerializer() throws WorkflowMonitorException {
        it.polito.dp2.WF.WorkflowMonitorFactory factory = it.polito.dp2.WF.WorkflowMonitorFactory.newInstance();
        monitor = factory.newWorkflowMonitor();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm'z'z");
    }

    public WFInfoSerializer(WorkflowMonitor monitor) {
        this.monitor = monitor;
        dateFormat = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm'z'z");
    }

    public static void main(String[] args) {
        WFInfoSerializer wfInfoSerializer;

        if (args.length != 1) {
            System.err.println("Name of the output file is missing");
            System.exit(1);
        }
        outputFilename = args[0];
        System.out.println("Filename: " + outputFilename);

        try {
            wfInfoSerializer = new WFInfoSerializer();

            System.out.println("Serialization starting");
            wfInfoSerializer.serialize();
            System.out.println("Serialization completed");

            wfInfoSerializer.parse();
            System.out.println("Parsing completed");
        } catch (WorkflowMonitorException e) {
            System.err.println("Could not instantiate data generator");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void serialize() throws WorkflowMonitorException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element information = document.createElement("information");

            // set the list of workflow
            Element workflows = setWorkflows(document);
            information.appendChild(workflows);
            System.out.println("Workflows set");

            // set the list of processes
            Element processes = setProcesses(document);
            information.appendChild(processes);
            System.out.println("Processes set");

            document.appendChild(information);

            printDocument(document, new FileOutputStream(outputFilename));

        } catch (ParserConfigurationException e) {
            throw new WorkflowMonitorException("ParserConfigurationException");
        } catch (TransformerException e) {
            throw new WorkflowMonitorException("TransformerException");
        } catch (IOException e) {
            throw new WorkflowMonitorException("IOException");
        }
    }

    private void parse() throws WorkflowMonitorException {
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

        // if parsing doesn't generate any error, than the document is valid
        final Document document;
        try {
            document = builder.parse(new File(outputFilename));
        } catch (SAXException | IOException e) {
            throw new WorkflowMonitorException("Error in parsing " + outputFilename);
        }
    }

    private Element setWorkflows(Document document)
            throws WorkflowMonitorException {
        Element workflowsElement = document.createElement("workflows");

        Set<WorkflowReader> workflowReaders = monitor.getWorkflows();
        for (WorkflowReader workflowReader : workflowReaders) {
            final Element workflowElement = document.createElement("workflow");
            workflowElement.setAttribute("name", workflowReader.getName());

            // get the list of actions in the workflow
            final List<Element> actions = setActions(document, workflowReader);

            for (Element actionElement : actions) {
                workflowElement.appendChild(actionElement);
            }
            workflowsElement.appendChild(workflowElement);
        }

        return workflowsElement;
    }

    private List<Element> setActions(Document document, WorkflowReader workflowReader)
            throws WorkflowMonitorException {
        List<Element> actionsElement = new LinkedList<>();

        Set<ActionReader> actionReaders = workflowReader.getActions();

        for (ActionReader actionReader : actionReaders) {
            Element actionElement = document.createElement("action");
            actionElement.setAttribute("name", actionReader.getName());
            actionElement.setAttribute("role", actionReader.getRole());
            actionElement.setAttribute("autins", Boolean.toString(actionReader.isAutomaticallyInstantiated()));

            // check if SimpleActionReader or ProcessActionReader
            if (actionReader instanceof SimpleActionReader) {
                Element simpleElement = document.createElement("simpleAc");

                SimpleActionReader simpleActionReader = (SimpleActionReader) actionReader;
                Set<ActionReader> simpleActionReaders = simpleActionReader.getPossibleNextActions();

                // check if the set is empty or not
                if (!simpleActionReaders.isEmpty()) {
                    String next = "";
                    Iterator<ActionReader> iterator = simpleActionReaders.iterator();
                    while (iterator.hasNext()) {
                        ActionReader reader = iterator.next();
                        next += reader.getName();
                        if (iterator.hasNext()) {
                            next += " ";
                        }
                    }

                    simpleElement.setAttribute("next", next);
                }

                actionElement.appendChild(simpleElement);
            } else if (actionReader instanceof ProcessActionReader) {
                Element processElement = document.createElement("processAc");

                ProcessActionReader processActionReader = (ProcessActionReader) actionReader;
                processElement.setAttribute("workflow", processActionReader.getActionWorkflow().getName());
                actionElement.appendChild(processElement);
            } else {
                // error
                throw new WorkflowMonitorException("ActionReader " + actionReader.getName() + " is neither Simple nor Process");
            }
            actionsElement.add(actionElement);
        }

        return actionsElement;
    }

    private Element setProcesses(Document document) throws WorkflowMonitorException {
        Element processesElement = document.createElement("processes");

        Set<ProcessReader> processReaders = monitor.getProcesses();
        for (ProcessReader processReader : processReaders) {
            Element processElement = document.createElement("process");
            processElement.setAttribute("workflow", processReader.getWorkflow().getName());

            GregorianCalendar calendar = (GregorianCalendar) processReader.getStartTime();
            Element startTime = initializeTimeElement(document, calendar, "starttime");
            processElement.appendChild(startTime);

            List<Element> statusElements = setStatus(document, processReader);
            for (Element statusElement : statusElements) {
                processElement.appendChild(statusElement);
            }

            processesElement.appendChild(processElement);
        }

        return processesElement;
    }

    private List<Element> setStatus(Document document, ProcessReader processReader)
            throws WorkflowMonitorException {
        List<Element> statusList = new LinkedList<>();
        WorkflowReader workflowReader = processReader.getWorkflow();

        List<ActionStatusReader> actionStatusReaders = processReader.getStatus();
        for (ActionStatusReader actionStatusReader : actionStatusReaders) {
            Element statusElement = document.createElement("status");
            statusElement.setAttribute("action", actionStatusReader.getActionName());

            // check that the Action exists in the Workflow
            ActionReader actionReader = workflowReader.getAction(actionStatusReader.getActionName());
            if (actionReader == null) {
                throw new WorkflowMonitorException("Action is not present into the workflow");
            }
            // if is taken in charge, it means that an actor is assigned to it
            if (actionStatusReader.isTakenInCharge()) {
                statusElement.setAttribute("actor", actionStatusReader.getActor().getName());

                // check that the Action role is equal to the role of the Actor
                if (!actionReader.getRole().equals(actionStatusReader.getActor().getRole())) {
                    throw new WorkflowMonitorException("Actior role is different from Actor role");
                }
                if (actionStatusReader.isTerminated()) {
                    GregorianCalendar calendar = (GregorianCalendar) actionStatusReader.getTerminationTime();
                    Element endTime = initializeTimeElement(document, calendar, "endtime");
                    statusElement.appendChild(endTime);
                }
            }
            statusList.add(statusElement);
        }

        return statusList;
    }

    private Element initializeTimeElement(Document document, GregorianCalendar event, String elementName) {
        Element element = document.createElement(elementName);

        Date date = event.getTime();
        element.setTextContent(dateFormat.format(date));

        return element;
    }

    private static void printDocument(Document document, OutputStream out)
            throws IOException, TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "./wfInfo.dtd");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(document), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }

}

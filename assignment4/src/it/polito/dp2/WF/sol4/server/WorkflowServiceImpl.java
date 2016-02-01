package it.polito.dp2.WF.sol4.server;

import it.polito.dp2.WF.*;
import it.polito.dp2.WF.sol4.server.wsdl.Fault;
import it.polito.dp2.WF.sol4.server.wsdl.Fault_Exception;
import it.polito.dp2.WF.sol4.server.wsdl.TakeAction.Actor;
import it.polito.dp2.WF.sol4.server.wsdl.WorkflowInfoResponse.Workflows;
import it.polito.dp2.WF.sol4.server.wsdl.WorkflowServicePortType;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.*;

@WebService(endpointInterface = "it.polito.dp2.WF.sol4.server.wsdl.WorkflowServicePortType")
public class WorkflowServiceImpl implements WorkflowServicePortType {
    private static final Logger LOGGER = Logger.getLogger("WorkflowServiceImpl");
    private static final String password = "secret_password_with_server";
    private static final String defaultFileName = "./logs/workflowServiceImpl";

    // once a Workflow has been created, will never modified except for its list of Process, that can be increased.
    private ConcurrentMap<String, WorkflowReader> workflowMap;
    private ConcurrentMap<UUID, ProcessReader> processMap;
    // used for creating a new UUID to insert in the ProcessMap and, at the same time, avoiding to lock it
    private final Object lockForID = new Object();

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");

    public WorkflowServiceImpl() {
        // intialize Logger consoles
        Handler fileHandler = initializeLogger("_configuration");

        LOGGER.config("Starting parsing of WorkflowMonitor");
        WorkflowMonitorFactory factory = WorkflowMonitorFactory.newInstance();
        WorkflowMonitor monitor;
        try {
            monitor = factory.newWorkflowMonitor();

            /*
            Creates two maps, one for the list of workflow and one for the processes.
            Each Workflow is identified by its name, that must be unique.
            Each Process is identified by an ID, that will be unique.
             */
            workflowMap = new ConcurrentHashMap<>();
            processMap = new ConcurrentHashMap<>();

            Set<WorkflowReader> workflowReaders = monitor.getWorkflows();
            for (WorkflowReader workflowReader : workflowReaders) {
                // Professor implementation cannot be trust
                // it doesn't let me play with the Set because it uses a Map
                WorkflowReader newWorkflow = new WorkflowReaderImpl(workflowReader);
                workflowMap.put(workflowReader.getName(), newWorkflow);

                Set<ProcessReader> processReaders = workflowReader.getProcesses();
                for (ProcessReader processReader : processReaders) {

                    UUID id = UUID.randomUUID();
                    // done for security reason, but should never happen because there are 2^128 possible keys
                    while (processMap.containsKey(id)) {
                        id = UUID.randomUUID();
                    }
                    // Professor implementation cannot be trust
                    // it doesn't let me play with the Set because it uses a Map
                    ProcessReader newProcessReader = new ProcessReaderImpl(id, processReader, newWorkflow);
                    for (ActionStatusReader statusReader : processReader.getStatus()) {
                        ActionStatusReader newStatus = new ActionStatusReaderImpl(statusReader);
                        newProcessReader.getStatus().add(newStatus);
                    }
                    processMap.put(id, newProcessReader);
                    newWorkflow.getProcesses().add(newProcessReader);
                }
                LOGGER.info(newWorkflow.getName() + " --> number of processes is " + newWorkflow.getProcesses().size());
            }
        } catch (WorkflowMonitorException e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        LOGGER.config("WorkflowServiceImpl completed");
        if (fileHandler != null) {
            fileHandler.close();
        }
        initializeLogger("_execution");
    }

    @WebMethod
    @Override
    public void workflowInfo(Holder<Workflows> workflows,
                             Holder<Calendar> timestamp) throws Fault_Exception {
        LOGGER.info("workflowInfo called");

        // The key set of the WorkflowMap doesn't need protection against concurrency because WorkflowMap will never
        // be modified.
        Set<String> workflowMapKeys = workflowMap.keySet();

        timestamp.value = Calendar.getInstance();
        LOGGER.info("Timestamp for client " + simpleDateFormat.format(timestamp.value.getTime()));

        JAXBBoss boss = new JAXBBoss();
        for (String workflowName : workflowMapKeys) {
            // synchronizes on the Workflow, so it cannot be modified during the reading
            WorkflowReader workflowReader = workflowMap.get(workflowName);
            try {
                boss.addWorkflow(workflowReader);
            } catch (WorkflowMonitorException e) {
                LOGGER.info(e.getMessage());
                throwFaultException(e.getMessage(), "WorkflowInfo Error", e.getMessage());
            }
        }

        // check validity of workflowsJAXB
        if (!boss.checkValidity(timestamp.value, LOGGER)) {
            throwFaultException("ServerError", "ServerError", "Server error processing data");
        }

        workflows.value = boss.getJAXBResult();
        LOGGER.info("workflowInfo completed");
    }

    @WebMethod
    @Override
    public void newProcesses(String digest, String seed, List<String> processWorkflow,
                             Holder<List<String>> processWorkflowID, Holder<Calendar> timestamp)
            throws Fault_Exception {
        // check validity of digest sent
        try {
            String serverDigest = generateSHA256Digest(seed);
            if (!serverDigest.equals(digest)) {
                LOGGER.severe("Digest error " + serverDigest + " seed " + seed + " digest " + digest);
                throwFaultException("Authentication", "Authentication Failed", "Failed authentication");
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, e.getClass().getName(), e.getCause());
            throwFaultException("Error in newProcess", "Error in newProcess", "Server error");
        }

        // check that there's at least a workflow name
        if (processWorkflow.size() == 0) {
            LOGGER.warning("Process size is empty");
            throwFaultException("Error in newProcesses", "Control", "The list of workflow names is empty");
        }

        timestamp.value = Calendar.getInstance();

        // check existence of all the workflow names passed as parameters
        for (String workflowName : processWorkflow) {
            if (!workflowMap.containsKey(workflowName)) {
                LOGGER.severe(workflowName + " not present");
                throwFaultException("Error in newProcesses", "Control", "Workflow doesn't exist with name " + workflowName);
            }
        }

        List<String> processesID = new ArrayList<>();
        for (String workflowName : processWorkflow) {
            // thread-safe operation
            WorkflowReader workflowReader = workflowMap.get(workflowName);
            boolean value;
            UUID uuid;
            synchronized (lockForID) {
                LOGGER.info("lockForID-locked");

                uuid = UUID.randomUUID();
                // nearly impossible - UUID has 2^128 possible keys
                while (processMap.containsKey(uuid)) {
                    uuid = UUID.randomUUID();
                }
                ProcessReader processReader = instantiateNewProcess(uuid, workflowReader);
                processMap.put(uuid, processReader);
                LOGGER.info("inserted process with ID = " + uuid.toString());

                // a ProcessReader is added to the list of Processes only in this creation method
                value = workflowReader.getProcesses().add(processReader);
            }
            LOGGER.info("process inserted in the set? " + value);
            LOGGER.info("lockForID-unlocked");

            processesID.add(uuid.toString());

        }
        processWorkflowID.value = processesID;
    }

	/*
     * Last two methods should not be implemented
	 */

    @WebMethod
    @Override
    public void takeAction(String processWorkflowID, String actionName,
                           Actor actor, Holder<Boolean> success, Holder<Calendar> timestamp)
            throws Fault_Exception {
        return;
    }

    @WebMethod
    @Override
    public void completeAction(String processWorkflowID, String actionName,
                               Holder<Boolean> success, Holder<Calendar> timestamp)
            throws Fault_Exception {
        return;
    }

    private ProcessReader instantiateNewProcess(UUID uuid, WorkflowReader workflowReader) {
        Set<ActionReader> actions = workflowReader.getActions();
        List<ActionStatusReader> statusReaders = new ArrayList<>();
        for (ActionReader actionReader : actions) {
            if (actionReader.isAutomaticallyInstantiated()) {
                statusReaders.add(new ActionStatusReaderImpl(actionReader.getName(), null, null));
            }
        }
        ProcessReader processReader = new ProcessReaderImpl(uuid, Calendar.getInstance(), statusReaders, workflowReader);
        return processReader;
    }

    // generates a SHA-256 digest of the password with a seed
    private String generateSHA256Digest(String seed) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] passwordBytes = password.getBytes("UTF-8");
        byte[] seedBytes = seed.getBytes("UTF-8");
        messageDigest.update(passwordBytes);
        messageDigest.update(seedBytes);
        return Arrays.toString(messageDigest.digest());
    }


    private Handler initializeLogger(String nameExtension) {
        try {
            SimpleDateFormat simpleDateFormatForFile = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss-z");
            String timestampLog = simpleDateFormatForFile.format(Calendar.getInstance().getTime());
            Handler fileHandler = new FileHandler(defaultFileName + nameExtension + timestampLog + ".log");
            fileHandler.setFormatter(new SimpleFormatter());

            LOGGER.addHandler(fileHandler);
            // avoid logging on console if fileHandler is set
            LOGGER.setUseParentHandlers(false);

            fileHandler.setLevel(Level.ALL);
            LOGGER.setLevel(Level.ALL);

            LOGGER.config("Logger Configuration done.");
            return fileHandler;
        } catch (IOException e) {
            LOGGER.warning("Error in FileHandler definition\n" + e.getMessage());
        }
        return null;
    }

    private void throwFaultException(String message, String faultInfo, String faultMessage) throws Fault_Exception {
        Fault fault = new Fault();
        fault.setFaultInfo(faultInfo);
        fault.setFaultMessage(faultMessage);
        throw new Fault_Exception(message, fault);
    }
}

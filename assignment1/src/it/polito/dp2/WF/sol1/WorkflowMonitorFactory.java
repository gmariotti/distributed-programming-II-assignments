package it.polito.dp2.WF.sol1;

import it.polito.dp2.WF.WorkflowMonitor;
import it.polito.dp2.WF.WorkflowMonitorException;

public class WorkflowMonitorFactory extends it.polito.dp2.WF.WorkflowMonitorFactory {
    private String filename;

    @Override
    public WorkflowMonitor newWorkflowMonitor() throws WorkflowMonitorException {
        // read the filename from the propriety and return the monitor
        filename = System.getProperty("it.polito.dp2.WF.sol1.WFInfo.file", "NoPropertyFound");
        if (filename.equals("NoPropertyFound")) {
            System.exit(1);
        }

        return new WorkflowMonitorImpl(filename);
    }
}

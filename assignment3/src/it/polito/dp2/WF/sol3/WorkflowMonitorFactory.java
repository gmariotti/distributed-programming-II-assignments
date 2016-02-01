package it.polito.dp2.WF.sol3;

import it.polito.dp2.WF.WorkflowMonitor;
import it.polito.dp2.WF.WorkflowMonitorException;
import it.polito.dp2.WF.lab3.gen.WorkflowInfo;
import it.polito.dp2.WF.lab3.gen.WorkflowInfoService;

import java.net.MalformedURLException;
import java.net.URL;

public class WorkflowMonitorFactory extends it.polito.dp2.WF.WorkflowMonitorFactory {
    String property = "it.polito.dp2.WF.sol3.URL";

    @Override
    public WorkflowMonitor newWorkflowMonitor() throws WorkflowMonitorException {
        String urlString = System.getProperty(property);
        if (urlString == null) {
            throw new WorkflowMonitorException("No property found for " + property);
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new WorkflowMonitorException("MalformedURLException in WorkflowMonitorFactory");
        }
        WorkflowInfoService service = new WorkflowInfoService(url);
        WorkflowInfo port = service.getWorkflowInfoPort();

        return new WorkflowMonitorImpl(port);
    }
}

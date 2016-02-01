package it.polito.dp2.WF.sol4.client1;

import it.polito.dp2.WF.WorkflowMonitor;
import it.polito.dp2.WF.WorkflowMonitorException;
import it.polito.dp2.WF.sol4.client1.wsdl.WorkflowServiceImplService;
import it.polito.dp2.WF.sol4.client1.wsdl.WorkflowServicePortType;

import java.net.MalformedURLException;
import java.net.URL;

public class WorkflowMonitorFactory extends it.polito.dp2.WF.WorkflowMonitorFactory {
    String property = "it.polito.dp2.WF.sol4.URL";

    @Override
    public WorkflowMonitor newWorkflowMonitor() throws WorkflowMonitorException {
        WorkflowServiceImplService service;
        String urlString = System.getProperty(property);
        if (urlString == null) {
            service = new WorkflowServiceImplService();
        } else {
            try {
                URL url = new URL(urlString);
                service = new WorkflowServiceImplService(url);
            } catch (MalformedURLException e) {
                throw new WorkflowMonitorException("MalformedURLException in WorkflowMonitorFactory");
            }
        }

        WorkflowServicePortType port = service.getWorkflowServiceImplPort();

        return new WorkflowMonitorImpl(port);
    }
}

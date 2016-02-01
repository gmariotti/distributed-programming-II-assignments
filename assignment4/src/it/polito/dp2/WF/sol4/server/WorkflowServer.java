package it.polito.dp2.WF.sol4.server;

import javax.xml.ws.Endpoint;
import java.util.concurrent.Executors;

public class WorkflowServer {
    private static final int numThreads = 10;

    public static void main(String args[]) {
        WorkflowServiceImpl service = new WorkflowServiceImpl();

        Endpoint endpoint1 = Endpoint.create(service);
        endpoint1.setExecutor(Executors.newFixedThreadPool(numThreads));
        endpoint1.publish("http://localhost:7070/wfcontrol");
        Endpoint endpoint2 = Endpoint.create(service);
        endpoint2.setExecutor(Executors.newFixedThreadPool(numThreads));
        endpoint2.publish("http://localhost:7071/wfinfo");
    }
}

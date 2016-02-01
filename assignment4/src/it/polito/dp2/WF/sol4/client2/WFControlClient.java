package it.polito.dp2.WF.sol4.client2;

import it.polito.dp2.WF.sol4.client2.wsdl.FaultException;
import it.polito.dp2.WF.sol4.client2.wsdl.WorkflowServiceImplService;
import it.polito.dp2.WF.sol4.client2.wsdl.WorkflowServicePortType;

import javax.xml.ws.Holder;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class WFControlClient {
    private static final String serverPassword = "secret_password_with_server";
    private static final String seed = UUID.randomUUID().toString();

    public static void main(String args[]) {
        // check number of arguments
        if (args.length != 2) {
            System.err.println("Args: <URL> <WorkflowName>");
            System.exit(2);
        }

        // connect to server
        try {
            URL url = new URL(args[0]);
            WorkflowServiceImplService service = new WorkflowServiceImplService(url);
            WorkflowServicePortType port = service.getWorkflowServiceImplPort();

            // prepare list of workflow name
            // for assignment 4, just one workflow name will be passed
            List<String> workflowNames = new ArrayList<>();
            workflowNames.add(args[1]);

            // prepare digest for server
            String digest = generateSHA256Digest();

            // not used but still passed as parameters
            Holder<List<String>> holderProcessesID = new Holder<>();
            holderProcessesID.value = new ArrayList<>();
            Holder<Calendar> holderTimestamp = new Holder<>();
            holderTimestamp.value = null;
            port.newProcesses(digest, seed, workflowNames, holderProcessesID, holderTimestamp);

            // success
            System.exit(0);
        } catch (MalformedURLException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            System.exit(2);
        } catch (FaultException e) {
            System.err.println(e.getFaultInfo().getFaultInfo() + " " + e.getFaultInfo().getFaultMessage());
            // server error in processing everything
            System.exit(1);
        }
    }

    /*
    Used for authentication with the server
     */
    private static String generateSHA256Digest() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] serverPasswordBytes = serverPassword.getBytes("UTF-8");
        byte[] seedBytes = seed.getBytes("UTF-8");
        messageDigest.update(serverPasswordBytes);
        messageDigest.update(seedBytes);
        return Arrays.toString(messageDigest.digest());
    }


}

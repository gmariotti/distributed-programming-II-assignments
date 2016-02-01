package it.polito.dp2.WF.sol2;

import it.polito.dp2.WF.WorkflowMonitor;
import it.polito.dp2.WF.WorkflowMonitorException;
import it.polito.dp2.WF.sol2.jaxb.WFInfo;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class WorkflowMonitorFactory extends it.polito.dp2.WF.WorkflowMonitorFactory {
    private String filename;

    @Override
    public WorkflowMonitor newWorkflowMonitor() throws WorkflowMonitorException {
        // read the filename from the propriety and return the monitor
        filename = System.getProperty("it.polito.dp2.WF.sol2.WorkflowInfo.file", "NoPropertyFound");
        if (filename.equals("NoPropertyFound")) {
            System.err.println("Error in filename property");
            System.exit(1);
        }

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new File("xsd/WFInfo.xsd"));
        } catch (SAXException e) {
            throw new WorkflowMonitorException("Error in defining the schema");
        }

        JAXBContext context;
        try {
            context = JAXBContext.newInstance("it.polito.dp2.WF.sol2.jaxb");
        } catch (JAXBException e) {
            throw new WorkflowMonitorException("Error generating the context");
        }

        Unmarshaller unmarshaller;
        try {
            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new WorkflowMonitorException("Error generating Unmarshaller");
        }
        unmarshaller.setSchema(schema);
        WFInfo wfInfo;
        try {
            wfInfo = (WFInfo) unmarshaller.unmarshal(new File(filename));
        } catch (JAXBException e) {
            throw new WorkflowMonitorException("Error generating WFInfo from schema");
        }

        return new WorkflowMonitorImpl(wfInfo.getWorkflow());
    }
}

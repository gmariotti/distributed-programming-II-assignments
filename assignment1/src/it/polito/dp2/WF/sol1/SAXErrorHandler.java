package it.polito.dp2.WF.sol1;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;

public class SAXErrorHandler implements ErrorHandler {
    private PrintWriter output;

    public SAXErrorHandler(PrintWriter output) {
        this.output = output;
    }

    private String getSAXExceptionInfo(SAXParseException spe) {
        String systemId = spe.getSystemId();
        if (systemId == null) {
            systemId = "null";
        }

        String info = "URI = " + systemId
                + " Line = " + spe.getLineNumber()
                + ": " + spe.getMessage();
        return info;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        output.println("Warning: " + getSAXExceptionInfo(exception));
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        String message = "Error: " + getSAXExceptionInfo(exception);
        throw new SAXException(message);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        String message = "Fatal Error: " + getSAXExceptionInfo(exception);
        throw new SAXException(message);
    }
}

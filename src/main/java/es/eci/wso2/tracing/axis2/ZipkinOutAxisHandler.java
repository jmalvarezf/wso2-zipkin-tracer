package es.eci.wso2.tracing.axis2;

import brave.Span;
import brave.http.HttpServerHandler;
import es.eci.wso2.tracing.Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ZipkinOutAxisHandler extends AbstractHandler {

    private static Log log = LogFactory.getLog(ZipkinOutAxisHandler.class);

    public ZipkinOutAxisHandler() {
    }

    @Override
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        //this method is invoked every time in Axis2 chain!!
        log.info("Invoking Axis2 Out Handler for tracing");
        if ((messageContext.getProperty(Constants.TRACE_HANDLER_PROPERTY_NAME) != null) && (messageContext
                .getProperty(Constants.SERVER_SPAN_PROPERTY_NAME) != null) && (messageContext.getProperty(Constants
                .CLOSE_SERVER_SPAN_PROPERTY_NAME) != null && (Boolean) messageContext.getProperty(Constants
                .CLOSE_SERVER_SPAN_PROPERTY_NAME))) {
            HttpServerHandler handler = (HttpServerHandler) messageContext.getProperty(Constants
                    .TRACE_HANDLER_PROPERTY_NAME);
            Span serverSpan = (Span) messageContext.getProperty(Constants.SERVER_SPAN_PROPERTY_NAME);
            log.debug("Closing server span: " + serverSpan);
            handler.handleSend(messageContext, null, serverSpan);
        }
        return InvocationResponse.CONTINUE;
    }

}

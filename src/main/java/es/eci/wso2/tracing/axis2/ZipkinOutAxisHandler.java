package es.eci.wso2.tracing.axis2;

import brave.Span;
import brave.http.HttpServerHandler;
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
        //esto se invoca CADA VEZ, luego hay que ver como trazar si es la petición tras synapse
        log.info("Invoking Axis2 Out Handler for tracing");
        if ((messageContext.getProperty("TRACE_HANDLER") != null) && (messageContext.getProperty("SERVER_SPAN") != null) && (messageContext.getProperty("CLOSE_SERVER_SPAN") != null && (Boolean) messageContext.getProperty("CLOSE_SERVER_SPAN"))) {
            HttpServerHandler handler = (HttpServerHandler) messageContext.getProperty("TRACE_HANDLER");
            Span serverSpan = (Span) messageContext.getProperty("SERVER_SPAN");
            log.debug("Closing server span: " + serverSpan);
            handler.handleSend(messageContext, null, serverSpan);
        }
//            else if ((messageContext.getProperty("CLOSE_SERVER_SPAN") instanceof String) && messageContext.getProperty("CLOSE_SERVER_SPAN").toString().equalsIgnoreCase("true")) {
//                HttpServerHandler handler = (HttpServerHandler) messageContext.getProperty("TRACE_HANDLER");
//                Span serverSpan = (Span) messageContext.getProperty("SERVER_SPAN");
//                log.warn("Closing server span");
//                log.warn("Server span: " + serverSpan);
//            }
        //} catch (Exception e) {
        //    log.error("Error starting a span", e);
        //handler.handleSend(messageContext, e, span);
        //}

        return InvocationResponse.CONTINUE;
    }

}

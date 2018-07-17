package es.eci.wso2.tracing;

import brave.Span;
import brave.Tracing;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.util.Locale;

public class ZipkinOutAxisHandler extends AbstractHandler {

    private static Log log = LogFactory.getLog(ZipkinOutAxisHandler.class);

    public ZipkinOutAxisHandler() {
    }

    @Override
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        log.warn("Invoking out handler for tracing");
        if ((messageContext.getProperty("TRACE_HANDLER") != null) && (messageContext.getProperty("SERVER_SPAN") != null)) {
            HttpServerHandler handler = (HttpServerHandler) messageContext.getProperty("TRACE_HANDLER");
            Span serverSpan = (Span) messageContext.getProperty("SERVER_SPAN");
            log.warn("Invoking tracer");
            log.warn("Span: " + serverSpan);
            handler.handleSend(messageContext, null, serverSpan);
            //} catch (Exception e) {
            //    log.error("Error starting a span", e);
            //handler.handleSend(messageContext, e, span);
            //}
        }
        return InvocationResponse.CONTINUE;
    }

}

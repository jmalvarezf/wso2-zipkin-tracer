package es.eci.wso2.tracing.axis2;

import brave.Span;
import brave.Tracing;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import es.eci.wso2.tracing.Axis2ServerAdapter;
import es.eci.wso2.tracing.LoggingReporter;
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

public class ZipkinInAxisHandler extends AbstractHandler {

    private static Log log = LogFactory.getLog(ZipkinInAxisHandler.class);

    private HttpTracing tracing;

    private Tracing braveTracing;

    private HttpServerHandler handler;

    private synchronized void initTracers(String serviceName, String zipkinUrl, String samplingRate) {
        if (handler == null) {
            if (zipkinUrl == null) {
                braveTracing = Tracing.newBuilder()
                        .localServiceName(serviceName)
                        .propagationFactory(B3Propagation.FACTORY)
                        .spanReporter(new LoggingReporter())
                        .supportsJoin(false)
                        .sampler(samplingRate == null ? Sampler.ALWAYS_SAMPLE : Sampler.create(Float.parseFloat(samplingRate)))
                        .build();
            }
            else {
                Sender sender = OkHttpSender.create(zipkinUrl + "/api/v2/spans");
                Reporter reporter = AsyncReporter.create(sender);
                braveTracing = Tracing.newBuilder()
                        .localServiceName(serviceName)
                        .propagationFactory(B3Propagation.FACTORY)
                        .spanReporter(reporter)
                        .supportsJoin(false)
                        .sampler(samplingRate == null ? Sampler.ALWAYS_SAMPLE : Sampler.create(Float.parseFloat(samplingRate)))
                        .build();
            }

            // use this to create a Tracer
            tracing = HttpTracing.create(braveTracing);
            handler = HttpServerHandler.create(tracing, new Axis2ServerAdapter());
            log.debug("End configuring server tracer");
        }
    }

    @Override
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        initTracers(messageContext.getConfigurationContext()
                .getAxisConfiguration().getParameter("serviceName").getValue().toString(), messageContext.getConfigurationContext()
                .getAxisConfiguration().getParameter("zipkinUrl") == null ? null : messageContext.getConfigurationContext()
                .getAxisConfiguration().getParameter("zipkinUrl").getValue().toString(), messageContext.getConfigurationContext()
                .getAxisConfiguration().getParameter("samplingRate").getValue().toString());
        log.info("Invoking Axis2 In Handler for tracing");
        //log.warn("Parameter 2: " + moduleConfig.getParameter("serviceName"));
        Span span = null;
        if (messageContext.getProperty("HTTP_METHOD") != null) {
            log.debug("Invoking server tracer");
            try {
                TraceContext.Extractor extractor = braveTracing.propagation().extractor(MESSAGE_CONTEXT_GETTER);
                span = handler.handleReceive(extractor, messageContext);
                messageContext.setProperty("TRACE_HANDLER", handler);
                messageContext.setProperty("SERVER_SPAN", span);
                log.debug("Server span: " + span);
            } catch (Exception e) {
                log.error("Error starting a span", e);
            }
        }
        return InvocationResponse.CONTINUE;
    }

    static final Propagation.Getter<MessageContext, String> MESSAGE_CONTEXT_GETTER =
            new Propagation.Getter<MessageContext, String>() {
                @Override
                public String get(MessageContext carrier, String key) {
                    if (carrier != null) {
                        log.debug("Trying to get header: " + key);
                        java.util.Map<String, String> headers = (java.util.Map) carrier.getProperty(MessageContext.TRANSPORT_HEADERS);
                        log.debug("Value: " + headers.get(key.toLowerCase(Locale.ROOT)));
                        return headers.get(key.toLowerCase(Locale.ROOT));
                    }
                    else {
                        log.error("Message context is null");
                        return null;
                    }
                }

                @Override
                public String toString() {
                    return "MessageContext::getLowerCase";
                }
            };
}

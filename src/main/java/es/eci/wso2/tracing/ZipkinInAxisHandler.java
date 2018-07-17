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

public class ZipkinInAxisHandler extends AbstractHandler {

    private static Log log = LogFactory.getLog(ZipkinInAxisHandler.class);

    private final HttpTracing tracing;

    private final Tracing braveTracing;

    private final HttpServerHandler handler;

    private final CurrentTraceContext currentTraceContext;

    public ZipkinInAxisHandler() {
        super();
        Sender sender = OkHttpSender.create("http://marathon-lb.azure.firefly.elcorteingles.es:10121/api/v2/spans");
        Reporter reporter = AsyncReporter.create(sender);


//        Propagation.Factory propagationFactory = ExtraFieldPropagation.newFactoryBuilder(B3Propagation.FACTORY).addField("server").build();

        // Now, create a Brave tracing component with the service name you want to see in Zipkin.
        //   (the dependency is io.zipkin.brave:brave)
        braveTracing = Tracing.newBuilder()
                .localServiceName("api-manager")
                .propagationFactory(B3Propagation.FACTORY)
                .spanReporter(new LoggingReporter())
                .supportsJoin(false)
//                .currentTraceContext()
                .sampler(Sampler.ALWAYS_SAMPLE)
                .build();

        // use this to create a Tracer
        tracing = HttpTracing.create(braveTracing);
        currentTraceContext = tracing.tracing().currentTraceContext();
        handler = HttpServerHandler.create(tracing, new Axis2ServerAdapter());
        //tracer = .tracing().tracer();
        log.warn("End configuring tracer");
    }

    @Override
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        log.warn("Invoking handler for tracing");
        Span span = null;
        if (messageContext.getProperty("HTTP_METHOD") != null) {
            log.warn("Invoking tracer");
            try {
                TraceContext.Extractor extractor = braveTracing.propagation().extractor(MESSAGE_CONTEXT_GETTER);
                span = handler.handleReceive(extractor, messageContext);
                messageContext.setProperty("TRACE_HANDLER", handler);
                messageContext.setProperty("SERVER_SPAN", span);
                log.warn("Span: " + span + " " + span.isNoop());
                //span.tag("http.path", "/kk");
                //handler.handleSend(messageContext, null, span);
            } catch (Exception e) {
                log.error("Error starting a span", e);
                //handler.handleSend(messageContext, e, span);
            }
        }
        return InvocationResponse.CONTINUE;
    }

    static final Propagation.Getter<MessageContext, String> MESSAGE_CONTEXT_GETTER =
            new Propagation.Getter<MessageContext, String>() {
                @Override
                public String get(MessageContext carrier, String key) {
                    if (carrier != null) {
                        log.warn("Trying to get header: " + key);
                        java.util.Map<String, String> headers = (java.util.Map) carrier.getProperty(MessageContext.TRANSPORT_HEADERS);
                        log.warn("Value: " + headers.get(key.toLowerCase(Locale.ROOT)));
                        return headers.get(key.toLowerCase(Locale.ROOT));
                    }
                    else {
                        log.warn("Message context is null");
                        return null;
                    }
                }

                @Override
                public String toString() {
                    return "MessageContext::getLowerCase";
                }
            };
}

package es.eci.wso2.tracing.synapse;

import brave.Span;
import brave.Tracing;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import es.eci.wso2.tracing.LoggingReporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.rest.RESTConstants;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class ApiManagerZipkinTracingHandler extends AbstractHandler implements ManagedLifecycle {

    private static Log log = LogFactory.getLog(ApiManagerZipkinTracingHandler.class);

    private String localServiceName;

    private String zipkinUrl;

    private String samplingRate;

    private Tracing braveTracing;

    private HttpClientHandler handler;


    public String getLocalServiceName() {
        return localServiceName;
    }

    public void setLocalServiceName(String localServiceName) {
        this.localServiceName = localServiceName;
    }

    public String getZipkinUrl() {
        return zipkinUrl;
    }

    public void setZipkinUrl(String zipkinUrl) {
        this.zipkinUrl = zipkinUrl;
    }

    public String getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(String samplingRate) {
        this.samplingRate = samplingRate;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) {
        try {
            TraceContext.Injector injector = braveTracing.propagation().injector(MESSAGE_CONTEXT_SETTER);

            org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            if (axis2MC.getProperty("SERVER_SPAN") != null) {
                Span serverSpan = (Span) axis2MC.getProperty("SERVER_SPAN");
                braveTracing.currentTraceContext().maybeScope(serverSpan.context());
                Span clientSpan = handler.handleSend(injector, messageContext);
                log.debug("Client span: " + clientSpan);
                serverSpan.tag("gateway.context", (String) messageContext.getProperty(RESTConstants.REST_API_CONTEXT));
                serverSpan.tag("gateway.api.name", (String) messageContext.getProperty(RESTConstants.SYNAPSE_REST_API));
                serverSpan.tag("gateway.api.version", (String) messageContext.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION));
                if (messageContext.getProperty("api.ut.consumerKey") != null) {
                    serverSpan.tag("gateway.consumer.key", (String) messageContext.getProperty("api.ut.consumerKey"));
                }
                if (messageContext.getProperty("api.ut.resource") != null) {
                    serverSpan.tag("gateway.api.resource", (String) messageContext.getProperty("api.ut.resource"));
                }
                if (messageContext.getProperty("api.ut.application.name") != null) {
                    serverSpan.tag("gateway.application.name", (String) messageContext.getProperty("api.ut.application.name"));
                }
                messageContext.setProperty("CLIENT_SPAN", clientSpan);
                messageContext.setProperty("SERVER_SPAN", serverSpan);
                messageContext.setProperty("TRACE_HANDLER", axis2MC.getProperty("TRACE_HANDLER"));
            }
            //handler.handleSend(messageContext, null, span);
        } catch (Exception e) {
            log.error("Error starting a client span", e);
            //handler.handleSend(messageContext, e, span);
        }
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) {
        if (handler != null && messageContext.getProperty("CLIENT_SPAN") != null) {
            Span clientSpan = (Span) messageContext.getProperty("CLIENT_SPAN");
            Span serverSpan = (Span) messageContext.getProperty("SERVER_SPAN");
            clientSpan.tag("gateway.endpoint", (String) messageContext.getProperty("ENDPOINT_ADDRESS"));
            handler.handleReceive(messageContext, null, clientSpan);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty("CLOSE_SERVER_SPAN", true);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty("SERVER_SPAN", messageContext.getProperty("SERVER_SPAN"));
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty("TRACE_HANDLER", messageContext.getProperty("TRACE_HANDLER"));
        }
        return true;
    }

    @Override
    public void init(SynapseEnvironment se) {
        if (zipkinUrl == null) {
            braveTracing = Tracing.newBuilder()
                    .localServiceName(localServiceName)
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
                    .localServiceName(localServiceName)
                    .propagationFactory(B3Propagation.FACTORY)
                    .spanReporter(reporter)
                    .supportsJoin(false)
                    .sampler(samplingRate == null ? Sampler.ALWAYS_SAMPLE : Sampler.create(Float.parseFloat(samplingRate)))
                    .build();
        }

        // use this to create a Tracer
        HttpTracing tracing = HttpTracing.create(braveTracing);
        handler = HttpClientHandler.create(tracing, new SynapseClientAdapter());
        log.debug("End configuring client tracer");
    }

    @Override
    public void destroy() {
        braveTracing.close();
    }

    static final Propagation.Setter<MessageContext, String> MESSAGE_CONTEXT_SETTER =
            new Propagation.Setter<MessageContext, String>() {
                @Override
                public void put(MessageContext carrier, String key, String value) {
                    if (carrier != null) {
                        log.debug("Trying to set header: " + key + " with value " + value);
                        java.util.Map<String, String> headers = (java.util.Map) ((Axis2MessageContext) carrier).getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                        headers.put(key, value);
                    }
                    else {
                        log.warn("Message context is null");
                    }
                }

                @Override
                public String toString() {
                    return "MessageContext::set";
                }
            };
}

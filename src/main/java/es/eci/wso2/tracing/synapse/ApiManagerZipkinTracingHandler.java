package es.eci.wso2.tracing.synapse;

import brave.Span;
import brave.Tracing;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import es.eci.wso2.tracing.Constants;
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
            if (axis2MC.getProperty(Constants.SERVER_SPAN_PROPERTY_NAME) != null) {
                Span serverSpan = (Span) axis2MC.getProperty(Constants.SERVER_SPAN_PROPERTY_NAME);
                braveTracing.currentTraceContext().maybeScope(serverSpan.context());
                Span clientSpan = handler.handleSend(injector, messageContext);
                log.debug("Client span: " + clientSpan);
                serverSpan.tag(Constants.GATEWAY_CONTEXT_TAG_PROPERTY, (String) messageContext.getProperty(RESTConstants.REST_API_CONTEXT));
                serverSpan.tag(Constants.GATEWAY_API_NAME_PROPERTY, (String) messageContext.getProperty(RESTConstants.SYNAPSE_REST_API));
                serverSpan.tag(Constants.GATEWAY_API_VERSION_PROPERTY, (String) messageContext.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION));
                if (messageContext.getProperty(Constants.SYNAPSE_API_CONSUME_KEY_PROPERTY) != null) {
                    serverSpan.tag(Constants.GATEWAY_CONSUMER_KEY_PROPERTY, (String) messageContext.getProperty(Constants.SYNAPSE_API_CONSUME_KEY_PROPERTY));
                }
                if (messageContext.getProperty(Constants.SYNAPSE_API_RESOURCE_PROPERTY) != null) {
                    serverSpan.tag(Constants.GATEWAY_API_RESOURCE_PROPERTY, (String) messageContext.getProperty(Constants.SYNAPSE_API_RESOURCE_PROPERTY));
                }
                if (messageContext.getProperty(Constants.SYNAPSE_APPLICATION_NAME_PROPERTY) != null) {
                    serverSpan.tag(Constants.GATEWAY_APPLICATION_NAME_PROPERTY, (String) messageContext.getProperty(Constants.SYNAPSE_APPLICATION_NAME_PROPERTY));
                }
                messageContext.setProperty(Constants.CLIENT_SPAN_PROPERTY_NAME, clientSpan);
                messageContext.setProperty(Constants.SERVER_SPAN_PROPERTY_NAME, serverSpan);
                messageContext.setProperty(Constants.TRACE_HANDLER_PROPERTY_NAME, axis2MC.getProperty(Constants.TRACE_HANDLER_PROPERTY_NAME));
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
        if (handler != null && messageContext.getProperty(Constants.CLIENT_SPAN_PROPERTY_NAME) != null) {
            Span clientSpan = (Span) messageContext.getProperty(Constants.CLIENT_SPAN_PROPERTY_NAME);
            Span serverSpan = (Span) messageContext.getProperty(Constants.SERVER_SPAN_PROPERTY_NAME);
            clientSpan.tag(Constants.GATEWAY_ENDPOINT_TAG_PROPERTY, (String) messageContext.getProperty(Constants.SYNAPSE_ENDPOINT_ADDRESS_PROPERTY));
            handler.handleReceive(messageContext, null, clientSpan);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty(Constants.CLOSE_SERVER_SPAN_PROPERTY_NAME, true);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty(Constants.SERVER_SPAN_PROPERTY_NAME, messageContext.getProperty
                    (Constants.SERVER_SPAN_PROPERTY_NAME));
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty(Constants.TRACE_HANDLER_PROPERTY_NAME, messageContext.getProperty
                    (Constants.TRACE_HANDLER_PROPERTY_NAME));
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
            Sender sender = OkHttpSender.create(zipkinUrl + Constants.ZIPKIN_API_V2_URL);
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
                        java.util.Map<String, String> headers = (java.util.Map) ((Axis2MessageContext) carrier).getAxis2MessageContext().getProperty(org
                                .apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
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

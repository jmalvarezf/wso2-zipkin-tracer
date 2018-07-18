package es.eci.wso2.tracing.synapse;

import brave.http.HttpClientAdapter;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import java.util.Map;

public class SynapseClientAdapter extends HttpClientAdapter<MessageContext, MessageContext> {

    @Override
    public String method(MessageContext messageContext) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        String method = axis2MC.getProperty("HTTP_METHOD").toString();
        return method;
    }

    @Override
    public String url(MessageContext messageContext) {
        return (String) messageContext.getProperty("ENDPOINT_ADDRESS");
    }

    @Override
    public String requestHeader(MessageContext messageContext, String s) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        Map<String, String> headers = (Map<String, String>) axis2MC.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        return headers.get(s);
    }

    @Override
    public Integer statusCode(MessageContext messageContext) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        return (Integer) axis2MC.getProperty(SynapseConstants.HTTP_SC);
    }
}

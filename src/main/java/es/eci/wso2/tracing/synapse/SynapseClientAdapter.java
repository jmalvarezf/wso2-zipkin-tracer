package es.eci.wso2.tracing.synapse;

import brave.http.HttpClientAdapter;
import es.eci.wso2.tracing.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.Map;

public class SynapseClientAdapter extends HttpClientAdapter<MessageContext, MessageContext> {

    private static Log log = LogFactory.getLog(SynapseClientAdapter.class);

    @Override
    public String method(MessageContext messageContext) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        String method = axis2MC.getProperty(Constants.AXIS2_HTTP_METHOD_PROPERTY).toString();
        return method;
    }

    @Override
    public String url(MessageContext messageContext) {
        return (String) messageContext.getProperty(Constants.SYNAPSE_ENDPOINT_ADDRESS_PROPERTY);
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
        if (axis2MC.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) != null && axis2MC.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) instanceof String) {
            return Integer.parseInt(axis2MC.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY).toString());
        }
        else if (axis2MC.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) != null && axis2MC.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) instanceof Integer) {
            return (Integer) axis2MC.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY);
        }
        else {
            log.error("No response code found");
            return 0;
        }
    }
}

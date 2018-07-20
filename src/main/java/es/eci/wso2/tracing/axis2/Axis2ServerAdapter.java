package es.eci.wso2.tracing.axis2;

import brave.http.HttpServerAdapter;
import es.eci.wso2.tracing.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Axis2ServerAdapter extends HttpServerAdapter<MessageContext, MessageContext> {

    private static Log log = LogFactory.getLog(Axis2ServerAdapter.class);

    @Override
    public String method(MessageContext req) {
        String method = req.getProperty(Constants.AXIS2_HTTP_METHOD_PROPERTY).toString();
        return method;
    }

    @Override
    public String url(MessageContext req) {
        java.util.Map<String, String> headers = (java.util.Map) req.getProperty(MessageContext.TRANSPORT_HEADERS);
        String url = headers.get("Host");
        //TODO: get schema from property
        return "http://" + url + "/" + req.getProperty(Constants.AXIS2_REST_URL_POSTFIX_PROPERTY).toString();
    }

    @Override
    public String requestHeader(MessageContext req, String key) {
        java.util.Map<String, String> headers = (java.util.Map) req.getProperty(MessageContext.TRANSPORT_HEADERS);
        return headers.get(key);
    }

    @Override
    public Integer statusCode(MessageContext resp) {
        if (resp.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) != null && resp.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) instanceof String) {
            return Integer.parseInt(resp.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY).toString());
        }
        else if (resp.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) != null && resp.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY) instanceof Integer) {
            return (Integer) resp.getProperty(Constants.AXIS2_HTTP_STATUS_CODE_PROPERTY);
        }
        else {
            log.error("No response code found");
            return 0;
        }
    }
}

package es.eci.wso2.tracing;

import brave.http.HttpServerAdapter;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Axis2ServerAdapter extends HttpServerAdapter<MessageContext, MessageContext> {

    private static Log log = LogFactory.getLog(Axis2ServerAdapter.class);

    @Override
    public String method(MessageContext req) {
        String method = req.getProperty("HTTP_METHOD").toString();
        return method;
    }

    @Override
    public String url(MessageContext req) {
        java.util.Map<String, String> headers = (java.util.Map) req.getProperty(MessageContext.TRANSPORT_HEADERS);
        String url = headers.get("Host");
        return "http://" + url + "/" + req.getProperty("REST_URL_POSTFIX").toString();
    }

    @Override
    public String requestHeader(MessageContext req, String key) {
        java.util.Map<String, String> headers = (java.util.Map) req.getProperty(MessageContext.TRANSPORT_HEADERS);
        return headers.get(key);
    }

    @Override
    public Integer statusCode(MessageContext resp) {
        if (resp.getProperty("HTTP_SC") != null) {
            return Integer.parseInt(resp.getProperty("HTTP_SC").toString());
        } else {
            log.warn("No response code found");
            return 0;
        }
    }
}

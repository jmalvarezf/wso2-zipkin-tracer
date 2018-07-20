package es.eci.wso2.tracing;

public class Constants {
    public static final String AXIS2_HTTP_METHOD_PROPERTY = "HTTP_METHOD";
    public static final String AXIS2_HTTP_STATUS_CODE_PROPERTY = "HTTP_SC";
    public static final String AXIS2_REST_URL_POSTFIX_PROPERTY = "REST_URL_POSTFIX";
    public static final String SYNAPSE_ENDPOINT_ADDRESS_PROPERTY = "ENDPOINT_ADDRESS";
    public static final String SERVER_SPAN_PROPERTY_NAME = "SERVER_SPAN";
    public static final String CLIENT_SPAN_PROPERTY_NAME = "CLIENT_SPAN";
    public static final String TRACE_HANDLER_PROPERTY_NAME = "TRACE_HANDLER";
    public static final String CLOSE_SERVER_SPAN_PROPERTY_NAME = "CLOSE_SERVER_SPAN";
    public static final String SYNAPSE_API_CONSUME_KEY_PROPERTY = "api.ut.consumerKey";
    public static final String SYNAPSE_API_RESOURCE_PROPERTY = "api.ut.resource";
    public static final String SYNAPSE_APPLICATION_NAME_PROPERTY = "api.ut.application.name";
    public static final String GATEWAY_CONTEXT_TAG_PROPERTY = "gateway.context";
    public static final String GATEWAY_API_NAME_PROPERTY = "gateway.api.name";
    public static final String GATEWAY_API_VERSION_PROPERTY = "gateway.api.version";
    public static final String GATEWAY_CONSUMER_KEY_PROPERTY = "gateway.consumer.key";
    public static final String GATEWAY_API_RESOURCE_PROPERTY = "gateway.api.resource";
    public static final String GATEWAY_ENDPOINT_TAG_PROPERTY = "gateway.endpoint";
    public static final String GATEWAY_APPLICATION_NAME_PROPERTY = "gateway.application.name";
    public static final String ZIPKIN_URL_PARAMETER_NAME = "zipkinUrl";
    public static final String SERVICE_NAME_PARAMETER_NAME = "serviceName";
    public static final String SAMPLING_RATE_PARAMETER_NAME = "samplingRate";
    public static final String ZIPKIN_API_V2_URL = "/api/v2/spans";
}

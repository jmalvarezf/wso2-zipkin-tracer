## WSO2 Zipkin Tracer plugin

This is an OSGI module that allows WSO2 to participate or start on a Trace and report it via log, or sent it to Zipkin asynchronously. There are two different spans that would be created here:

1. Server trace: it is created when the request for an API is received. It is created by the Axis2 chain, and configured in the axis2.xml file:

`<parameter name="serviceName" locked="false">##servicename##</parameter>`
`<parameter name="zipkinUrl" locked="false">##zipkinurl##</parameter>`
`<parameter name="samplingRate" locked="false">1.0</parameter>`

and this:
`<module ref="zipkinHandler"/>`

If zipkinUrl is not present, it will log the spans in the carbon log.

1. Client trace: it is created when an api is called in the mediation process. To make it work, you need to add to your default api template (just before `</handlers>` :

    `<handler class="es.eci.wso2.tracing.synapse.ApiManagerZipkinTracingHandler">`
      `<property name="localServiceName" value="##servicename##" />`
      `<property name="zipkinUrl" value="##zipkinurl##" />`
      `<property name="samplingRate" value="1.0" />`
    `</handler>`

1. It is also needed to touch the default main.xml file found in the synapse sequences directory, and add this property (for failure reporting):

`<property name="CLOSE_SERVER_SPAN" value="true" scope="axis2" type="BOOLEAN" />`

Just after the first log tag you find

This module is in beta stage. It has been only tested with APIM 2.1.0 and not throughly.



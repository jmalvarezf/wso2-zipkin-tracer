## WSO2 Zipkin Tracer plugin

This is an OSGI module that allows WSO2 to participate or start on a Trace and report it via log, or sent it to Zipkin asynchronously, in a microservices arquitecture. It is based on the HTTP instrumentation provided by [Brave](https://github.com/openzipkin/brave/).
There are two different spans that would be created here:

* **Server trace**: it is created when the request for an API is received. It is created by the Axis2 chain, and configured in the axis2.xml file. You would need to add these:

`<parameter name="serviceName" locked="false">##serviceName##</parameter>`
`<parameter name="zipkinUrl" locked="false">##zipkinUrl##</parameter>`
`<parameter name="samplingRate" locked="false">##samplingRate##</parameter>`

and this:
`<module ref="zipkinHandler"/>`

If zipkinUrl is not present, it will log the spans in the carbon log.

* **Client trace**: it is created when an api is called in the mediation process. To make it work, you need to add to your default api template (just before `</handlers>`) :

    `<handler class="es.eci.wso2.tracing.synapse.ApiManagerZipkinTracingHandler">`
      `<property name="localServiceName" value="##serviceName##" />`
      `<property name="zipkinUrl" value="##zipkinUrl##" />`
      `<property name="samplingRate" value="##samplingRate##" />`
    `</handler>`

* It is also needed to touch the default main.xml and fault.xml files found in the synapse sequences directory, and add this property (for failure reporting):

`<property name="CLOSE_SERVER_SPAN" value="true" scope="axis2" type="BOOLEAN" />`

Just after the first `</log>` xml tag you find

This module is in beta stage. It has been only tested with APIM 2.1.0 and not throughly.



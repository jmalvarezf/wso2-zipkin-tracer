package es.eci.wso2.tracing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import zipkin2.reporter.Reporter;

public class LoggingReporter implements Reporter<zipkin2.Span> {

    private static Log log = LogFactory.getLog(LoggingReporter.class);
    @Override
    public void report(zipkin2.Span span) {
           log.info("Report: " + span.toString());
    }
}

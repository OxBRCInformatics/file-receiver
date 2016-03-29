import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender

String defPattern = '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'

def logDir = new File('.', 'logs').canonicalFile
if (!logDir) logDir.mkdirs()

appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = defPattern
    }
    filter(ThresholdFilter) {
        level = DEBUG
    }
}

appender("FILE", RollingFileAppender) {
    file = "${logDir}/filereceive.log"
    append = false
    encoder(PatternLayoutEncoder) {
        pattern = defPattern
    }
}

root(INFO, ['STDOUT', 'FILE'])

logger('ox.softeng', DEBUG)
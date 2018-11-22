/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.apache.camel.examples;

import java.nio.charset.Charset;
import javax.activation.DataHandler;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CamelConfiguration extends RouteBuilder {

  private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);
  
  @Autowired
  private ApplicationProperties properties;
  
  @Bean
  HL7MLLPNettyEncoderFactory hl7Encoder() {
    HL7MLLPNettyEncoderFactory encoder = new HL7MLLPNettyEncoderFactory();
    encoder.setCharset(Charset.forName("iso-8859-1"));
    encoder.setConvertLFtoCR(true);
    return encoder;
  }

  @Bean
  HL7MLLPNettyDecoderFactory hl7Decoder() {
    HL7MLLPNettyDecoderFactory decoder = new HL7MLLPNettyDecoderFactory();
    decoder.setCharset(Charset.forName("iso-8859-1"));
    decoder.setConvertLFtoCR(true);
    return decoder;
  }
  
  @Override
  public void configure() throws Exception {

    fromF("file:%s?antInclude=*.hl7", properties.getIngest().getFile().getDirectory())
      .convertBodyTo(String.class)
      .log(LoggingLevel.DEBUG, log, "HL7 request: [${body}]")
      .setHeader("OriginalHl7Message").body()
      .unmarshal().hl7(true)
      .enrich("direct:processHl7V2", AggregationStrategies.useOriginal())
    ;
    
    fromF("netty4:tcp://0.0.0.0:%s?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder", properties.getIngest().getMllp().getPort())
      .convertBodyTo(String.class)
      .log(LoggingLevel.DEBUG, log, "HL7 request: [${body}]")
      .setHeader("OriginalHl7Message").body()
      .unmarshal().hl7(true)
      .enrich("direct:processHl7V2", AggregationStrategies.useOriginal())
      .transform(HL7.ack())
    ;
    
    from("servlet:hl7v2?httpMethodRestrict=POST")
      .convertBodyTo(String.class)
      .log(LoggingLevel.DEBUG, log, "HL7 request: [${body}]")
      .setHeader("OriginalHl7Message").body()
      .transform(HL7.convertLFToCR())
      .unmarshal().hl7(true)
      .enrich("direct:processHl7V2", AggregationStrategies.useOriginal())
      .transform(HL7.ack())
    ;
    
    from("direct:processHl7V2")
      .onException(Exception.class)
        .handled(true)
        .to("direct:correctData")
      .end()
      .choice()
        .when().simple("'${header.CamelHL7MessageType}_${header.CamelHL7TriggerEvent}' == 'ADT_A01'")
          .log(LoggingLevel.DEBUG, log, "Processing patient create message")
          .to("direct:processPatientCreate")
        .otherwise()
          .log(LoggingLevel.DEBUG, log, "Unknown message type: [${header.CamelHL7MessageType}_${header.CamelHL7TriggerEvent}]")
          .throwException(IllegalArgumentException.class, "Unknown message type: [${header.CamelHL7MessageType}_${header.CamelHL7TriggerEvent}]")
      .end()
    ;
    
    from("direct:processPatientCreate")
      .transform().groovy("resource:classpath:/groovy/ADT_A01__PatientCreate.groovy")
      .marshal().fhirJson("DSTU3")
      .log(LoggingLevel.DEBUG, log, "FHIR request: [${body}]")
      .to("fhir://create/resource?inBody=resourceAsString&fhirVersion=DSTU3")
      .setBody().simple("${body.getOperationOutcome()}")
      .marshal().fhirJson("DSTU3")
      .log(LoggingLevel.DEBUG, log, "FHIR response: [${body}]")
    ;
    
    from("direct:correctData")
      .onCompletion()
        .useOriginalBody()
        .to("direct:sendEmail")
      .end()
      .log(LoggingLevel.DEBUG, log, "Sending data for manual correction: [${header.OriginalHl7Message}]")
      .setHeader("CamelHttpMethod").constant("POST")
      .setHeader("Accept").constant("application/json")
      .setHeader("Content-Type").constant("application/json")
      .setHeader("Authorization").groovy(String.format("'Basic '+('%s:%s').bytes.encodeBase64()", properties.getDataCorrection().getUsername(), properties.getDataCorrection().getPassword()))
      .transform().groovy(String.format("[ 'callbackURL': '%s', 'message': request.headers['OriginalHl7Message'] ]", properties.getDataCorrection().getCallbackUrl()))
      .marshal().json(JsonLibrary.Jackson)
      .removeHeader("OriginalHl7Message")
      .toF("netty4-http:%s://%s:%s/%s", properties.getDataCorrection().getPrefix(), properties.getDataCorrection().getHost(), properties.getDataCorrection().getPort(), properties.getDataCorrection().getPath())
    ;
    
    from("direct:sendEmail")
      .setHeader("From").simple("dataingest@hca.org")
      .setHeader("Subject").simple("Data Ingest Error")
      .enrich("direct:lookupContact", AggregationStrategies.flexible().pick(new SimpleExpression("${body}")).storeInHeader("To"))
      .transform().groovy("resource:classpath:/groovy/NotificationEmail.groovy")
      .process((Exchange exchange) -> {
        exchange.getIn().addAttachment("hl7v2_message.txt", new DataHandler(exchange.getIn().getHeader("OriginalHl7Message"), "text/plain"));
      })
      .log(LoggingLevel.DEBUG, log, "Sending notification email to [${header.To}] for message [${header.OriginalHl7Message}]")
      .toF("%s://%s@%s:%s?password=RAW(%s)", properties.getNotification().getPrefix(), properties.getNotification().getUsername(), properties.getNotification().getHost(), properties.getNotification().getPort(), properties.getNotification().getPassword())
    ;
    
    from("direct:lookupContact")
      .log(LoggingLevel.DEBUG, log, "Finding contact for [${header.CamelHL7SendingFacility}]")
      .to("sql:select EMAIL from CONTACTS where SENDING_FACILITY=:#${header.CamelHL7SendingFacility}?dataSource=#dataSource&outputType=SelectOne")
    ;
  }
}

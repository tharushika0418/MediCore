package com.medicore.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.medicore.patient.events.PatientEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes patient events from the "patient" Kafka topic.
 * <p>
 * Uses its own consumer group ("analytics-service"), so it receives every event
 * independently of other consumers such as notification-service.
 */
@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);

            // TODO: business logic (e.g. update analytics, store metrics)
            log.info("Received PatientEvent: patientId={}, eventType={}",
                    patientEvent.getPatientId(),
                    patientEvent.getEventType());

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing PatientEvent ({} bytes)", event.length, e);
        }
    }
}

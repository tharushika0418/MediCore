package com.medicore.patientservice.kafka;

import com.medicore.patient.events.PatientEvent;
import com.medicore.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes patient events to the "patient" Kafka topic.
 * <p>
 * Events are serialised with Protobuf ({@link PatientEvent}) and keyed by patient id,
 * so all events for one patient go to the same partition and stay in order.
 */
@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    private static final String TOPIC = "patient";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPatientCreatedEvent(Patient patient) {
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        kafkaTemplate.send(TOPIC, event.getPatientId(), event.toByteArray())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send PatientCreated event for patientId={}",
                                event.getPatientId(), ex);
                    } else {
                        log.info("Sent PatientCreated event for patientId={} to {}-{}@{}",
                                event.getPatientId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

package com.bank.transfer_service.events;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
public class TransferEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventProducer.class);

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    public TransferEventProducer(KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTransferCompleted(TransferCompletedEvent event) {
        kafkaTemplate.send(
                "transfer-events",
                event.transferId(),
                event
        );
    }
}

package com.gururaj.jobservice.service;

import com.gururaj.jobservice.entity.OutboxEvent;
import com.gururaj.jobservice.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private static final String JOB_TOPIC = "job-events";
    private static final String DEAD_LETTER_TOPIC = "job-dead-letter";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        var events =
                outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {

            try {

                String topic =
                        event.getEventType().equals("JOB_DEAD")
                                ? DEAD_LETTER_TOPIC
                                : JOB_TOPIC;

                var result = kafkaTemplate
                        .send(
                                topic,
                                event.getAggregateId().toString(),
                                event.getPayload()
                        )
                        .get();

                System.out.println(
                        "Published job=" + event.getAggregateId()
                                + " partition=" + result.getRecordMetadata().partition()
                                + " offset=" + result.getRecordMetadata().offset()
                );

                event.setPublished(true);
                outboxEventRepository.save(event);

                System.out.println(
                        "Published event: " + event.getId()
                );

            } catch (Exception e) {

                System.err.println(
                        "Failed to publish event: "
                                + event.getId()
                );
            }
        }
    }
}
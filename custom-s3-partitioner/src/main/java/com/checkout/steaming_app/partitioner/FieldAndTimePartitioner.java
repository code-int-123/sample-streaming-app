package com.checkout.steaming_app.partitioner;

import io.confluent.connect.storage.partitioner.TimeBasedPartitioner;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.data.Struct;

import java.util.Map;

/**
 * A partitioner that combines field-based partitioning with time-based partitioning.
 * Produces paths like: year=2025/month=01/day=01/hour=10/minute=00/postcode=SW1
 */
public class FieldAndTimePartitioner extends TimeBasedPartitioner<String> {

    private String fieldName;

    @Override
    public void configure(Map<String, Object> config) {
        fieldName = (String) config.get("partition.field.name");
        super.configure(config);
    }

    @Override
    public String encodePartition(SinkRecord sinkRecord) {
        String fieldValue = extractFieldValue(sinkRecord);
        String timePath = super.encodePartition(sinkRecord);
        return timePath + "/" + fieldName + "=" + fieldValue;
    }

    private String extractFieldValue(SinkRecord sinkRecord) {
        Object value = sinkRecord.value();
        if (value instanceof Struct struct) {
            return struct.get(fieldName).toString();
        }
        if (value instanceof Map<?, ?> map) {
            Object fieldVal = map.get(fieldName);
            if (fieldVal != null) {
                return fieldVal.toString();
            }
        }
        throw new IllegalArgumentException(
                "Cannot extract field '" + fieldName + "' from record value of type " +
                        (value == null ? "null" : value.getClass().getName()));
    }
}

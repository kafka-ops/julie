CREATE OR REPLACE  STREAM ordersStream (
    transactionId BIGINT KEY,
    transactionMainDate BIGINT KEY,
    number BIGINT,
    date BIGINT,
    state VARCHAR
) WITH (
    KAFKA_TOPIC = 'more-orders-avro',
    KEY_FORMAT = 'JSON',
    VALUE_FORMAT = 'JSON',
    PARTITIONS=1,
    REPLICAS=1
);
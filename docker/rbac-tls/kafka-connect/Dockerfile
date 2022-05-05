FROM confluentinc/cp-server-connect:6.2.0

ENV CONNECT_PLUGIN_PATH="/usr/share/java,/usr/share/confluent-hub-components"

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.5.0 \
    && confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:10.2.1 \
    && confluent-hub install --no-prompt debezium/debezium-connector-sqlserver:1.6.0 \
    && confluent-hub install --no-prompt confluentinc/kafka-connect-ibmmq:11.0.8

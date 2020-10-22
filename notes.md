 docker exec kafka kafka-acls --bootstrap-server localhost:9092 --command-config /etc/kafka/kafka-user.properties --list --principal User:bob
 
 docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list --command-config /etc/kafka/kafka-user.properties

docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 \
    --consumer.config /etc/kafka/bob.properties --topic context.projectA.foo \
    --from-beginning --group "foo" 
     

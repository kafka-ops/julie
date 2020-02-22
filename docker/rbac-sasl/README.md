# Confluent RBAC

## Predefined Roles

https://docs.confluent.io/current/security/rbac/rbac-predefined-roles.html#rbac-predefined-roles

*Description*:

* _super.user_: The purpose of super.user is to have a bootstrap user who can initially grant another user the SystemAdmin role.
* _SystemAdmin_: Provides full access to all scoped resources in the cluster (KSQL cluster, Kafka cluster, or Schema Registry cluster).
* _ClusterAdmin_: Sets up clusters (KSQL cluster, Kafka cluster, or Schema Registry cluster).
* _UserAdmin_: Manages role bindings for users and groups in all clusters managed by MDS.
* _SecurityAdmin_: Enables management of platform-wide security initiatives.
* _Operator_: Provides operational management of clusters and scale applications as needed.
* _ResourceOwner_: Transfers the ownership of critical resources and to scale the ability to manage authorizations for those resources.
* _DeveloperRead, DeveloperWrite, DeveloperManage_: Allows developers to drive the implementation of applications they are working on and manage the content within, especially in development, test, and staging environments.


*Examples*:

|  Predefined Role |  Plan |
|---|---|
| super.user  |  Sam is granted full access to all project resources and operations. He will create the initial set of roles for the project. |
| ResourceOwner  | 	Ryan will own all topics with the prefix finance_. He can grant others permission to access and use this resource. In this use case, he is the ResourceOwner for the finance topics.  |
| UserAdmin  | 	Uri will manage the users and groups for the project.  |
| Operator  | 	Olivia will be responsible for the operational and health management of the platform and applications. |
| ClusterAdmin  | 	Cindy is a member of the Kafka cluster central team.  |
| DeveloperRead, DeveloperWrite, DeveloperManage  | 	David will be responsible for developing and managing the application.  |

## Interesting commands

confluent iam role describe ResourceOwner

confluent iam role list

confluent iam rolebinding [command]

Available Commands:
 create      Create a role binding.
 delete      Delete an existing role binding.
 list        List role bindings.


*Get Kafka cluster ID*
 docker-compose exec broker zookeeper-shell zookeeper:2181  get /cluster/id  


### using  CLI tools

docker-compose exec broker kafka-topics --bootstrap-server broker:9092 --list --command-config /etc/client-configs/professor.properties

```bash
docker-compose exec broker kafka-topics --bootstrap-server broker:9092 --create --topic foo --partitions 1 --replication-factor 1   --command-config /etc/client-configs/fry.properties

Error while executing topic command : org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [Authorization failed.]
[2019-08-20 14:29:21,562] ERROR java.util.concurrent.ExecutionException: org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [Authorization failed.]
	at org.apache.kafka.common.internals.KafkaFutureImpl.wrapAndThrow(KafkaFutureImpl.java:45)
	at org.apache.kafka.common.internals.KafkaFutureImpl.access$000(KafkaFutureImpl.java:32)
	at org.apache.kafka.common.internals.KafkaFutureImpl$SingleWaiter.await(KafkaFutureImpl.java:89)
	at org.apache.kafka.common.internals.KafkaFutureImpl.get(KafkaFutureImpl.java:260)
	at kafka.admin.TopicCommand$AdminClientTopicService.createTopic(TopicCommand.scala:190)
	at kafka.admin.TopicCommand$TopicService.createTopic(TopicCommand.scala:149)
	at kafka.admin.TopicCommand$TopicService.createTopic$(TopicCommand.scala:144)
	at kafka.admin.TopicCommand$AdminClientTopicService.createTopic(TopicCommand.scala:172)
	at kafka.admin.TopicCommand$.main(TopicCommand.scala:60)
	at kafka.admin.TopicCommand.main(TopicCommand.scala)
Caused by: org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [Authorization failed.]
 (kafka.admin.TopicCommand$)
 ```

docker-compose exec broker kafka-console-producer --broker-list broker:9092  --topic source-topic --producer.config /etc/client-configs/professor.properties
docker-compose exec broker kafka-console-consumer --bootstrap-server broker:9092  --topic target-topic --from-beginning --property print.key=true --consumer.config  /etc/client-configs/professor.properties

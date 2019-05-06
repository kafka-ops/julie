
trait ACLCommand {

  def build:List[String]
}

abstract class AclAppCommand(users: Array[String]) extends ACLCommand {
  def isEmpty: Boolean = users.isEmpty

  override def toString : String = build.mkString("\n")
}
case class ConsumerACLCommand(users: Array[String], topics: Array[String], zookeepers: Array[String]) extends AclAppCommand(users) {

  // bin/kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 \
  //  --add --allow-principal User:Bob \
  //  --consumer --topic test-topic --group Group-1

  override def build: List[String] = {
    if (users.isEmpty || topics.isEmpty)
      return List.empty

    val basicACL = s"kafka-acls --authorizer-properties zookeeper.connect=${zookeepers.mkString(",")} --add --group '*' --consumer"
    val allowedPrincipals: String = users.map(user => s"--allow-principal User:$user").mkString(" ")
    val allowedTopics: String = topics.map(topic => s"--topic $topic").mkString(" ")
    List(s"$basicACL $allowedPrincipals $allowedTopics")
  }

}

case class ProducerACLCommand(users: Array[String], topics: Array[String], zookeepers: Array[String]) extends AclAppCommand(users) {

  // bin/kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 \
  //  --add --allow-principal User:Bob \
  //  --producer --topic test-topic --group Group-1

  override def build: List[String] = {
    if (users.isEmpty || topics.isEmpty)
      return List.empty

    val basicACL = s"kafka-acls --authorizer-properties zookeeper.connect=${zookeepers.mkString(",")} --add --group '*' --producer"
    val allowedPrincipals: String = users.map(user => s"--allow-principal User:$user").mkString(" ")
    val allowedTopics: String = topics.map(topic => s"--topic $topic").mkString(" ")
    List(s"$basicACL $allowedPrincipals $allowedTopics")
  }


}

case class ConnectACLCommand(users: Array[String], topics: Array[String], zookeepers: Array[String]) extends AclAppCommand(users) {

  /*
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Create --cluster
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Read --group *
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Read --topic connect-status
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Read --topic connect-offsets
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Read --topic connect-configs
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Write --topic connect-status
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Write --topic connect-offsets
   ./bin/kafka-acls --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:Bob --operation Write --topic connect-configs
*/


  override def build: List[String] = {
    if (users.isEmpty)
      return List.empty

    val basicACL = s"kafka-acls  --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=${zookeepers.mkString(",")} --add"
    val allowedPrincipals: String = users.map(user => s"--allow-principal User:$user").mkString(" ")

    val internalTopics = Array("connect-status", "connect-offsets", "connect-configs" ).map(topic => s"--topic $topic").mkString(" ")

    List(
      s"$basicACL $allowedPrincipals --operation Create --cluster",
      s"$basicACL $allowedPrincipals --operation Read --group *",
      s"$basicACL $allowedPrincipals --operation Read $internalTopics",
      s"$basicACL $allowedPrincipals --operation Write $internalTopics"
    )
  }
}

case class KafkaStreamsACLCommand(users: Array[KStreamApp], group: String, projectName: String, zookeepers: Array[String]) extends AclAppCommand(users.map(_.name)) {

  // # Allow Streams to read the input topics:
  //kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation Read --topic source-topic
  //
  //# Allow Streams to write to the output topics:
  //kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation Write --topic target-topic
  //
  //# Allow Streams to manage its own internal topics and consumer groups:
  //kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation All --resource-pattern-type prefixed --topic porsche-streams-app --group porsche-streams-app

  override def build: List[String] = {
    if (users.isEmpty)
      return List.empty

    val basicACL = s"kafka-acls --authorizer-properties zookeeper.connect=${zookeepers.mkString(",")} --add --group '*'"

    /**
      * TODO: This operation could be optimised by grouping per topic and operation, so if User2 and User3
      * need write access to topic baz, we generate one single rule and not two (one per each user)
      */
    val singleTopics: Array[String] = users.flatMap { user =>
      val allowedReadTopics: String = user.topics.read.map(topic => s"--topic $topic").mkString(" ")
      val allowedWriteTopics: String = user.topics.write.map(topic => s"--topic $topic").mkString(" ")

      List(s"$basicACL --allow-principal User:${user.name} --operation Read $allowedReadTopics",
           s"$basicACL --allow-principal User:${user.name} --operation Write $allowedWriteTopics")
    }

    val internalTopics: Array[String] = users.map { user =>
      val pattern = s"--resource-pattern-type prefixed --topic $group.$projectName --group $group.$projectName"
      s"$basicACL --allow-principal User:${user.name} --operation All $pattern"
    }

    singleTopics ++ internalTopics toList
  }

}

class ACL(consumers: Array[String],
          producers: Array[String],
          connectors: Array[String],
          streams: Array[KStreamApp]) {

  def build(group: String, projectName: String, topics: Array[String], zookeepers: Array[String]) : List[ACLCommand] = {

    List(
      ConsumerACLCommand(consumers, topics, zookeepers),
      ProducerACLCommand(producers, topics,  zookeepers),
      KafkaStreamsACLCommand(streams, group, projectName, zookeepers),
      ConnectACLCommand(connectors, Array.empty, zookeepers)
    ).filterNot(_.isEmpty)
  }
}

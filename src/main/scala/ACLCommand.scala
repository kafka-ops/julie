
trait ACLCommand {

  def buildAsString:List[String]
  def build:List[ACLAction]
}

abstract class AclAppCommand(users: Array[String]) extends ACLCommand {
  def isEmpty: Boolean = users.isEmpty

  override def toString : String = {
    if (buildAsString.isEmpty) {
      build.mkString("\n")
    } else {
      buildAsString.mkString("\n")
    }
  }


  override def buildAsString: List[String] = build.map(_.toString)

  override def build: List[ACLAction] = List.empty[ACLAction]
}
case class ConsumerACLCommand(users: Array[String], topics: Array[String], zookeepers: Array[String]) extends AclAppCommand(users) {

  override def build: List[ACLAction] = {
    if (users.isEmpty || topics.isEmpty)
      return List.empty

    List(
      new ACLAction(zookeeperHosts = zookeepers, action = "add", principals = users, topics = topics, role = Some(ConsumerRole))
    )
  }

}

case class ProducerACLCommand(users: Array[String], topics: Array[String], zookeepers: Array[String]) extends AclAppCommand(users) {

  override def build: List[ACLAction] = {
    if (users.isEmpty || topics.isEmpty)
      return List.empty

    List(
      new ACLAction(zookeeperHosts = zookeepers, action = "add", principals = users, topics = topics, role = Some(ProducerRole))
    )

  }


}

case class ConnectACLCommand(users: Array[String], zookeepers: Array[String]) extends AclAppCommand(users) {

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


  override def build: List[ACLAction] = {
    if (users.isEmpty)
      return List.empty

    val internalTopics = Array("connect-status", "connect-offsets", "connect-configs" )

    val aclAction = new ACLAction(zookeeperHosts = zookeepers, action = "add", principals = users)

    List(
      aclAction.copy(operation = Some("Create"), role=Some(ClusterRole)),
      aclAction.copy(operation = Some("Read")),
      aclAction.copy(operation = Some("Read"), topics = internalTopics),
      aclAction.copy(operation = Some("Write"), topics = internalTopics)
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

  override def buildAsString: List[String] = {
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
      ConnectACLCommand(connectors, zookeepers)
    ).filterNot(_.isEmpty)
  }
}

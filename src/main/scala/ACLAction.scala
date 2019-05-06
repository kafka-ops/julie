

sealed trait AclRole
case object ProducerRole extends AclRole
case object ConsumerRole extends AclRole
case object ClusterRole extends AclRole

case class ACLAction(zookeeperHosts: Array[String], action: String,
                principals: Array[String], topics: Array[String]=Array.empty[String],
                group:String="*", operation: Option[String]=None, role: Option[AclRole]=None) {

  // bin/kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 \
  //  --add --allow-principal User:Bob \
  //  --consumer --topic test-topic --group Group-1

  override def toString: String = {

    val basicACL = s"kafka-acls --authorizer-properties zookeeper.connect=${zookeeperHosts.mkString(",")} --$action --group '$group'"

    val roleString = role match {
      case Some(role) => {
        if ( role.getClass.isInstance(ProducerRole)) {
           " --producer "
        } else {
          " --consumer "
        }
      }
      case None => ""
    }

    val allowedPrincipals: String = principals.map(user => s"--allow-principal User:$user").mkString(" ")
    val allowedTopics: String = topics.map(topic => s"--topic $topic").mkString(" ")

    s"$basicACL $allowedPrincipals $allowedTopics $roleString"
  }

}

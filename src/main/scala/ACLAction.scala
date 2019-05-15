

sealed trait AclRole
case object ProducerRole extends AclRole
case object ConsumerRole extends AclRole
case object ClusterRole extends AclRole

trait ACLAction {
  def action: String
  def zookeeperHosts: Array[String]
  def principals: Array[String]
  def topics: Array[String]
  def group: String
  def operation: Option[String]
  def role: Option[AclRole]
}

abstract class AbstractACLAction(
                          val zookeeperHosts: Array[String],
                          val action: String,
                          val principals: Array[String],
                          val topics: Array[String],
                          val group:String,
                          val operation: Option[String],
                          val role: Option[AclRole]
                        ) extends ACLAction {

}

case class BasicACLAction(
                           zookeeperHosts: Array[String],
                           action: String,
                           principals: Array[String],
                           topics: Array[String]=Array.empty[String],
                           group:String="*",
                           operation: Option[String]=None,
                           role: Option[AclRole]=None
                         ) extends ACLAction {

    def asPrefixedACL: PrefixedACLAction = PrefixedACLAction(zookeeperHosts, action, principals, topics, group)


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

      val operationString = operation match {
        case Some(op) => s"--operation $op"
        case None => ""
      }

      val allowedPrincipals: String = principals.map(user => s"--allow-principal User:$user").mkString(" ")
      val allowedTopics: String = topics.map(topic => s"--topic $topic").mkString(" ")

      s"$basicACL $allowedPrincipals $allowedTopics $roleString $operationString"
    }
}

case class PrefixedACLAction(
                              zookeeperHosts: Array[String], action: String,
                              principals: Array[String],topics: Array[String]=Array.empty[String],
                              group:String="*"
                            )  extends ACLAction {

  override def toString: String = {
    val basicACL = s"kafka-acls --authorizer-properties zookeeper.connect=${zookeeperHosts.mkString(",")} --$action --group '$group'"

    val allowedPrincipals: String = principals.map(user => s"--allow-principal User:$user").mkString(" ")
    val allowedTopics: String = topics.map(topic => s"--topic $topic").mkString(" ")


    val operationString = "--operation All"

    s"$basicACL --resource-pattern-type prefixed $operationString $allowedPrincipals $allowedTopics"
  }

  override def role: Option[AclRole] = None

  override def operation: Option[String] = Some("All")
}
case class Projects(projects: Array[Project]);

case class Project(name: String, users: Accounts, topics: Array[ProjectTopic]=Array.empty, zookeepers: Array[String]=Array("localhost:2181"));

case class Accounts(consumers: Array[String], producers: Array[String], streams: Array[KStreamApp], connectors: Array[String] )

case class KStreamApp(name: String, topics: KStreamTopics)
case class KStreamTopics(read: Array[String], write: Array[String])
case class ProjectTopic(name: String, config: Map[String, Any])

object Topology {

  def dummy: Projects = {

    val monitoringUsers = Accounts(
      consumers = Array("app2"),
      producers = Array.empty,
      streams = Array.empty,
      connectors = Array("conn1")
    )

    val monitoringTopics = Array(
      ProjectTopic(
        name = "baz",
        config = Map("partitions" -> 2)
      )
    )

    val monitoringProject = Project(
      name = "monitoring",
      users = monitoringUsers,
      topics = monitoringTopics
    )

    val streamsUsers = Array(
      KStreamApp(
        name = "app2",
        topics = KStreamTopics( read = Array("foo"), write = Array("bar", "baz"))
      ),
      KStreamApp(
        name = "app3",
        topics = KStreamTopics( read = Array("bar"), write = Array("foo", "baz"))
      )
    )

    val dataLakeUsers = Accounts(
      consumers = Array("app0", "app1"),
      producers = Array("app0", "app3"),
      streams = streamsUsers,
      connectors = Array.empty
    )

    val dataLakeTopics = Array(
      ProjectTopic(
        name = "foo",
        config = Map("partitions" -> 1, "retention.ms" -> 100)
      ),
      ProjectTopic(
        name = "bar",
        config = Map("partitions" -> 1)
      )
    )

    val dataLakeProject = Project(
      name = "data-lake",
      users = dataLakeUsers,
      topics = dataLakeTopics)

    Projects(Array(dataLakeProject, monitoringProject))
  }
}
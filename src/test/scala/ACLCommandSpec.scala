import org.scalatest.FunSpec
import org.scalatest.Matchers._

class ACLCommandSpec extends FunSpec {

  val defaultZookeeper = Array("localhost:2181")

  describe("Consumer ACL")
  {
    it("always generate one request, with the default right values")  {

      val users = Array("user0", "user1")
      val topics = Array("topic0", "topic1")

      val cmd = ConsumerACLCommand(users, topics, defaultZookeeper);
      val listOfActions = cmd.build

      assert(listOfActions.size == 1)

      listOfActions.foreach { action =>
        assert( action.action == "add" )
        action.principals shouldBe users
        action.topics shouldBe topics
        action.zookeeperHosts shouldBe defaultZookeeper
      }
    }

    describe("when params are empty") {
      it("if no users passed, should return an empty list") {

        val users = Array.empty[String]
        val topics = Array("topic0", "topic1")

        val cmd = ConsumerACLCommand(users, topics, defaultZookeeper);
        assert(cmd.build.isEmpty)
      }

      it("if no topics passed, should return an empty list") {

        val users = Array("user0", "user1")
        val topics = Array.empty[String]

        val cmd = ConsumerACLCommand(users, topics, defaultZookeeper);
        assert(cmd.build.isEmpty)
      }
    }
  }

  describe("Producer ACL") {
    it ("always generate one request") {

      val users = Array("user0", "user1")
      val topics = Array("topic0", "topic1")

      val cmd = ProducerACLCommand(users, topics, defaultZookeeper);
      assert(cmd.build.size == 1)
    }

    describe("when params are empty") {
      it("if no users passed, should return an empty list") {

        val users = Array.empty[String]
        val topics = Array("topic0", "topic1")

        val cmd = ProducerACLCommand(users, topics, defaultZookeeper);
        assert(cmd.build.isEmpty)
      }

      it("if no topics passed, should return an empty list") {

        val users = Array("user0", "user1")
        val topics = Array.empty[String]

        val cmd = ProducerACLCommand(users, topics, defaultZookeeper);
        assert(cmd.build.isEmpty)
      }
    }
  }

  describe("Connect ACLs") {

    it("should generate enought internal and external acls") {

      val users = Array("user0", "user1")

      val cmd = ConnectACLCommand(users = users, zookeepers = defaultZookeeper)

      val actions = cmd.build

      actions should have length 4

      // should have only two actions with --topics, this are the ones for the internal topics.
      actions.filterNot(_.topics.isEmpty) should have length 2
      // should have one acl action with --cluster scope
      actions.filter(_.role.isDefined) should have length 1
      actions.filter(_.role.isDefined).foreach { action =>
        action.role.get shouldBe ClusterRole
      }
    }
  }

  describe("Kafka Streams ACLs") {

    it("should generate enought internal and external acls") {

      val users = Array(
        KStreamApp(name = "app0", topics = KStreamTopics(read = Array("topic0"), write = Array("topic1") ))
      )

      val cmd = KafkaStreamsACLCommand(users = users, group = "foo", projectName = "bar", zookeepers = defaultZookeeper)

      val actions = cmd.build

      actions should have length 3

      actions.filter(_.isInstanceOf[BasicACLAction]) should have length 2
      actions.filter(_.isInstanceOf[PrefixedACLAction]) should have length 1
    }
  }
}
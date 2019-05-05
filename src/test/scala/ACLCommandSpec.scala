import org.scalatest.FunSpec

class ACLCommandSpec extends FunSpec {

  val defaultZookeeper = Array("localhost:2181")

  describe("Consumer ACL")
  {
    it("always generate one request")  {

      val users = Array("user0", "user1")
      val topics = Array("topic0", "topic1")

      val cmd = ConsumerACLCommand(users, topics, defaultZookeeper);
      assert(cmd.build.size == 1)
    }

    it("return empty if no users passed")  {

      val users = Array.empty[String]
      val topics = Array("topic0", "topic1")

      val cmd = ConsumerACLCommand(users, topics, defaultZookeeper);
      assert(cmd.build.isEmpty)
    }

    it("return empty if no topics passed")  {

      val users = Array("user0", "user1")
      val topics = Array.empty[String]

      val cmd = ConsumerACLCommand(users, topics, defaultZookeeper);
      assert(cmd.build.isEmpty)
    }

  }

  describe("Producer ACL") {
    it ("always generate one request") {

      val users = Array("user0", "user1")
      val topics = Array("topic0", "topic1")

      val cmd = ProducerACLCommand(users, topics, defaultZookeeper);
      assert(cmd.build.size == 1)
    }
  }
}
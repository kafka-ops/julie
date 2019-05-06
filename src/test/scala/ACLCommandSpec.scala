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
}
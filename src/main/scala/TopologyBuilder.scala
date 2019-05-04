import java.io.FileReader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class TopologyBuilder {

}

object TopologyBuilder {

  val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())
  mapper.registerModule(DefaultScalaModule)

  def parse(file: String): Projects = {
    mapper.readValue(new FileReader(file), classOf[Projects]);
  }

  def main(args: Array[String]): Unit = {

    val cmd = new PrepareCommand()
    val projects = parse("descriptor.yaml")

    projects.projects.map(println)
  }
}

import java.io.FileReader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.sellmerfud.optparse.OptionParser

class TopologyBuilder {

}

case class Config(descriptorFile: String = "")

object TopologyBuilder {

  val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())
  mapper.registerModule(DefaultScalaModule)

  val cli = new OptionParser[Config] {
    reqd[String]("-f", "--file STRING", "filename") { (value, cfg) => cfg.copy(descriptorFile = value) }
  }


  def parse(file: String): Projects = {
    mapper.readValue(new FileReader(file), classOf[Projects]);
  }

  def main(args: Array[String]): Unit = {

    val config: Config = cli.parse(args, Config())
    val cmd = new PrepareCommand()
    val projects = parse(config.descriptorFile)
    cmd.execute(projects)
  }
}

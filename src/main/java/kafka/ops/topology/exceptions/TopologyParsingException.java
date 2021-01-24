package kafka.ops.topology.exceptions;

public class TopologyParsingException extends RuntimeException {
  public TopologyParsingException(String msg, Throwable e) {
    super(msg, e);
  }
}

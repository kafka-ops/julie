package com.purbon.kafka.topology.audit;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StdoutAppender implements Appender {

  private static final Logger LOGGER = LogManager.getLogger(StdoutAppender.class);

  @Override
  public void log(String msg) {
    try {
      flush(msg, System.out);
    } catch (IOException e) {
      LOGGER.error(e);
    }
  }

  protected void flush(String msg, OutputStream os) throws IOException {
    os.write(msg.getBytes(StandardCharsets.UTF_8));
  }
}

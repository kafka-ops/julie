package com.purbon.kafka.topology;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class OnceOnlyWarningLogger {

  private static final OnceOnlyWarningLogger INSTANCE = new OnceOnlyWarningLogger();
  private static final Logger LOGGER = LogManager.getLogger(OnceOnlyWarningLogger.class);
  private boolean remoteStateVerificationDisabledWarningShown = false;

  private OnceOnlyWarningLogger() {}

  public static OnceOnlyWarningLogger getInstance() {
    return INSTANCE;
  }

  public void logRemoteStateVerificationDisabledWarning() {
    if (remoteStateVerificationDisabledWarningShown) {
      return;
    }
    LOGGER.warn(
        "Remote state verification is disabled. This is not a good practice, "
            + "and in future versions this check may become mandatory.");
    remoteStateVerificationDisabledWarningShown = true;
  }
}

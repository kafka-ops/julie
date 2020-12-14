package com.purbon.kafka.topology.api.ccloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purbon.kafka.topology.model.cluster.Cluster;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudCLI {

  private static final Logger LOGGER = LogManager.getLogger(CCloudCLI.class);

  private ObjectMapper mapper;

  public CCloudCLI() {
    mapper = new ObjectMapper();
  }

  public List<Cluster> clusters() throws IOException {
    List<String> cmd = Arrays.asList("ccloud", "kafka", "cluster", "list", "--output", "json");
    String stdout = "";
    try {
      stdout = run(cmd);
      Cluster[] items = mapper.readValue(stdout, Cluster[].class);
      return Arrays.asList(items);
    } catch (IOException | InterruptedException e) {
      handleError(stdout, e);
    }
    return null;
  }

  public Map<String, ServiceAccount> serviceAccounts() throws IOException {
    List<String> cmd = Arrays.asList("ccloud", "service-account", "list", "--output", "json");
    String stdout = "";
    try {
      stdout = run(cmd);
      ServiceAccount[] items = mapper.readValue(stdout, ServiceAccount[].class);
      return Arrays.asList(items).stream()
          .collect(Collectors.toMap(ServiceAccount::getName, i -> i));
    } catch (IOException | InterruptedException e) {
      handleError(stdout, e);
      return null;
    }
  }

  public void setEnvironment(String environment) throws IOException {
    List<String> cmd = Arrays.asList("ccloud", "environment", "use", environment);
    String stdout = "";
    try {
      stdout = run(cmd);
    } catch (IOException | InterruptedException e) {
      handleError(stdout, e);
    }
  }

  public ServiceAccount newServiceAccount(String name, String description) throws IOException {
    List<String> cmd =
        Arrays.asList(
            "ccloud",
            "service-account",
            "create",
            name,
            "--description",
            description,
            "--output",
            "json");
    String stdout = "";
    ServiceAccount sa = null;
    try {
      stdout = run(cmd);
      sa = mapper.readValue(stdout, ServiceAccount.class);
    } catch (IOException | InterruptedException e) {
      handleError(stdout, e);
    }
    return sa;
  }

  public void deleteServiceAccount(int id) throws IOException {
    List<String> cmd = Arrays.asList("ccloud", "service-account", "delete", String.valueOf(id));
    String stdout = "";
    try {
      stdout = run(cmd);
    } catch (IOException | InterruptedException e) {
      handleError(stdout, e);
    }
  }

  private void handleError(String stdout, Exception e) throws IOException {
    String errorMsg = String.format("Something happen with ccloud. \n %s", stdout);
    LOGGER.error(errorMsg, e);
    throw new IOException(e);
  }

  private String run(List<String> cmd) throws IOException, InterruptedException {

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(cmd);
    builder.redirectErrorStream(true);

    Process pr = builder.start();
    String stdout = readStdOut(pr);
    pr.waitFor();

    LOGGER.debug("Exit code: " + pr.exitValue());
    return stdout;
  }

  private String readStdOut(Process pr) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = br.readLine()) != null) {
      sb.append(line);
      sb.append("\n");
    }
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {

    CCloudCLI cli = new CCloudCLI();
    cli.setEnvironment("env-j9wgp");
  }
}

# The Kafka Topology builder tool

<a href="https://codeclimate.com/github/purbon/kafka-topology-builder/maintainability"><img src="https://api.codeclimate.com/v1/badges/ef4bcda7d1b5fd0a4f1e/maintainability" /></a> ![CI tests](https://github.com/kafka-ops/kafka-topology-builder/workflows/CI%20tests/badge.svg?branch=master) [![Gitter](https://badges.gitter.im/kafka-topology-builder/community.svg)](https://gitter.im/kafka-topology-builder/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) [![Documentation Status](https://readthedocs.org/projects/kafka-topology-builder/badge/?version=latest)](https://kafka-topology-builder.readthedocs.io/?badge=latest)

This tool helps you build proper ACLs for Apache Kafka. The Kafka ACL builder tool knows what do you
need for each of the products/projects you are planning, either Kafka Connect, Kafka Streams or others.

## The motivation 

One of the typical questions while building an Apache Kafka infrastructure is how to handle topics, 
configurations and the required permissions to use them (Access Control List).
The Kafka Topology Builder, in close collaboration with GIT and Jenkins (CI/CD) is here to help you setup an organised and automated way of managing your Kafka Cluster.

## Where's the docs?

We recommend taking time to [read the docs](https://kafka-topology-builder.readthedocs.io/).
There's quite a bit of detailed information about gitops, Apache Kafka and how this project can help you automate your basic operations tasks.

## Automating the Topic Management with CI/CD (Jenkins) and gitops

![KafkaTopologyBuilder](imgs/kafka-topology-builder.png)

You might be wondering what is the usual workflow to implement this approach:

**Action**: As a user, part of a developer team (for example), I like to have some changes in Apache Kafka.

**Change Request**: As a user:

- Go to the git repository where the topology is described
- Create a new branch
- Perform the changes need
- Make a pull request targeting master branch

**Approval process**: As an ops admin, I can:

- Review the pull request (change request) initiated by teams
- Request changes when need
- Merge the requests.

Considerations:

* Using webhooks, the git server (github, gitlab or bitbucket) will inform the CI/CD system changes had happened and they need to be applied to the cluster.
* All changes (git push) to master branch are disabled directly. 
Changes only can happen with a pull request.

## Help??

If you are using the Kafka Topology Builder, or plan to use it in your project? might be you have encounter a bug? or a challenge?
need a certain future? feel free to reach out into our [gitter community](https://gitter.im/kafka-topology-builder/community).

[![Gitter](https://badges.gitter.im/kafka-topology-builder/community.svg)](https://gitter.im/kafka-topology-builder/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

## Feature list, not only bugs ;-)

What can you achieve with this tool:

* Support for multiple access control mechanisms:
    * Traditional ACLs
    * Role Bases Access Control as provided by Confluent
* Automatically set access control rules for:
    * Kafka Consumers
    * Kafka Producers
    * Kafka Connect 
    * Kafka Streams applications ( microservices )
    * Schema Registry instances
    * Confluent Control Center
* Manage topic naming with a topic name convention
    * Including the definition of *projects*, *teams*, *datatypes* and for sure the topic name
    * Some of the topics are flexible defined by user requirements
* Allow for creation, delete and update of:
    * topics, following the topic naming convention
    * Topic configuration, variables like retention, segment size, etc
    * Acls, or RBAC rules
* Manage your cluster schemas.
    - Support for Confluent Schema Registry

More details and examples of the functionality can be found in the wiki.  

### How can I run the topology builder directly?

This tool is available in multiple formats:

- As an RPM package, for the RedHat alike distributions
- As a DEB package, for Debian based distros
- Directly as a fat jar (zip/tar.gz)

The latest version are available from the [releases](https://github.com/purbon/kafka-topology-builder/releases) page.

The release is as well available in [docker hub](https://hub.docker.com/r/purbon/kafka-topology-builder).

#### How to execute the tool

This is how you can run the tool directly as a docker image:

```bash 
docker run purbon/kafka-topology-builder:latest kafka-topology-builder.sh  --help
Parsing failed cause of Missing required options: topology, brokers, clientConfig
usage: cli
    --allowDelete          Permits delete operations for topics and
                           configs.
    --brokers <arg>        The Apache Kafka server(s) to connect to.
    --clientConfig <arg>   The AdminClient configuration file.
    --dryRun               Print the execution plan without altering
                           anything.
    --help                 Prints usage information.
    --quiet                Print minimum status update
    --topology <arg>       Topology config file.
    --version              Prints useful version information.
```

If you install the tool as rpm, you will have available in your $PATH the _kafka-topology-builder.sh_.
You can run this script with the same options observed earlier.  

#### An example topology

An example topology should look like this (in yaml format):

```yaml
context: "context"
source: "source"
projects:
- name: "foo"
  consumers:
  - principal: "User:app0"
  - principal: "User:app1"
  streams:
  - principal: "User:App0"
    topics:
      read:
      - "topicA"
      - "topicB"
      write:
      - "topicC"
      - "topicD"
  connectors:
  - principal: "User:Connect1"
    topics:
      read:
      - "topicA"
      - "topicB"
  - principal: "User:Connect2"
    topics:
      write:
      - "topicC"
      - "topicD"
  topics:
  - name: "foo" # topicName: context.source.foo.foo
    config:
      replication.factor: "2"
      num.partitions: "3"
  - name: "bar" # topicName: context.source.foo.bar
    config:
      replication.factor: "2"
      num.partitions: "3"
- name: "bar"
  topics:
  - name: "bar" # topicName: context.source.bar.bar
    config:
      replication.factor: "2"
      num.partitions: "3"
```

more examples can be found at the [example/](example/) directory. 
Also check the documentation on ACL and RBAC in [the docs](https://kafka-topology-builder.readthedocs.io/) for information on what ACLs are created.

## Interested in contributing back?

Interested on contributing back? might be have an idea for a great future? or wanna fix a bug? Check our [contributing](CONTRIBUTING.md) doc for guidance.

## Building the Kafka Topology Builder from scratch (source code)

The project is build using Java and Maven, so both are required if you aim to build the tool from scratch.
The minimum version of Java supported is Java 8, note it soon will be deprecated here, it is only keep as supported for very legacy environments.

It is recommended to run the Kafka Topology Builder with Java 11 and an open JDK version.

### Building a release

If you are interested on building a release artifact from the source code, check our [release](RELEASE.md) doc for guidance.

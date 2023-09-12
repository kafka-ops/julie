# An operational manager for Apache Kafka (Automation, GitOps, SelfService)

## Note - Governance - Hibernation state

I'm gratefuly of how many people the JulieOps project has helped during it existance, it is totally mind blowing to get more than 300 starts for a humble human like me, thanks everyone!!.

Sadly this days, between my workload and personal arrangements, the project has been lacking proper mantainance and care, what honestly makes me very sad as I would love to see it grow and provide more and more people with such features, I'm a big beliver of self service and automation.

So, until new notice, or something change, you should take the project with care, as currently it is mostly on a long winter hibernation :-) I'm sorry for this, but I can't do more as a mostly sole mantainer.

Thanks again to everyone who was, is or will be involved with the project life.

<center>
<img src="https://media.kidadl.com/Do_Pandas_Hibernate_Why_Are_They_More_Active_During_Winter_d5daed1b94.jpg" alt="panda" width="300"/>
 </center>

-- Pere

### README

*NOTE*: This project was formally known as Kafka Topology Builder, old versions of this project can still be found under that name.

<a href="https://codeclimate.com/github/purbon/kafka-topology-builder/maintainability"><img src="https://api.codeclimate.com/v1/badges/ef4bcda7d1b5fd0a4f1e/maintainability" /></a> ![CI tests](https://github.com/kafka-ops/kafka-topology-builder/workflows/CI%20tests/badge.svg?branch=master) [![Gitter](https://badges.gitter.im/kafka-topology-builder/community.svg)](https://gitter.im/kafka-topology-builder/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) [![Documentation Status](https://readthedocs.org/projects/julieops/badge/?version=latest)](https://julieops.readthedocs.io/?badge=latest)

JulieOps helps you automate the management of your things within Apache Kafka, from Topics,
Configuration to Metadata but as well Access Control, Schemas.
More items are plan, check [here](https://github.com/kafka-ops/julie/issues) for details.

## The motivation

One of the typical questions while building an Apache Kafka infrastructure is how to handle topics,
configurations and the required permissions to use them (Access Control List).

The JulieOps cli, in close collaboration with git and Jenkins (CI/CD) is here to help you setup an
organised and automated way of managing your Kafka Cluster.

## Where's the docs?

We recommend taking time to [read the docs](https://julieops.readthedocs.io/en/latest/).
There's quite a bit of detailed information about GitOps, Apache Kafka and how this project can help you automate
the common operational tasks.

## Automating Management with CI/CD and GitOps

![JulieOps](imgs/julie-ops.png)

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

* Using webhooks, the git server (github, gitlab or bitbucket) will inform the CI/CD system changes had happened
and the need to apply them to the cluster.
* All changes (git push) to master branch are disabled directly.
Changes only can happen with a pull request. Providing a Change Management mechanism to fit into your org procedures.

## Help??

If you are using the JulieOps tool, or plan to use it in your project? might be you have encounter a bug? or a challenge?
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
    * KSQL applications
    * Schema Registry instances
    * Confluent Control Center
    * KSQL server instances
* Manage topic naming with a topic name convention
    * Including the definition of *projects*, *teams*, *datatypes* and for sure the topic name
    * Some of the topics are flexible defined by user requirements
* Allow for creation, delete and update of:
    * topics, following the topic naming convention
    * Topic configuration, variables like retention, segment size, etc
    * Acls, or RBAC rules
    * Service Accounts (Experimental feature only available for now in Confluent Cloud)
* Manage your cluster schemas.
    - Support for Confluent Schema Registry

Out of the box support for Confluent Cloud and other clouds that enable you to use the AdminClient API.

### How can I run JulieOps directly?

This tool is available in multiple formats:

- As a Docker image, available from [docker hub](https://hub.docker.com/r/purbon/kafka-topology-builder)
- As an RPM package, for the RedHat alike distributions
- As a DEB package, for Debian based distros
- Directly as a fat jar (zip/tar.gz)
- As a fat jar.

The latest version are available from the [releases](https://github.com/kafka-ops/julie/releases) page.

#### How to execute the tool

This is how you can run the tool directly as a docker image:

```bash
docker run purbon/kafka-topology-builder:latest julie-ops-cli.sh  --help
Parsing failed cause of Missing required options: topology, brokers, clientConfig
usage: cli
    --brokers <arg>                  The Apache Kafka server(s) to connect
                                     to.
    --clientConfig <arg>             The client configuration file.
    --dryRun                         Print the execution plan without
                                     altering anything.
    --help                           Prints usage information.
    --overridingClientConfig <arg>   The overriding AdminClient
                                     configuration file.
    --plans <arg>                    File describing the predefined plans
    --quiet                          Print minimum status update
    --topology <arg>                 Topology config file.
    --validate                       Only run configured validations in
                                     your topology
    --version                        Prints useful version information.
```

If you install the tool as rpm, you will have available in your $PATH the _julie-ops-cli.sh_.
You can run this script with the same options observed earlier, however you will need to be using, or be in the group,
for the user julie-kafka.

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

Also, please check, the documentation in [the docs](https://julieops.readthedocs.io/) for extra information and
examples on managing ACLs, RBAC, Principales, Schemas and many others.

## Troubleshooting guides

If you're having problems with JulieOps I would recommend lookup up two main sources of information:

* The project [issues](https://github.com/kafka-ops/julie/issues) tracker. Highly possible others might have had your problem before.
* Our always work in progress [troubleshooting guide](troubleshooting.md)

## Interested in contributing back?

Interested on contributing back? might be have an idea for a great future? or wanna fix a bug?
Check our [contributing](CONTRIBUTING.md) doc for guidance.

## Building JulieOps from scratch (source code)

The project is build using Java and Maven, so both are required if you aim to build the tool from scratch.
The minimum version of Java supported is Java 8, note it soon will be deprecated here, it is only keep as supported
for very legacy environments.

It is recommended to run JulieOps with Java 11 and an open JDK version.

### Building a release

If you are interested on building a release artifact from the source code, check our [release](RELEASE.md) doc for guidance.

Nightly builds as well as release builds are regularly available from the [Actions](https://github.com/kafka-ops/julie/actions)
in this project.

Nightly release build are available as well from [here](https://github.com/kafka-ops/julie/actions/workflows/nightly-artifacts-build.yml).

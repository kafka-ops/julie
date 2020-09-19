# The Kafka Topology builder tool

<a href="https://codeclimate.com/github/purbon/kafka-topology-builder/maintainability"><img src="https://api.codeclimate.com/v1/badges/ef4bcda7d1b5fd0a4f1e/maintainability" /></a> [![Build Status](https://travis-ci.org/purbon/kafka-topology-builder.svg?branch=master)](https://travis-ci.org/purbon/kafka-topology-builder)

[![Gitter](https://badges.gitter.im/kafka-topology-builder/community.svg)](https://gitter.im/kafka-topology-builder/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) [![Documentation Status](https://readthedocs.org/projects/kafka-topology-builder/badge/?version=latest)](https://kafka-topology-builder.readthedocs.io/?badge=latest)

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

## Topic naming convention
Topic names will be chosen according to the scheme:
```
[context].[source].[project-name].[topic-name]
```
It is possible to give a finer structure to the topic names by specifying additional fields between 
the `company` and `projects` fields. Optionally, a `dataType` can be specified, which will be suffixed to the topic name.
For example:
```
context: "context"
company: "company"
env: "env"
source: "source"
projects:
  - name: "projectA"
    topics:
      - name: "foo"
      - name: "bar"
        dataType: "avro"
```
will lead to topic names
```
context.company.env.source.projectA.foo
context.company.env.source.projectA.bar.avro
```

## What ACLs are created
Kafka Topology Builder will assign the following ACLs:

* each principal in the `consumers` list will get `READ` and `DESCRIBE` permissions on each topic in the containing project as well as `READ` access on every consumer group
* each principal in the `producers` list will get `WRITE` and `DESCRIBE` permissions on each topic in the containing project
* each principal in the `streams` list will get 
  * `READ` access on every topic in its `read` sub-object
  * `WRITE` access on every topic `write` sub-object
  * `ALL` access on every topic starting with fully-qualified project name, e.g. `context.company.env.source.projectA` in the example above. These are `PREFIXED` ACLs.
  
* each principal for a connector will get
  * read and write access on the corresponding `status_topic`, `offset_topic`, and `config_topics` (`LITERAL` ACLs)
    * these fields default to `connect-status`, `connect-status`, and `connect-configs`. Hence access to these topics will be granted to the Connect principal if the fields are not explicitly given.    
  * `CREATE` access on the cluster resource
  * `READ` access on every topic in the corresponding `topics.read` subobject
  * `WRITE` access on every topic in the corresponding `topics.write` subobject
  * `READ` access on the group specified in the corresponding `group` field
    * if no `group` is specified, rights to `connect-cluster` will be granted
* the principal for a `schema_registy` platform component will be given `DESCRIBE_CONFIGS`, `READ`, and `WRITE` access to each topic.
* the principal for a `control_center` platform component will be given:
    * `DESCRIBE` and `DESCRIBE_CONFIGS` on the cluster resource
    * `READ` on every consumer group starting with the corresponding `appId` (`PREFIXED` ACLs)
    * `CREATE`, `DESCRIBE`, `READ`, and `WRITE` access on each topic starting with the corresponding `appId` (`PREFIXED`)
    * `CREATE`, `DESCRIBE`, `READ`, and `WRITE` access on the `_confluent-metrics`, `_confluent-command`, and `_confluent-monitoring` topics
         
## Which ACLs does the user running Kafka Topology Builder need?
The principal which the Kafka Topology Builder uses to authenticate towards the Kafka cluster should have the following rights:

* `ALTER` on the cluster resource to create and delete ACLs
* `DESCRIBE` on the cluster resource
* the following operations be allowed for topic resources prefixed with the current context:
    * `ALTER_CONFIGS`, `CREATE`, and `DESCRIBE`
    * `ALTER` when changing the number of partitions should be allowed
    * `DELETE` when topic deletion should be allowed


See (https://docs.confluent.io/current/kafka/authorization.html)[here] for an overview of ACLs. When setting up the topology builder for a specific context,
prefixed ACLs can be used for all topic-level operations.

When using Confluent Cloud, a *service account* with the proper rights to run the topology builder for the context `samplecontext` could be generated as follows using the Confluent Cloud CLI `ccloud`:
```bash
ccloud service-account create sa-for-ktb --description 'A service account for the Kafka Topology Builder'
# note the Id for the service account, we will use 123456 below

ccloud kafka acl create --allow --service-account 123456 --cluster-scope --operation ALTER 
ccloud kafka acl create --allow --service-account 123456 --cluster-scope --operation DESCRIBE 
cloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation ALTER_CONFIGS
cloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation CREATE
cloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation DESCRIBE
cloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation ALTER
cloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation DELETE
```


## Interested in contributing back?

Interested on contributing back? might be have an idea for a great future? or wanna fix a bug? Check our [contributing](CONTRIBUTING.md) doc for guidance.

## Building the Kafka Topology Builder from scratch (source code)

The project is build using Java and Maven, so both are required if you aim to build the tool from scratch.
The minimum version of Java supported is Java 8, note it soon will be deprecated here, it is only keep as supported for very legacy environments.

It is recommended to run the Kafka Topology Builder with Java 11 and an open JDK version.

### Building a release

If you are interested on building a release artifact from the source code, check our [release](RELEASE.md) doc for guidance.

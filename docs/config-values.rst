.. _config:
Important configuration values
*******************************

This page describe the most common configuration values for the Kafka Topology Builder, this values can be set within the topology-builder properties file.

Access control configuration
-----------

Configure the access control methodology.

**Property**: *topology.builder.access.control.class*
**Default value**: "com.purbon.kafka.topology.roles.SimpleAclsProvider"
**values**:
 - RBAC: "com.purbon.topology.roles.RBACProvider"
 - ACLs: "com.purbon.kafka.topology.roles.SimpleAclsProvider"

RBAC configuration
-----------

To configure RBAC, as a user you need to setup the access to your MDS server location, for this you need to setup the user and password to access it.
An example configuration looks like this:
::
    topology.builder.mds.server = "http://localhost:8090"
    topology.builder.mds.user = "mds"
    topology.builder.mds.password = "mds-secret"

A part from that, you need to setup the UUID for each of your clusters. This is one like this:
::
    topology.builder.mds.kafka.cluster.id = "foobar"
    topology.builder.mds.schema.registry.cluster.id = "schema-registry-cluster"
    topology.builder.mds.kafka.connect.cluster.id = "connect-cluster"


Schema Management
-----------
If you plan to manage and deploy schemas with KTB, you must define the url to your Confluent Schema Registry as follows
::
    schema.registry.url = "http://localhost:8081"



Topology Builder backend usage and selection
-----------

It is necessary for the topology builder to keep some state, if it does not retrieve state from the cluster (see also "topology.state.cluster.enabled"),
for example for situations when the tool needs to decide what ACLs to remove.
As well this property is important when the tool does not manage all topics in the cluster, so it is important to know it context.

The default implementation is a File, however is possible ot use other systems.
To configure it you can use:

Configure the state management system.
**Property**: *topology.builder.state.processor.class*
**Default value**: "com.purbon.kafka.topology.backend.FileBackend"
**values**:
 - File: "com.purbon.kafka.topology.backend.FileBackend"
 - Redis: "com.purbon.kafka.topology.backend.RedisBackend"

If you are using redis, you need to extend two other properties to setup the server location:
::
  topology.builder.redis.host = "example.com"
  topology.builder.redis.port = 6379

Customize the topic naming convention
-----------

A request, not either common, but necessary in some situations is to customize the topic naming convention.
For this the Kafka Topology Builder offers the user the option to set it up using the configuration file.

This future accepts patterns using the `jinja template <https://jinja.palletsprojects.com/en/2.11.x/>`_ formatting.
*NOTE*: The properties used in the template need to exist in the topology as attributes.

As a user you can customize:

- **Property**: *topology.topic.prefix.format*, to set the full topic naming format.
- **Property**: *topology.project.prefix.format*, to set the project level name format, it should be a subset of the previous one.
- **Property**: *topology.topic.prefix.separator*, to select a custom separator between attributes.

Optimised number of ACLs and RBAC bindings
-----------

This property is used to reduce the number of ACLs, or RBAC bindings, created. In the normal operational mode, the KTB, will create direct pair of bindings for each user and topic.
However for some organisations, it might be enough, to create an optimised list by using prefixed bindings.

**Property**: *topology.acls.optimized*
**Default value**: "false"

An example configuration might look like this:
::
    topology.acls.optimized=true

Internal topics prefixes
-----------

This is used to avoid deleting topics not controlled by topology builder.

**Property**: *kafka.internal.topic.prefixes*
**Default value**: "_"

An example configuration might look like this:
::
    kafka.internal.topic.prefixes.0=_
    kafka.internal.topic.prefixes.1=topicPrefixA
    kafka.internal.topic.prefixes.2=topicPrefixB

Topology level validations
-----------

It is now possible to define a list of validations to be applied to the desired Topology file.

As a user you can list the validations to be applied using the configuration property:

- **Property**: *topology.validations*

This property accepts the list of validations available in the class path.
They will be applied in sequence as defined.

An example configuration might look like this:
::
    topology.validations.0=topology.CamelCaseNameFormatValidation
    topology.validations.1=topic.PartitionNumberValidation

Users can pull custom validation available from the class path.

Prevent ACL for topic creation for connector principal
-----------

By default KTB will create the ACLs needed for connectors to create their own topics (with CREATE ACL operation on the CLUSTER resource).
You can override this behaviour by setting the config below to `false`. And instead create the needed topics with KTB.

**Property**: *topology.connector.allow.topic.create*
**Default value**: true

An example configuration will look like this:
::
    topology.connector.allow.topic.create=false

Retrieve topic management state from local controlled view
-----------

By default since it's creation KTB has been retrieving the state of topics from the target cluster, this means pulling the actual view directly
from there (AK cluster) using AdminClient. To disable this it can be done below.

If you want to manage the current view of topics from the own KTB  cluster state subsystem, you should use this property.

**Property**: *topology.state.topics.cluster.enabled*
**Default value**: true

This property is for the time being true as default (backwards compatible).

An example to use local topic management state will look like this:
::
    topology.state.topics.cluster.enabled=false


Retrieve management state from local controlled view
-----------

KTB for everything apart from topics uses a local state, so that KTB's uses the actual state not its internal state this means pulling the actual view directly,
we can enable this for everything, topics, acls, service accounts etc. Note this flag supercedes the topology.state.topics.cluster.enabled.

If you want to manage the current view of everything from the own KTB cluster state subsystem, you should use this property.

**Property**: *topology.state.cluster.enabled*
**Default value**: true

This property is for the time being false as default (backwards compatible).

An example to use actual view management state will look like this:
::
    topology.state.cluster.enabled=false


Control allowed Topics to be managed by KTB
-----------

This property is used to control which Topics are allowed to be managed by the KTB, this variable contains a list of allowed prefixes.

**Property**: *topology.topic.managed.prefixes*
**Default value**: "[]"

An example configuration might look like this:
::
    topology.topic.managed.prefixes.0=User:AService
    topology.topic.managed.prefixes.1=User:BService

If this prefix list is used, only topics that match the prefix will be ever processed, anything else will be ignored.
This is useful in a shared cluster, to avoid KTB removing/accidentally managing topics managed by other teams with seperate pipelines.


Control allowed Service accounts to be managed by KTB
-----------

This property is used to control which Service Accounts are allowed to be managed by the KTB, this variable contains a list of allowed prefixes.

**Property**: *topology.service.accounts.managed.prefixes*
**Default value**: "[]"

An example configuration might look like this:
::
    topology.service.accounts.managed.prefixes.0=User:AService
    topology.service.accounts.managed.prefixes.1=User:BService

If this prefix list is used, only service accounts that match the prefix will be ever processed, anything else will be ignored.
This is useful in a shared cluster, to avoid KTB removing/accidentally managing service accounts managed by other teams with seperate pipelines.

Control allowed Group to be managed by KTB
-----------

Note, currently KTB just manages Group ACLS.

This property is used to control which Group prefixes are allowed to be managed by the KTB, this variable contains a list of allowed prefixes.

**Property**: *topology.group.managed.prefixes*
**Default value**: "[]"

An example configuration might look like this:
::
    topology.group.managed.prefixes.0=NameSpaceA
    topology.group.managed.prefixes.1=NameSpaceB

If this prefix list is used, only groups that match the prefix will be ever processed, if wildcard it will be managed if the service account is managed by KTB, anything else will be ignored.
This is useful in a shared cluster, to avoid KTB removing/accidentally managing group acls by other teams with seperate pipelines.

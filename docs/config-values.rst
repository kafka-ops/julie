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




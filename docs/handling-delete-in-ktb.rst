Handling delete in the Kafka Topology Builder
*******************************

As the reader might already be aware, the Kafka Topology Builder is capable of handling deletes for you.
This way the project can ensure that the final state of the cluster is consistent with the declarative state in the topology descriptors.

However in some situations having delete enabled might not be what you are looking for.
For this reason the tool allows you to control it in full granularity.

Delete flag in the CLI
-----------

The CLI provide you currently with a global DELETE flag, this would allow deletion of all resources controlled by the Kafka Topology builder.
The user can set this flag (*--allowDelete*) from the CLI as described.

.. code-block:: bash

    $> kafka-topology-builder.sh  --help
        usage: cli
          --allowDelete          Permits delete operations for topics and
                                 configs. (deprecated, to be removed)
          --brokers <arg>        The Apache Kafka server(s) to connect to.
          --clientConfig <arg>   The AdminClient configuration file.
          --dryRun               Print the execution plan without altering
                                  anything.
          --help                 Prints usage information.
          --quiet                Print minimum status update
          --topology <arg>       Topology config file.
          --version              Prints useful version information.

*NOTE*: This is a global flag, allowing/deny all delete operations.

By default the value for this variable is false, so the Kafka Topology Builder will **not** delete any resource.

Granular delete flags
-----------

There could be situations when the reader aim to :
 * Control delete operations in a more granular way, for example allow delete of bindings (Acls/RBAC) but not topics.
 * Control the delete via ENV variables. This could be very handy when doing CI/CD integrations and setting variables over the pipeline executions.

This can be done in KTB by using the capabilities provided by the configuration library in use.
The tool allows the user to set:

* A configuration variable to allow/deny the delete operations.
* Use an ENV variable as a first priority citizen to handle this operation

*NOTE* ENV variables take priority over other ways to set a config value.

Topics deletion flag
^^^^^^^^^^^
The user can control topic deletion by:

- setting the *allow.delete.topics* configuration in the provided file to the tool.
- set the ENV variable *ALLOW_DELETE_TOPICS* when calling the tool from the CLI.

Bindings deletion flag
^^^^^^^^^^^

The user can control bindings deletion by:

- setting the *allow.delete.bindings* configuration in the provided file to the tool.
- set the ENV variable *ALLOW_DELETE_BINDINGS* when calling the tool from the CLI.
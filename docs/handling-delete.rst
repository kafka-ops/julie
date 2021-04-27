Handling delete in Julie Ops
*******************************

As the reader might already be aware, Julie Ops is capable of handling deletes for you.
This way the project can ensure that the final state of the cluster is consistent with the declarative state in the topology descriptors.

However in some situations having delete enabled might not be what you are looking for.
For this reason the tool allows you to control it in full granularity.

*NOTE*: By default this configurations options are false, to enable delete the reader will need to enable each one of them based on your use case.

Granular delete flags
-----------

There could be situations when the reader aim to :
 * Control delete operations in a more granular way, for example allow delete of bindings (Acls/RBAC) but not topics.
 * Control the delete via ENV variables. This could be very handy when doing CI/CD integrations and setting variables over the pipeline executions.

This can be done in Julie Ops by using the capabilities provided by the configuration library in use.
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


Principals deletion flag
^^^^^^^^^^^

The user can control principals deletion by:

- setting the *allow.delete.principals* configuration in the provided file to the tool.
- set the ENV variable *ALLOW_DELETE_PRINCIPALS* when calling the tool from the CLI.
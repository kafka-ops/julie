JulieOps Audit Log
*******************************

One of the most requested features for a system such as JulieOps is the opportunity to inspect the different actions that has been taken.
This is critical specially for highly regulated environments where an auditor can come in and request what has happened with a certain system.

Since the introduction of the audit log mechanism, now this is possible with JulieOps.

Configure and use it
-----------

By default, the audit log is disabled, you would be require to activate it.
A sample configuration for enabling the audit log might look like this:

.. code-block:: JAVA
    julie.audit.enabled=true

if not specifically configured, JulieOps will print the audit log messages to the stdout, useful if you are in
a kubernetes environment, but generally not for everyone.

There is a possibility to configure an special Kafka Appender for your audit logs, this appender will send this
messages to a dedicated kafka topic.
A sample configuration, example with a sasl-plain config, would be like this:

.. code-block:: JAVA
    julie.audit.enabled=true
    julie.audit.appender.class=com.purbon.kafka.topology.audit.KafkaAppender
    julie.audit.appender.kafka.bootstrap-servers=....
    julie.audit.appender.kafka.security.protocol=SASL_PLAINTEXT
    julie.audit.appender.kafka.sasl.mechanism=PLAIN
    julie.audit.appender.kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
        username="kafka" \
        password="kafka";

A few notes on this configuration.

* Everything that starts with *julie.audit.appender.kafka* will be passed as part of the dedicated Kafka Producer configuration.
Please prefix the necessary config to access your destination cluster with this prefix.
* By default messages are send to *_julieops_audit_log* topic, if you would like to use a different topic please configure *julie.audit.appender.kafka.topic*.
This topic will not be pre-created for you, it will need to exist in the cluster.

**Note:** Right not there is no *RollingFileAppender*, that would be an amazing future contribution to this feature.

Sample messages
-----------

JulieOps audit log will generate a custom message for each action performed, from creating a topic, acls towards updating your configurations.
The messages will look like this:

.. code-block:: JSON
    [{
		"principal": "name",
		"resource_name": "rn://create.account/com.purbon.kafka.topology.actions.accounts.CreateAccounts/name",
		"operation": "com.purbon.kafka.topology.actions.BaseAccountsAction$1"
	},
	{
		"principal": "another-name",
		"resource_name": "rn://create.account/com.purbon.kafka.topology.actions.accounts.CreateAccounts/another-name",
		"operation": "com.purbon.kafka.topology.actions.BaseAccountsAction$1"
	}]

each action will have more properties specific to it, ranging from the binding details, the principals, etc...
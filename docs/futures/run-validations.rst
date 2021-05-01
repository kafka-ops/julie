Validate your configuration before application
*******************************

A normal practise in many *gitops* deployments is to run a set of automated validations before allowing the changes in.
JulieOps allows the users to run a variable set of validations before the project will apply the changes into each of the managed components.

Validate a topology in a feature branch
-----------

As a user you can use the *--validate* CLI option to only validate the incoming topology. Note this would run a validation completely offline,
without any knowledge of the current state in the cluster.

To configure which validations you require for your topology the reader would need to do it in the configuration file, this can be done like this:

.. code-block:: bash

        topology.validations.0=com.purbon.kafka.topology.validation.topic.ConfigurationKeyValidation
        topology.validations.1=com.purbon.kafka.topology.validation.topic.TopicNameRegexValidation
        topology.validations.topic.name.regexp="[a-z0-9]"

In the previous example we have configured two validations.

1.- ConfigurationKeyValidation will make sure all config keys are valid for Kafka.
2.- Will validate, based on the configured regexp that all topic names follow the right pattern.

Add your own validations
-----------

JulieOps provides you with a set of integrated validations, however you as user can provide your own. To do so you will need to:

* Code your validation following the required interfaces as defined in the JulieOps project. See core validations to see the current pattern.
* Build a jar with your validations.
* Run JulieOps with a configured CLASSPATH where the JVM can find access to your validations jar in order to dynamically load them.
Remember when running JulieOps you can use the _JULIE_OPS_OPTIONS_ env variable to pass custom system configurations such as CLASSPATH or related to security.
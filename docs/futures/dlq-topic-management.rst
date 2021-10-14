Managed DLQ topics with JulieOps
*******************************

Since version 3.2 it is possible in JulieOps to manage DLQ topics in a controlled, but still, abstracted way.
Until this version if you wanted to have DLQ topics for your deployment you had to add them manually in your topology files, however this could become of a certain pain if too many DLQ scenarios are necessary.


Configure the DLQ topics in JulieOps
-----------

As a user of JulieOps you can activate DLQ topic management by configuring the *topology.dlq.topics.generate=true* in your configuration file,
once this is in, JulieOps will by default generate DLQ equivalent topics for *ALL* your topology specified topics.

We know this might not be your wish, might be you only want a DLQ topic for a known one, or you want to exclude a list of topics from your
topology, this is possible if you configure the respective allow and deny list.

For example, if you aim to only allow a certain topic, it could be done like this:

.. code-block:: JAVA

    topology.dlq.topics.allow.list.0=contextOrg.source.foo.bar.avro

in this example, only one DLQ topic will be generate, the one for the topic with a final name *contextOrg.source.foo.bar.avro*


If instead, you configure:

.. code-block:: JAVA

    topology.dlq.topics.deny.list.0=contextOrg.source.foo.bar.avro

JulieOps will generate DLQ equivalent topics for *ALL* topics in the topology, except for the one we have configured.


Tune the final DLQ topic name
-----------

As a proud user of JulieOps you might be interested in customize the final name for your DLQ topic.
This feature can be achieved in a similar fashion as in the other customization topics.

If instead, you configure:

.. code-block:: JAVA

    topology.topic.dlq.prefix.format={dlq}}.{{context}}.{{project}}
    topology.topic.dlq.label=muelltone

The final DLQ topic generated will include the DLQ label, context and project name.

*Notice*, we have used as well the *topology.topic.dlq.label* config property, this one allow you to customize which label is going
to be used to differentiate the DLQ topics from the other ones. In the previous example this would be muelltone, so the final topic might look like
*muelltone.context.project*
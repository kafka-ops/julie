Use of custom configuration plans
*******************************

It is possible, with Julie Ops, to use custom configuration plans.
These plans will allow you to summarize a set of default configuration properties in a reusable label, making easier for users and as well operators to configure each topic.

You might wonder, why are Plans an interesting thing, this are a few ideas where you can use it:

* To define custom service levels, where you have topics with different retentions or that accept different message sizes.
* To group default configuration, allowing you as Julie Ops user and operator to have an smaller file size.

And I am sure there is going to be more.

How can you take advantage of Plans?

Defining the your plans
-----------

As an operator of Julie Ops you can define a set of custom plans, this would be a file that look like this:

.. code-block:: YAML

  plans:
    gold:
      alias: "gold"
      config:
        retention.ms: "5000"
        max.message.bytes: "7340116"
    silver:
      alias: "silver"
      config:
        retention.ms: "6000"
        max.message.bytes: "524294"

In this file, the reader can see two plans, *gold* and *silver* with different default configuration values.


Using plans in the Topology
-----------

Once the plan is defined, you can use it in your Topology files. This process would look like this:

.. code-block:: YAML

  context: "contextOrg"
  source: "source"
  projects:
    - name: "foo"
      topics:
        - name: "foo"
          config:
            replication.factor: "1"
            num.partitions: "1"
        - name: "fooBar"
          plan: "silver"
          config:
            replication.factor: "1"
        - name: "barFoo"
          plan: "gold"
          config:
            replication.factor: "1"
        - name: "barFooBar"
          plan: "gold"
          config:
            replication.factor: "1"

In this Topology the topics *fooBar*, *barFoo* and *barFooBar* will be using custom plans.

**Things to consider**:

* Configuration values defined in the topic take precedence over the config values defined plan.
* A topic can have only a plan, without providing any configuration.
* The Plan label is optional, topics can use, or not, plans.
* If plans are defined in the Topology, but no proper definition file is passed, the tool will complain.

What about from the CLI
-----------

As a user of Julie Ops CLI, if interested to use Plans you can pass a file using the dedicated parameter.
An example call will look like:

.. code-block:: BASH

  $>  julie-ops-cli.sh --brokers localhost:9092  \
                --clientConfig example/topology-builder.properties \
                --topology example/descriptor-with-plans.yaml
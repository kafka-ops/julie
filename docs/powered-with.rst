A collection of Kafka Topology Demos
*******************************

If you're looking for building examples, you can find in this page provided examples build by community members.

A Jenkins CI/CD example
-----------

In case you're looking to use the Kafka Topology Builder in collaboration with Jenkins, you can find an example available for you `here <https://github.com/purbon/kafka-topology-builder-demo>`_.

This project uses the Jenkins Pipeline future with the Docker execution agent to build and compose the two required pipelines, the one for `pull requests <https://github.com/purbon/kafka-topology-builder-demo/blob/master/JenkinsfileVerify>`_
and the one for `applying the changes to the cluster <https://github.com/purbon/kafka-topology-builder-demo/blob/master/Jenkinsfile>`_.


A TravisCI integration
-----------

If you are are TravisCI user, you can find an example setup available for you `here <https://github.com/nerdynick/kafka-topology-builder-travisci-demo>`_.

In this example you can see a similar setup as with the previously described Jenkins example, using docker and two different pipelines you can grasp a working
example to start using the Kafka Topology Builder.

Thanks `Nikoleta Verbeck <https://github.com/nerdynick>`_ for making this.


How to contribute a new page
-----------

If you are using the Kafka Topology Builder, integrating it with more CI/CD solutions let us know and we will add your example to this list. Examples help
newcomers use it faster and are a great contribution to the project.

Thanks a lot.
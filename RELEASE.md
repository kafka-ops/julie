# How to build a release

Interested in building a release of the Kafka Topology Builder, this document will guide you to the necessary steps.

## Building an RPM release

To build an RPM release artifact you need to have:

* A RedHat based system, for example Redhat 7, CentOS 7 or newer.
* Install the rpm builder tolling

### Preparing the environment

The Kafka Topology Builder takes advantage of the maven rpm plugin, that depends on system tools to create the packages.
So we need to install the required tooling like this:

```bash
   $> yum install gcc rpm-build rpm-devel rpmlint make  bash coreutils diffutils patch rpmdevtools
```

### Building the artifact

As introduced earlier, to build the artifact we are going to take advantage of the maven rpm plugin, to build a release we need to execute:

```bash
   $> mvn rpm:rpm
```

## Releasing a version to a docker registry

To release a version to a docker registry, for example docker hub, you can use the build.sh script under the [release/docker/](release/docker/) directory.
Once the new image is build, the reader will be required to push to the docker repository of interest.
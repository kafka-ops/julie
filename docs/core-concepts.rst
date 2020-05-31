Core Concepts
*******************************

In this page you will find a description of the core concepts necessary to move on with implementing a gitops approach for automating Kafka operations.

What is gitops?
-----------

GitOps is a paradigm or a set of practices that empowers developers to perform tasks which typically fall under the purview of IT operations.
GitOps requires us to describe and observe systems with declarative specifications that eventually form the basis of continuous everything.

*Source* https://www.cloudbees.com/gitops/what-is-gitops

So with this we bring into the table:

* Autonomy for teams, they can request and fulfil their own ops needs independently.
* A declarative approach, users describe what they need and not how it is implemented, facilitating the solution of problems, keeping detailed implementation abstracted.
* A centralised way to verify and control changes, providing an audit trail to fulfil change management requirements.
* Have a centralised source of truth where the current state of the platform is represented.

What do I need to use gitops?
-----------

To setup a gitops flow you need three basic components:

- A git server, anything will work from Github, Gitlab or Bitbucket
- A CI/CD pipeline, for example the master Jenkins!
- And the Kafka Topology Builder project

This building blocks will build up necessary integration steps:

* In the git server, ex. Gitlab, we're going to store the topology definition, so how we want the cluster to look like in terms of topics, acls, etc.
* The git server will manage as well the change management flow using Change Requests (or pull request :-P)
* Jenkins will be the muscle of the operation, in charge of retrieving the git content, do an initial verification and run the topology builder to apply the changes in Apache Kafka.
* And The Kafka Topology Builder project, the responsible of interpreting the configuration files (topologies) and apply the changes directly into Apache Kafka and related components.

How are the users / dev teams leveraging gitops?
-----------

This flow is in place, at most the organisations, in order to enable individual cluster users request the resources they need for their project, and do this without very tied formal processes or even worst a Jira ticket :-).

The traditional flow for this approach is:

- A user (dev team) has the need of more topics (for example)
- This same user, does a new branch in git, and apply the required changes in the configuration file
- Once happy with the required changes, it will be submitting a change request (PR) to merge this changes to the master branch

then (in more controlled environments) :

- An authorised agent of change, for example people from the operations team, will do a manual review of the changes. If required will request changes to the original requester
- An automatic verification, using the CI/CD pipeline, is run automatically once the change request is created
- Once the agent is happy with the change request, it will merge it into master
- From master a CI/CD job will be triggered automatically to perform the requested changes into the cluster

*NOTE*: In environments, or organisations, less restrictive you might like to approve the change request automatically if the CI/CD pipeline verification is greed.

It should be noted as well that thanks to the change request mechanism offered by Gitlab, Github and Bitbucket, the change management chain and audit is fulfilled.
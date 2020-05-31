Workflow setup
*******************************

This section describe the configuration steps need to setup a flow with the Kafka Topology Builder.
For this example we're going to use:

* Github as the git server.
* Jenkins as the CI/CD pipeline.

Configuration is possible with other technologies such as Gitlab or Concourse for example.

Configure the git server
-----------

From the git server perspective you are going to need to setup the next steps:

* Block the master branch to push commits, so everyone is required to perform changes using a pull request.
* Configure a webhook to inform the CI/CD pipeline every time:
  * A pull request is created or changed.
  * A merge happened to the master branch.

For Github the webhook events to be selected are:

- *Pull requests*: _Pull request opened, closed, reopened, edited, assigned, unassigned, review requested, review request removed, labeled, unlabeled, synchronized, ready for review, converted to draft, locked, or unlocked._
- push event.

Setup CI/CD
-----------

TBA

Using it in the day to day
-----------

TBA

Taking advantage of the Kafka topology Builder across environments
-----------

TBA
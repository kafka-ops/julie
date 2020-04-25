pipeline {

    agent any

    tools {
        maven 'maven'
        jdk 'jdk8'
    }

   stages {
      stage('checkout') {
        when { branch 'master' }
         steps {
            git branch: 'demo-setup', url: 'https://github.com/purbon/kafka-topology-builder.git'
         }
      }
      stage('build') {
          steps {
              sh 'mvn assembly:assembly'
          }
      }
      stage('run') {
          steps {
              withCredentials([usernamePassword(credentialsId: 'confluent-cloud	', usernameVariable: 'CLUSTER_API_KEY', passwordVariable: 'CLUSTER_API_SECRET')]) {
                sh './demo/build-connection-file.sh > topology-builder.properties'
              }
              sh 'java -jar target/kafka-topology-builder-jar-with-dependencies.jar  --brokers ${Brokers} --clientConfig topology-builder.properties --topology ${TopologyFiles}'
          }
      }
   }
}

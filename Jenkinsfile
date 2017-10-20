pipeline {
  agent any
  options {
    buildDiscarder(logRotator(artifactNumToKeepStr: '1', numToKeepStr: ''))
  }
  stages {
    stage ('SCM'){
      steps {
        checkout([
          $class: 'GitSCM',
          branches: scm.branches,
          doGenerateSubmoduleConfigurations: false,
          extensions: scm.extensions + [[$class: 'SubmoduleOption', disableSubmodules: false, recursiveSubmodules: true, reference: '', trackingSubmodules: false, parentCredentials: true]],
          submoduleCfg: [],
          userRemoteConfigs: scm.userRemoteConfigs])
      }
    }
    stage('Clean') {
      steps {
        sh './gradlew --build-cache --parallel clean'
      }
    }
    stage('Build') {
      steps {
        sh './gradlew --build-cache --parallel build buildDist'
      }
    }
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: '**/target/test-results/test/TEST-*.xml'
      archive '.dist/**/*'
    }
  }
}


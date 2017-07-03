pipeline {
  agent any
  options {
  	buildDiscarder(logRotator(artifactNumToKeepStr: '1', numToKeepStr: ''))
  }
  stages {
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


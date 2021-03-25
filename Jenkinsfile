pipeline {
    agent {
        docker {
            image 'maven'
        }
    }

    options {
        skipStagesAfterUnstable()
    }

    stages {
        stage('Build') {
            withMaven {
                sh "mvn clean verify"
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Deliver') {
            steps {
                sh './jenkins/scripts/deliver.sh'
            }
        }
    }
}
node {
  stage ('Fetch source') {
      git url: 'http://git.home/spiller/epg-scrapper.git'
  }

  stage ('Build') {
    withMaven {
      sh "mvn clean package -DskipTests"
    }
  }

  stage ('Test') {
    withMaven {
      sh "mvn test -DskipIntegrationTests=true"
    }
  }

  stage ('Intg-Test') {
    withMaven {
      sh "mvn failsafe:integration-test failsafe:verify"
    }
  }

  stage ('Deploy') {
    withMaven {
      sh "mvn deploy:deploy"
    }
  }

  stage('Publish') {
        def customImage = docker.build("epg-scrapper:${env.BUILD_ID}")
        customImage.push()
        customImage.push('latest')
  }

}
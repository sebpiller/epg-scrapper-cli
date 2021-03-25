node {
  stage ('Fetch source') {
      git url: 'http://git.home/spiller/epg-scrapper.git'
  }

  stage ('Build') {
    withMaven {
      sh "mvn clean compile -DskipTests"
    }
  }

  stage ('Test') {
    withMaven {
      sh "mvn test"
    }
  }

  stage ('Intg-Test') {
    withMaven {
      sh "mvn integration-test"
    }
  }

  stage ('Deploy') {
    withMaven {
      sh "mvn deploy"
    }
  }

  stage('Publish') {
        def customImage = docker.build("epg-scrapper:${env.BUILD_ID}")
        customImage.push()
        customImage.push('latest')
  }

}
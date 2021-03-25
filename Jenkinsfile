node {
  stage ('Build') {
    git url: 'http://git.home/spiller/epg-scrapper.git'
    withMaven {
      sh "mvn clean compile -DskipTests"
    }
  }

  stage ('Test') {
    git url: 'http://git.home/spiller/epg-scrapper.git'
    withMaven {
      sh "mvn test"
    }
  }

  stage ('Deploy') {
    git url: 'http://git.home/spiller/epg-scrapper.git'
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
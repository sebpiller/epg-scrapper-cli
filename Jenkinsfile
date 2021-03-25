node {
  stage ('Build') {
    git url: 'http://git.home/spiller/epg-scrapper.git'
    withMaven {
      sh "mvn clean verify"
    } // withMaven will discover the generated Maven artifacts, JUnit Surefire & FailSafe reports and FindBugs reports
  }
}
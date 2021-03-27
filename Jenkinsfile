pipeline
{
agent any

environment {
    BRANCH = "${env.BRANCH_NAME}"
}

tools
 {
  maven 'Maven'
  jdk 'OpenJDK11'
 }

options
 {
  buildDiscarder(logRotator(numToKeepStr: '4'))
  skipStagesAfterUnstable()
  disableConcurrentBuilds()
 }


triggers
 {
  // MINUTE HOUR DOM MONTH DOW
  pollSCM('H 6-18/4 * * 1-5')
 }


stages
 {

  stage('Initialize')
   {
    steps
     {
      script
       {
          def matcherRelease = env.BRANCH_NAME =~ /^release\/(.*)$/

          if(matcherRelease.matches()) {
             env.BRANCH_TYPE = "release"
             env.RELEASE_MAJ_MIN = matcherRelease[0][1]

             env.VERSIONING_OVERRIDE = " -Dbranch=" + env.RELEASE_MAJ_MIN + " -Drevision=$BUILD_NUMBER -Dmodifier= "

             echo "RELEASE BRANCH DETECTED!"
          } else {
             env.BRANCH_TYPE = "snapshot"
             env.VERSIONING_OVERRIDE = " -Drevision=$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
             echo "NON-RELEASE BRANCH DETECTED"
          }

          echo "VERSIONING_OVERRIDE: " + env.VERSIONING_OVERRIDE
       }
     }
   }

  stage('Clean')
   {
    steps
     {
      script
       {
          sh '''
              mvn --batch-mode clean ${VERSIONING_OVERRIDE}
          '''
       }
     }
   }

  stage('Build')
   {
    steps
     {
      script
       {
          sh '''
             mvn --batch-mode package -DskipUTs -DskipITs -Dmaven.site.skip ${VERSIONING_OVERRIDE}
          '''
       }
     }
   }


  stage('Unit Tests')
   {
    steps
     {
      script
       {
          sh '''
             mvn --batch-mode verify -DskipITs -Dmaven.site.skip ${VERSIONING_OVERRIDE}
          '''
       }
     }
    post
     {
      always
       {
        junit testResults: 'target/surefire-reports/*.xml'
       }
     }
   }

 stage('Integration tests')
  {
   steps
    {
     script
      {
         sh '''
             mvn --batch-mode verify -DskipUTs -Dmaven.site.skip ${VERSIONING_OVERRIDE}
         '''
      }
    }
    post
     {
      always
       {
        junit testResults: 'target/failsafe-reports/*.xml'
       }
     }
  }

  stage('Documentation')
   {
    steps
     {
      script
       {
         sh '''
             mvn --batch-mode site ${VERSIONING_OVERRIDE}
         '''
       }
     }
    post
     {
      always
       {
        publishHTML(target: [reportName: 'Site', reportDir: 'target/site', reportFiles: 'index.html', keepAll: false])
       }
     }
   }


  stage('Deploy and Tag')
   {
    steps
     {
      script
       {
         if(env.BRANCH_TYPE == "release") {
           sh '''
               mvn --batch-mode deploy scm:tag -DskipUTs -DskipITs ${VERSIONING_OVERRIDE}
           '''
         } else {
           sh '''
               mvn --batch-mode deploy -DskipUTs -DskipITs ${VERSIONING_OVERRIDE}
           '''
         }
       }
     }
   }

   /*
     stage('Tomcat Deploy Test')
      {
       steps
        {
         script
          {
           if (isUnix())
            {
             // todo
            }
           else
            {
             bat returnStatus: true, script: 'sc stop Tomcat8'
             sleep(time:30, unit:"SECONDS")
             bat returnStatus: true, script: 'C:\\scripts\\clean.bat'
             bat returnStatus: true, script: 'robocopy "target" "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps" Test.war'
             bat 'sc start Tomcat8'
             sleep(time:30, unit:"SECONDS")
            }
          }
        }
      }*/
 }

}
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
  buildDiscarder(logRotator(numToKeepStr: '10'))
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
          // Early abort if we run the pipeline on master.
          if(env.BRANCH_NAME == "master") {
              currentBuild.result = 'ABORTED'
              error('Master branch is not meant to be built. It act as a code reference of the last production version.')
          }

          def matcherRelease = env.BRANCH_NAME =~ /^release\/(.*)$/
          def matcherFeature = env.BRANCH_NAME =~ /^feature\/(.*)$/
          def matcherPr = env.BRANCH_NAME =~ /^pr\/(.*)$/

          if(matcherRelease.matches()) {
              // Release branches
              echo "RELEASE BRANCH DETECTED!"
              env.BRANCH_TYPE = "release"

              env.RELEASE_VERSION = matcherRelease[0][1]
              env.VERSIONING_OVERRIDE = "-Dbranch=" + env.RELEASE_VERSION + " -Drevision=.b$BUILD_NUMBER -Dmodifier="
          } else if(matcherFeature.matches()) {
              // Feature branches are tagged as release of a particular name, with build number in it.
              echo "FEATURE BRANCH DETECTED!"
              env.BRANCH_TYPE = "feature"

              env.FEATURE_NAME = matcherFeature[0][1]
              env.VERSIONING_OVERRIDE = "-Dbranch= -Dfeature=" + env.FEATURE_NAME  + " -Drevision=.b$BUILD_NUMBER -Dmodifier="
          } else if(matcherPr.matches()) {
              // Pull requests branches are NOT deployed, NOT tagged and NO documentation is generated. Only tests are runned.
              echo "PULL REQUEST BRANCH DETECTED!"
              env.BRANCH_TYPE = "pr"

              env.PR_NAME = matcherPr[0][1]
              env.VERSIONING_OVERRIDE = "-Dbranch=PR -Dfeature=." + env.PR_NAME  + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
          } else {
              echo "REGULAR BRANCH DETECTED"
              env.BRANCH_TYPE = "snapshot"

              env.VERSIONING_OVERRIDE = "-Dmodifier=-SNAPSHOT"
          }

          echo "  > VERSIONING_OVERRIDE settings: " + env.VERSIONING_OVERRIDE
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
         if(env.BRANCH_NAME == "develop") {
             echo "Not running integration tests on branch " + env.BRANCH_NAME
         } else {
             sh '''
                 mvn --batch-mode verify -DskipUTs -Dmaven.site.skip ${VERSIONING_OVERRIDE}
             '''
         }
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
         if ( env.BRANCH_TYPE == "pr") {
             echo "Not generating documentation for Pull Requests validation builds"
         } else {
             sh '''
                 mvn --batch-mode site ${VERSIONING_OVERRIDE}
             '''
         }
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
         if ( env.BRANCH_TYPE == "pr") {
             echo "Not deploying nor tagging for Pull Requests validation builds"
         } else {
             sh '''
                 mvn --batch-mode deploy scm:tag -DskipUTs -DskipITs ${VERSIONING_OVERRIDE}
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
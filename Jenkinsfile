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
          echo env.BRANCH_NAME

          // Early abort if we run the pipeline on master.
          if( env.BRANCH_NAME == "master" || env.BRANCH_NAME == "main" ) {
              currentBuild.result = 'ABORTED'
              error(env.BRANCH_NAME + ' branch is not meant to be built. It acts as a code reference of the latest production version.')
          }

          def matcherRelease = env.BRANCH_NAME =~ /^release\/(.*)$/
          def matcherFeature = env.BRANCH_NAME =~ /^feature\/(.*)$/
          def matcherPr = env.BRANCH_NAME =~ /^pr\/(.*)$/

          def versionOpts = ""
          def docOpts = ""
          env.DO_TAG = false

          if(matcherRelease.matches()) {
              // Release branches
              echo "RELEASE BRANCH DETECTED!"
              env.BRANCH_TYPE = "release"
              env.DO_TAG = true

              env.RELEASE_VERSION = matcherRelease[0][1]
              versionOpts = "-Dbranch=" + env.RELEASE_VERSION + " -Dfeature= -Drevision=.b$BUILD_NUMBER -Dmodifier="
          } else if(matcherFeature.matches()) {
              // Feature branches are tagged as snapshot of a particular name, with build number in it.
              echo "FEATURE BRANCH DETECTED!"
              env.BRANCH_TYPE = "feature"
              env.DO_TAG = true

              env.FEATURE_NAME = matcherFeature[0][1]
              versionOpts = "-Dbranch=FEAT -Dfeature=." + env.FEATURE_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
              docOpts = "-Dmaven.site.skip"
          } else if(matcherPr.matches()) {
              // Pull requests branches are NOT deployed, NOT tagged and NO documentation is generated. Only tests are run.
              echo "PULL REQUEST BRANCH DETECTED!"
              env.BRANCH_TYPE = "pr"

              env.PR_NAME = matcherPr[0][1]
              versionOpts = "-Dbranch=PR -Dfeature=." + env.PR_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
              docOpts = "-Dmaven.site.skip"
          } else {
              echo "OTHER BRANCH DETECTED"
              env.BRANCH_TYPE = "other"

              versionOpts = "-Dbranch=" + env.BRANCH_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
              docOpts = "-Dmaven.site.skip"
          }

          echo "  > versioning settings: " + versionOpts
          echo "  > documentation settings: " + docOpts

          env.MAVEN_ARGS = versionOpts + " " + docOpts


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
              mvn --batch-mode clean ${MAVEN_ARGS}
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
             mvn --batch-mode package -DskipUTs -DskipITs -Dmaven.site.skip ${MAVEN_ARGS}
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
             mvn --batch-mode verify -DskipITs -Dmaven.site.skip ${MAVEN_ARGS}
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
             mvn --batch-mode verify -DskipUTs -Dmaven.site.skip ${MAVEN_ARGS}
         '''
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
             mvn --batch-mode site ${MAVEN_ARGS}
         '''
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
         if ( env.DO_TAG ) {
             sh '''
                 mvn --batch-mode deploy scm:tag -DskipUTs -DskipITs ${MAVEN_ARGS}
             '''
         } else {
             echo "Skip and doc tag for this kind of branch: " + env.BRANCH_TYPE

             sh '''
                 mvn --batch-mode deploy -DskipUTs -DskipITs -Dmaven.site.skip ${MAVEN_ARGS}
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
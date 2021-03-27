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
          sh '''
              export isRelease=$(echo "${BRANCH}" | grep -qE "^release/.*$" && echo 'yes' || echo 'no')
              echo $isRelease

              if [ "${isRelease}" = "yes" ]
              then
                  export relbr=`echo ${BRANCH} | sed -E '/release\\/(.*)/g'`
                  echo "WE ARE CURRENTLY BUILDING RELEASE BRANCH for version ${relbr}"
              else
                  echo "NOT IN A RELEASE BRANCH, SO NO RELEASE IS GOING TO BE BUILT"
              fi
          '''
       }
     }
   }

  stage('Clean')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode clean'
       }
     }
   }

  stage('Build')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode package -DskipUTs -DskipITs -Dmaven.site.skip'
       }
     }
   }


  stage('Unit Tests')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode verify -DskipITs -Dmaven.site.skip'
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
         sh 'mvn --batch-mode verify -DskipUTs -Dmaven.site.skip'
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
         sh 'mvn --batch-mode site'
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
          sh '''
              # check for release branch
              if [[ "${BRANCH}" == release/* ]]
              then
                  echo "RELEASE BRANCH DETECTED"
                  brid = ${BRANCH#*/}
                  echo "branch version id: $brid"
                  mvn --batch-mode deploy scm:tag -Dbranch=$brid -Drevision=$BUILD_NUMBER -Dmodifier= -DskipUTs -DskipITs -Dmaven.site.skip
              else
                  echo "STANDARD BRANCH DETECTED"
                  mvn --batch-mode deploy scm:tag -Drevision=$BUILD_NUMBER -DskipUTs -DskipITs -Dmaven.site.skip
              fi
          '''
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
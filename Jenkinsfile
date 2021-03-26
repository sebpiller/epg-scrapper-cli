pipeline
{
agent any

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
          sh 'mvn --batch-mode compile'
       }
     }
   }


  stage('Unit Tests')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode resources:testResources compiler:testCompile surefire:test'
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
         sh 'mvn --batch-mode failsafe:integration-test@integration-test failsafe:verify@verify-it -X'
      }
    }
  }

  stage('Sanity check')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode checkstyle:checkstyle pmd:pmd pmd:cpd com.github.spotbugs:spotbugs-maven-plugin:spotbugs'
       }
     }
   }

  stage('Packaging')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode jar:jar'
       }
     }
   }

  stage('Install')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode jar:jar source:jar install:install'
       }
     }
   }

   stage('Check for updates')
      {
       steps
        {
         script
          {
             sh 'mvn --batch-mode org.codehaus.mojo:versions-maven-plugin:display-property-updates'
             sh 'mvn --batch-mode org.codehaus.mojo:versions-maven-plugin:display-dependency-updates'
             sh 'mvn --batch-mode org.codehaus.mojo:versions-maven-plugin:display-plugin-updates'
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

  stage('Deploy to Distribution Management')
   {
    steps
     {
      script
       {
          sh 'mvn --batch-mode deploy:deploy'
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
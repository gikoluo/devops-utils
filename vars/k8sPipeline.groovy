def call(Map config) {
  // Jenkinsfile
  def projectName = config.PROJECT_NAME   //Project name, Usually it is the name of jenkins project folder name.
  def serviceName = config.SERVICE_NAME   //Service name. Usually it is the process name running in the server.
  //def archiveFile = config.ARCHIVE_FILE
  //def branchName = env.BRANCH_NAME     //Branch name. And the project must be multibranch pipeline, Or set the env in config
  def branchName
  
  def deploymentName = config.DEPLOYMENT_NAME
  def containerName = config.CONTAINER_NAME
  def deployNamespace = config.PROJECT_NAME

  def hubCredential="dockerhub"
  def enableQA = false
  def k8sNS="devops"

  def namespace = "swr.cn-east-2.myhuaweicloud.com"
  def org = "greenland"
  def imageName
  def version = 0
  def tag
  def archiveFlatName 
  def versionImage
  def sourceImage
  //def envs = ['dev','test','uat', 'prd']
  def envs = ['common']
  def packageArgs = " -Dmaven.test.skip=true"
  def timeFlag = new Date().format("yyyyMMdd-hhmm")

  def sonarExtendsParams = "-Dsonar.sources=./src/main/java/ -Dsonar.java.binaries=./target/classes"

  if(env.SONAR_EXTENDS_PARAMS) {
      sonarExtendsParams = env.SONAR_EXTENDS_PARAMS
  }

  if( env.ENABLE_QA ) {
      enableQA = !! env.ENABLE_QA
  }

  podTemplate(
    cloud: 'kubernetes', 
    workspaceVolume: persistentVolumeClaimWorkspaceVolume(readOnly: false, claimName: 'cce-sfs-devops-jenkins'),
    containers: [
      containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
      containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.14.6', command: 'cat', ttyEnabled: true),
      containerTemplate(name: 'sonar', image: 'newtmitch/sonar-scanner', command: 'cat', ttyEnabled: true)
    ],
    volumes: [
      // hostPathVolume(mountPath: '/home/jenkins/.m2', hostPath: '/root/.m2'),
      hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
    ]
  ) {

    node(POD_LABEL) {

      stage('Init') {
        def checkoutResults = checkout scm: scm

        //echo 'checkout results' + checkoutResults.toString()
        // echo 'checkout revision' + checkoutResults['SVN_REVISION']

        // echo 'scm: ' + scm.toString()

        // echo checkoutResults.SVN_REVISION

        // echo checkoutResults.GIT_COMMIT

        // version = checkoutResults.GIT_COMMIT || checkoutResults.SVN_REVISION

        if (scm instanceof hudson.plugins.git.GitSCM) {
          sh 'git rev-parse HEAD > commit'
          
        }
        else if (scm instanceof hudson.scm.SubversionSCM) {
          sh 'echo ${BUILD_NUMBER} > commit'
        }

        version = readFile('commit').trim()

        imageName = "${projectName}-${serviceName}"
        // version = readFile('commit').trim()
        tag = "${namespace}/${org}/${imageName}"

        container('docker') {
          withCredentials([[$class: 'UsernamePasswordMultiBinding',
            credentialsId: "${hubCredential}",
            usernameVariable: 'DOCKER_HUB_USER',
            passwordVariable: 'DOCKER_HUB_PASSWORD']]) {
            sh """
              docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD} ${namespace}
              """
          }
        }
      }

      stage('Build image') {
        container('docker') {
          try {
              sourceImage = docker.build("${tag}:build_stage", "--target build_stage .")

              versionImage = docker.build("${tag}:${version}")
          }
          catch (exc) {
            sh """
              docker rmi ${tag}:${version}  || echo "clean up build tag"  #${tag}:build_stage

            """
            throw exc
          }
        }
      }

      // stage('Prepare Configmaps') {
      //   steps {
      //     container('docker') {
      //       script {
      //         def configFile = "./target/classes/application.properties"

      //         sourceImage.inside {
      //           envs.each{ 
      //             print "${it}," 
      //             sh "mvn ${packageArgs} package -Dmaven.test.skip=true -Denv=${it}"
      //             stash name:"${imageName}-config-${it}", includes: targetFile
      //           }
      //         }
      //       }
      //     }

      //     container('kubectl') {
      //       script {
      //         def configFile = "./target/classes/application.properties"

      //         envs.each{

      //           dir("/tmp/${BUILD_ID}/") {
      //             script.deleteDir()
      //             script.unstash "${imageName}-config-${it}"
      //             sh "kubectl create configmap ${imageName}-config-${it} --from-file $configFile -o yaml --dry-run | kubectl apply -f -"
      //           }
      //         }
      //       }
      //     }
      //   }
      // }


      stage('SonarQube analysis') {
        container('docker') {
          echo "Run SonarQube Analysis"
          if( enableQA.toBoolean() ) {
            //docker run -ti -v $(pwd):/root/src --entrypoint='' newtmitch/sonar-scanner sonar-scanner -Dsonar.host.url=http://docker.for.mac.host.internal:9000 -X
            //def image = docker.image("nikhuber/sonar-scanner:latest")
            //def image = docker.build("${tag}:sonarqube", "--target build_stage")
            sourceImage.inside {
              //sh "sonar-scanner -Dsonar.host.url=http://docker.for.mac.host.internal:9000 || echo 'Snoar scanner failed';"

              //withSonarQubeEnv('SonarQubeServer') {
                sh """
                mvn ${packageArgs} package sonar:sonar \
                  -Dsonar.host.url=http://devops-sonarqube-sonarqube:9000 \
                  ${sonarExtendsParams}
                """
              //}
            }
            //def scannerHome = tool 'SonarScanner 4.0';
            // image.inside {
            //     sh 'make test'
            // }

            // sh """
            // docker build --target sonarqube -t ${tag}:sonarqube .
            // docker push ${tag}:sonarqube
            // """

            //docker run -ti -v $(pwd):/root/src --link sonarqube newtmitch/sonar-scanner sonar-scanner sonar.host.url=YOURURL -Dsonar.projectBaseDir=./src


            // withSonarQubeEnv('SonarQubeServer') { // If you have configured more than one global server connection, you can specify its name
            //   def image = docker.build("${tag}:sonarqube", "--target build_stage .")
            //   image.inside {  //docker inside changed the workdir to project home. so cd /build is required
            //     sh "pwd"
              
            //     sh 'mvn package sonar:sonar -Dsonar.host.url=http://docker.for.mac.host.internal:9000'
            //     //sh "${scannerHome}/bin/sonar-scanner"
            //     // withMaven(maven:'Maven 3.6') {
            //     //       sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar -Dsonar.host.url=http://docker.for.mac.host.internal:9000'
            //     //   }
            //   }
            //   //sh """
            //   //cat sonar-project.properties;
            //   //sonar-scanner -Dsonar.host.url=http://docker.for.mac.host.internal:9000 || echo 'Snoar scanner failed';
            //   //"""
            // }
          }
          else {
            echo "Skipped QA."
          }
        }
      }

      stage('Archive Target File & push image') {
        container('docker') {
          echo "Extract the Archive File"
          sourceImage.inside {
            sh "env"
            archiveFile = sh (
                script: "echo \${ARCHIVE_FILE}",
                returnStdout: true
            ).trim()
            if (archiveFile == "") {
              echo "ARCHIVE_FILE does not defined. skipped."
            }
            else {
              archiveFlatName = sh (
                  script: "basename \${ARCHIVE_FILE}",
                  returnStdout: true
              ).trim()

              sh "cp \${ARCHIVE_FILE} ${WORKSPACE}/${archiveFlatName}"
              archiveArtifacts "${archiveFlatName}"
              sh "rm ${WORKSPACE}/${archiveFlatName}"
            }
          }
          versionImage.push()
        }
      }

      stage('Deploy To Test') {
        if ( env.BRANCH_NAME ) {  // In multiple branch deploy
          container('docker') {
            sh """
                docker tag ${tag}:${version} ${tag}-test:test-${timeFlag}
                docker push ${tag}-test:test-${timeFlag}
                """
          }

          container('kubectl') {
            withKubeConfig([credentialsId: 'kubeconfig-uat']) {  //USE UAT cluster in test Enviroment, seperate by namespace
              sh "kubectl set image ${deploymentName} ${containerName}=${tag}-test:test-${timeFlag} --namespace=${deployNamespace}-test"
            }
            echo "The service is Deployed in TEST"
          }

          if ( ! (env.BRANCH_NAME == "trunk" || env.BRANCH_NAME == "master" ) ) {
            echo "The lifecycle of branches is teminaled in TEST."
            echo '[FAILURE] Failed to build'
            currentBuild.result = 'SUCCESS'
            return
          } 
        }
        else {
          echo "SKIP TEST."
        }
      }

      stage('Deploy To UAT') {
        container('docker') {
          sh """
              docker tag ${tag}:${version} ${tag}:uat-${timeFlag}
              docker push ${tag}:uat-${timeFlag}
              docker rmi ${tag}:${version}
              """
        }
        try {
          timeout(time:1, unit:'DAYS') {
            def submitter = "test,sa,scm,publisher"
            input message: "可以发布 UAT 了吗?", ok: '可以了，发布！', submitter: submitter
          }
        } catch(err) { // timeout reached or input false
            container('docker') {
              sh """
                  docker rmi ${tag}:uat-${timeFlag}
              """
            }
            throw err
        }

        container('kubectl') {
          withKubeConfig([credentialsId: 'kubeconfig-uat']) {
            sh "kubectl set image ${deploymentName} ${containerName}=${tag}:uat-${timeFlag} --namespace=${deployNamespace}"
          }
        }
      }


      stage('Deploy To Production') {
        container('docker') {
          sh """
              docker tag ${tag}:uat-${timeFlag} ${tag}:prod-${timeFlag}
              docker push ${tag}:prod-${timeFlag}
              #docker rmi ${tag}:uat-${timeFlag} ${tag}:prod-${timeFlag}
              """
        }
        timeout(time:1, unit:'DAYS') {
          def submitter = "sa,scm,publisher"
          input message: "可以发布 PROD 了吗?", ok: '可以了，发布！', submitter: submitter
        }

        container('kubectl') {
          withKubeConfig([credentialsId: 'kubeconfig-prod']) {
            sh "kubectl set image ${deploymentName} ${containerName}=${tag}:prod-${timeFlag} --namespace=${deployNamespace}"
          }
        }
      }

      // post {
      //   always {
      //       echo 'cleanup'
      //       container('docker') {
      //         sh """
      //             docker rmi ${tag}:${version} ${tag}:uat-${timeFlag} ${tag}:prod-${timeFlag} || echo "clean up finished"
      //             """
      //       }
      //   }
      //   success {
      //       echo 'I succeeeded!'
      //   }
      //   unstable {
      //       echo 'I am unstable :/'
      //   }
      //   failure {
      //       echo 'I failed :('
      //   }
      //   changed {
      //       echo 'Things were different before...'
      //   }
      // }
    }
  }
}

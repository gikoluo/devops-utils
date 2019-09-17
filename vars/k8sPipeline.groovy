def call(Map config) {
  // Jenkinsfile
  def projectName = config.PROJECT_NAME   //Project name, Usually it is the name of jenkins project folder name.
  def serviceName = config.SERVICE_NAME   //Service name. Usually it is the process name running in the server.
  def archiveFile = config.ARCHIVE_FILE
  //def branchName = env.BRANCH_NAME     //Branch name. And the project must be multibranch pipeline, Or set the env in config
  def branchName
  
  def deploymentName = config.DEPLOYMENT_NAME
  def containerName = config.COMTAINER_NAME
  def deplayNamespace = config.PROJECT_NAME

  def hubCredential=env.HUB_CREDENTIAL
  def skipQA = env.SKIP_QA
  def k8sNS="devops"

  def namespace = "swr.cn-east-2.myhuaweicloud.com"
  def org = "greenland"
  def imageName
  def version 
  def tag
  def archiveFlatName 
  def versionImage
  def sourceImage
  //def envs = ['dev','test','uat', 'prd']
  def envs = ['common']
  def packageArgs = " -Dmaven.test.skip=true"

  pipeline {
    agent {
      kubernetes {
        // this label will be the prefix of the generated pod's name
        //label '${projectName}-${serviceName}'
        defaultContainer 'jnlp'
        yaml """
  apiVersion: v1
  kind: Pod
  metadata:
    labels:
      component: ci
  spec:
    containers:
    - name: jnlp
      image: 'jenkins/jnlp-slave:3.27-1-alpine'
      args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    - name: docker
      image: docker
      command:
      - cat
      tty: true
      volumeMounts:
        - mountPath: /var/run/docker.sock
          name: docker-sock
    - name: kubectl
      image: lachlanevenson/k8s-kubectl:v1.14.6
      command:
        - cat
      tty: true
    volumes:
      - name: docker-sock
        hostPath:
          path: /var/run/docker.sock
  """
      }
    }

    stages {
      stage('Init') {
        steps {
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

          // container('kubectl') {
          //   withKubeConfig([credentialsId: 'kubeconfig-uat']) {
          //     sh 'kubectl get namespaces'
          //     sh "kubectl config set-context --current --namespace=${k8sNS}-uat"
          //   }
          // }

          script {
            sh 'git rev-parse HEAD > commit'

            imageName = "${projectName}-${serviceName}"
            version = readFile('commit').trim()
            tag = "${namespace}/${org}/${imageName}"

            archiveFlatName = sh (
                script: "basename ${archiveFile}",
                returnStdout: true
            ).trim()
          }
        }
      }

      stage('Build image') {
        steps {
          container('docker') {
            script {
              versionImage = docker.build("${tag}:${version}")

              sourceImage = docker.build("${tag}:build_stage", "--target build_stage .")
            }
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
        steps {
          container('docker') {
            echo "Run SonarQube Analysis"
            script {
              if(! skipQA) {
                //docker run -ti -v $(pwd):/root/src --entrypoint='' newtmitch/sonar-scanner sonar-scanner -Dsonar.host.url=http://docker.for.mac.host.internal:9000 -X
                //def image = docker.image("nikhuber/sonar-scanner:latest")
                //def image = docker.build("${tag}:sonarqube", "--target build_stage")
                sourceImage.inside {
                  //sh "sonar-scanner -Dsonar.host.url=http://docker.for.mac.host.internal:9000 || echo 'Snoar scanner failed';"

                  //withSonarQubeEnv('SonarQubeServer') {
                    sh """
                    mvn ${packageArgs} package sonar:sonar \
                      -Dsonar.host.url=http://docker.for.mac.host.internal:9000 \
                      -Dsonar.sources=./src/main/java/ \
                      -Dsonar.java.binaries=./target/classes
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
        }
      }

      stage('Archive Target File & push image') {
        steps {
          container('docker') {
            echo "Extract the Archive File : ${archiveFile} to ${archiveFlatName}"

            script {
              //def image = docker.image("${tag}:${version}")
              versionImage.inside {
                sh "cp ${archiveFile} ${WORKSPACE}/${archiveFlatName}"
                archiveArtifacts "${archiveFlatName}"
              }
              versionImage.push()
            }
          }
        }
      }

      

      stage('Deploy To UAT') {
        steps {
          container('docker') {
            sh """
                docker tag ${tag}:${version} ${tag}:uat
                docker push ${tag}:uat
                """
            // withCredentials([[$class: 'UsernamePasswordMultiBinding',
            //   credentialsId: "${hubCredential}",
            //   usernameVariable: 'DOCKER_HUB_USER',
            //   passwordVariable: 'DOCKER_HUB_PASSWORD']]) {
            //   sh """
            //     docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD} ${namespace}
            //     docker tag ${tag} ${tag_uat}
            //     docker push ${tag_uat}
            //     """

            //   script {
            //     tag_uat = "${namespace}/${org}/${imageName}:uat"

            //     def image = docker.image("${tag}")
            //     image.inside {
            //       sh "cp ${archiveFile} ${WORKSPACE}/${archiveFlatName}"
            //       archiveArtifacts "${archiveFlatName}"
            //     }
            //   }
            // }
          }

          container('kubectl') {
            withKubeConfig([credentialsId: 'kubeconfig-uat']) {
              sh "kubectl set image ${deploymentName} ${containerName}=${tag}:uat --namespace=${deplayNamespace}"
            }
            //kubectl set image deployment/my-deployment mycontainer=myimage:latest

            //  sh "kubectl version"
            // sh "kubectl delete -f ./kubernetes/deployment.yaml"
            // sh "kubectl apply -f ./kubernetes/deployment.yaml"
            // sh "kubectl apply -f ./kubernetes/service.yaml"
          }
        }
      }


      stage('Deploy To Production') {
        steps {
          container('docker') {
            sh """
                docker tag ${tag}:uat ${tag}:prod
                docker push ${tag}:prod
                """
            // withCredentials([[$class: 'UsernamePasswordMultiBinding',
            //   credentialsId: "${hubCredential}",
            //   usernameVariable: 'DOCKER_HUB_USER',
            //   passwordVariable: 'DOCKER_HUB_PASSWORD']]) {
            //   sh """
            //     docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD} ${namespace}
            //     docker tag ${tag} ${tag_uat}
            //     docker push ${tag_uat}
            //     """
            //docker login -u cn-east-2@$AK -p 153690a09016d5e98aae063039d305e3df3fb3756649862b1c0c0955bab12f06 swr.cn-east-2.myhuaweicloud.com



            //   script {
            //     tag_uat = "${namespace}/${org}/${imageName}:uat"

            //     def image = docker.image("${tag}")
            //     image.inside {
            //       sh "cp ${archiveFile} ${WORKSPACE}/${archiveFlatName}"
            //       archiveArtifacts "${archiveFlatName}"
            //     }
            //   }
            // }
          }

          // container('kubectl') {
          //   withKubeConfig([credentialsId: 'kubeconfig-prod']) {
          //     sh 'kubectl get namespaces'
          //     sh 'kubectl apply -f ./kubernetes/deployment.yaml'
          //   }
          //   //  sh "kubectl version"
          //   // sh "kubectl delete -f ./kubernetes/deployment.yaml"
          //   // sh "kubectl apply -f ./kubernetes/deployment.yaml"
          //   // sh "kubectl apply -f ./kubernetes/service.yaml"
          // }
        }
      }
    }
  }
}
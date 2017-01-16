#!groovy 
//filename: Deploy.groovy

//--Part1. Include the library and make utils.
@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)

//--Part2. Get the variables from Jenkins job settings.
def projectName = env.PROJECT_NAME   //Project name, Usually it is the name of jenkins project folder name.
def serviceName = env.SERVICE_NAME   //Service name. Usually it is the process name running in the server.
def buildJob    = env.BUILD_JOB      //Build job in jenkins. 
def targetFile = env.TARGET_FILE     //Build target.
def autoBuild  = false               //set it to true if you like to build the build job manually.
def playbook = "";                   //the playbook script file to deploy target, default is "${projectName}/${serviceName}"


if(env.PLAYBOOK) {
    playbook = env.PLAYBOOK
}
else {
    playbook = "${projectName}/${serviceName}"
}

if(env.AUTO_BUILD) {
    autoBuild = env.AUTO_BUILD
}


echo "Deploy ${projectName}/${serviceName} with ${playbook}"
echo "buildJob: ${buildJob}"
echo "targetFile: ${targetFile}"
echo "autoBuild: ${autoBuild}"

//--Part3. workflow for deploy.
milestone 1
stage('Copy Target') {
    if(autoBuild) {
        build job: buildJob
    }
    node('master') {
        utils.copyTarget(buildJob, targetFile)
    }
}

milestone 2
stage('QA') {
    utils.qaCheck()
}

milestone 3
stage('Test') {
    remote = new Remote(steps, 'test')
    remote.deployProcess(playbook, targetFile, BUILD_ID)
}

milestone 4
stage('UAT') {
    remote = new Remote(steps, 'uat')
    remote.deployProcess(playbook, targetFile, BUILD_ID)
}

milestone 5
stage ('Production') {
    remote = new Remote(steps, 'prod')
    remote.deployProcess(playbook, targetFile, BUILD_ID)
}



#!groovy 

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote


def utils = new Utilities(steps)


def projectName = env.PROJECT_NAME
def serviceName = env.SERVICE_NAME


def buildJob = env.BUILD_JOB

def targetFile = env.TARGET_FILE
def autoBuild = false

echo "${projectName}"

// echo build.environment.get("PROJECT_NAME")
// echo build.buildVariableResolver.resolve("PROJECT_NAME")

def playbook = "";

if(env.PLAYBOOK) {
    playbook = env.PLAYBOOK
}
else {
    playbook = "${projectName}/${serviceName}"
}

if(env.AUTO_BUILD) {
    autoBuild = env.AUTO_BUILD
}


def archivePublisher = true

def testLinks = [
  "test": "https://bbctest.91dbq.com:8443/",
  "uat": "https://bbcuat.91dbq.com:8443/",
  "prod": "https://bbc.91dbq.com:8443/"
]


milestone 1
stage('Copy Target') {
    if(autoBuild) {
        build job: buildJob
    }
    node('master') {
        utils.copyTarget(buildJob, targetFile, archivePublisher)
    }
}


milestone 2
stage('QA') {
    utils.qaCheck()
}

milestone 3
stage('Test') {
    remote = new Remote(steps, 'test')
    remote.deployProcess (playbook, targetFile, BUILD_ID)
}

milestone 4
stage('UAT') {
    remote = new Remote(steps, 'prod')
    remote.deployProcess (playbook, targetFile, BUILD_ID)
}


milestone 5
stage ('Production') {
    remote = new Remote(steps, 'prod')
    remote.deployProcess (playbook, targetFile, BUILD_ID)
}



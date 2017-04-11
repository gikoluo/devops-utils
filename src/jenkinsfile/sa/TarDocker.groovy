#!groovy

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)

//def targetFile="dist/rhasta.1.1.0.tar.gz"

def targetFile = env.TARGET_FILE     //Build target.
def buildJob    = env.BUILD_JOB      //Build job in jenkins. 
def target    = env.TARGET      //Build job in jenkins. 

def ask_permission = false

//def buildProjectName = "builds/Rhasta"
playbook= "ansible"
remoteUser="ops"
test_url = ""
uat_url = ""
prod_url = ""
def autoBuild = true

milestone 1
stage('Dev') {
    node('master') {
        if(autoBuild) {
            build job: buildJob
        }
        utils.copyTarget(buildJob, targetFile, true)
    }
}


/*
milestone 3
stage('Quick Test') {
    node {
        deploy( targetFile, playbook, 'uat' )
    }
}
*/


milestone 2
stage('Deploy Docker') {
    node("docker-manager") {
        remote = new Remote(steps, 'test', remoteUser)
        remote.deployTarGz( targetFile, target, BUILD_ID)
    }
}








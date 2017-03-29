#!groovy

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)

//def targetFile="dist/rhasta.1.1.0.tar.gz"

def targetFile = env.TARGET_FILE     //Build target.
def buildJob    = env.BUILD_JOB      //Build job in jenkins. 
def target    = env.TARGET      //Build job in jenkins. 

def ask_permission = False

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
stage('Deploy Test') {
    node("ansible-test") {
        remote = new Remote(steps, 'test', remoteUser)
        remote.deployTarGz( targetFile, target)
    }
}

stage('UAT') {
    lock(resource: "${target}-UAT", inversePrecedence: true) {
        milestone 3
        ask_permission && timeout(time:1, unit:'DAYS') {
            input id: 'pushToUAT', message: "Test环境正常了么？可以提交 UAT 了吗?", ok: '准备好了，发布！', submitter: 'scm,sa'
        }

        node("ansible-uat") {
            echo 'UAT deploy start'
            remote = new Remote(steps, 'uat', remoteUser)
            remote.deployTarGz( targetFile, target)
            echo "UAT deployed"
        }
        
        ask_permission && timeout(time:1, unit:'DAYS') {
            input message: " UAT 通过了吗? ${uat_url} ", ok: '通过！', submitter: 'scm,sa'
        }
    }
}


milestone 4
stage ('Production') {
    lock(resource: "${target}-Production", inversePrecedence: true) {
        ask_permission && timeout(time:1, unit:'DAYS') {
            input message: "可以提交 Prod 了吗?", ok: '准备好了，发布！', submitter: 'scm,sa'
        }
        
        node("ansible-prod") {
            echo 'Production deploy status'
            remote = new Remote(steps, 'prod', remoteUser)
            remote.deployTarGz( targetFile, target)
            echo "Production deployed"
        }
        
        ask_permission && timeout(time:1, unit:'DAYS') {
            input message: "Prod测试完成了吗? ${prod_url} ", ok: '通过！下班，困觉！', submitter: 'scm,sa'
        }
    }
}








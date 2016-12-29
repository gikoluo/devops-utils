#!groovy 

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote


def utils = new Utilities(steps)


sh "echo 'Deploying to ${DeployEnvironment.toLowerCase()}.example.com'"
def projectName = env.PROJECT_NAME
def serviceName = env.SERVICE_NAME
def targetFile = env.TARGET_FILE

echo build.environment.get("PROJECT_NAME")
echo build.buildVariableResolver.resolve("PROJECT_NAME")


def playbook = build.environment.get("PLAYBOOK", "${projectName}/${serviceName}")

//def playbook = "${projectName}/${serviceName}"
def buildJob = "${projectName}/builds/${serviceName}_all"
//def playbook = "openpay/cotton-candy-channel-provider"
//def buildProjectName = "OPEN_PAY/builds/candy"
//def targetFile = "cotton-candy/cotton-candy-channel-provider/target/cotton-candy-channel-provider-1.0.tar.gz"
def archivePublisher = true

def testLinks = [
  "test": "https://bbctest.91dbq.com:8443/",
  "uat": "https://bbcuat.91dbq.com:8443/",
  "prod": "https://bbc.91dbq.com:8443/"
]


milestone 1
stage('Copy Target') {
    node {
        utils.copyTarget(buildProjectName, targetFile, archivePublisher)
    }
}


milestone 2
stage('QA') {
    utils.qaCheck()
}

milestone 3
stage('Test') {
    node {
        remote = new Remote(steps, 'test')
        remote.deploy (playbook, targetFile, BUILD_ID)
    }
}

milestone 4
stage('UAT') {
    timeout(time:1, unit:'DAYS') {
        input message: "Test环境 ${testLinks.get('test', '')} 正常了么？可以提交 UAT 了吗?", ok: '准备好了，发布！'
    }
    lock(resource: "${playbook}-staging-server", inversePrecedence: true) {
        node {
            echo 'UAT deploy start'
            remote = new Remote(steps, 'uat')
            remote.deploy (playbook, targetFile, BUILD_ID)
        }
    }
    timeout(time:1, unit:'DAYS') {
        input message: " UAT 通过了吗? ${testLinks.get('uat', '')} ", ok: '通过！'
    }
}


milestone 5
stage ('Production') {
    timeout(time:1, unit:'DAYS') {
        input message: "可以提交 Prod 了吗?", ok: '准备好了，发布！'
    }
    lock(resource: "${playbook}-production-server", inversePrecedence: true) {
        node {
            echo 'Production deploy status'
            remote = new Remote(steps, 'prod')
            remote.deploy (playbook, targetFile, BUILD_ID)
            echo "Production deployed"
        }
    }
    timeout(time:1, unit:'DAYS') {
        input message: "Prod测试完成了吗? ${testLinks.get('prod', '')} ", ok: '通过！下班，困觉！'
    }
}



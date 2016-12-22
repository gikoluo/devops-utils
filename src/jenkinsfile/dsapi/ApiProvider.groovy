#!groovy 

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)
def remoteUser = "ops"
def playbook = "dspay/biz"
def buildProjectName = "ds-biz-provider"
def needArchive = true

def targetFile = "target/ds-biz-1.0.tar.gz"

def testLinks = [
  "test": "https://bbctest.91dbq.com:8443/",
  "uat": "https://bbcuat.91dbq.com:8443/",
  "prod": "https://bbc.91dbq.com:8443/"
]



milestone 1
stage('Dev') {
    node {
        step([$class: 'hudson.plugins.copyartifact.CopyArtifact',
                 filter: targetFile, 
                 fingerprintArtifacts: true, 
                 projectName: buildProjectName
            ])

        if ( needArchive ) {
            archiveArtifacts artifacts:targetFile, fingerprint: true
        }

        stash name:'targetArchive', includes: targetFile
    }
}


milestone 2
stage('QA') {

    parallel( 'quality scan': {
        
    },
    'integration tests': {
        
    }, 'functional-tests': {
    /*
        node('selenium'){ 
        }
    */
    }, 'codecheck': {
    /*
        node('selenium'){
        }
    */
    })
    
    /*
    parallel 'quality scan': { node {sh 'mvn sonar:sonar'} }, 'integration test': { node {sh 'mvn verify'} }
    */
}

milestone 3
stage('Test') {
    node {
        remote = new Remote(steps, 'test', remoteUser)
        remote.deploy (playbook, targetFile, BUILD_ID, 'update')
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
            remote = new Remote(steps, 'uat', remoteUser)
            remote.deploy (playbook, targetFile, BUILD_ID, 'update')
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
            remote = new Remote(steps, 'prod', remoteUser)
            remote.deploy (playbook, targetFile, BUILD_ID, 'update')
            echo "Production deployed"
        }
    }
    timeout(time:1, unit:'DAYS') {
        input message: "Prod测试完成了吗? ${testLinks.get('prod', '')} ", ok: '通过！下班，困觉！'
    }
}



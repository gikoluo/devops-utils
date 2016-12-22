#!groovy 

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)
def playbook = "dsbbc/bbcweb"
def buildProjectName = "builds/Backend"
def targetFile = 'target/app.war'
def tags = ["update"]
def archivePublisher = true

def testLinks = [
  "test": "https://bbctest.91dbq.com:8443/be/",
  "uat": "https://bbcuat.91dbq.com:8443/be/",
  "prod": "https://bbc.91dbq.com:8443/be/"
]

tags << "backend"

milestone 1
stage('Dev') {
    node {
        dir(".") {
            step([$class: 'hudson.plugins.copyartifact.CopyArtifact',
                 filter: targetFile, 
                 fingerprintArtifacts: true, 
                 projectName: buildProjectName
            ])
            if ( archivePublisher ) {
                archiveArtifacts artifacts:targetFile, fingerprint: true
            }
            stash name:'targetArchive', includes: targetFile
        }
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
        remote = new Remote(steps, 'test')
        remote.deploy (playbook, targetFile, BUILD_ID, tags)
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
            remote.deploy (playbook, targetFile, BUILD_ID, tags)
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
            remote.deploy (playbook, targetFile, BUILD_ID, tags)
            echo "Production deployed"
        }
    }
    timeout(time:1, unit:'DAYS') {
        input message: "Prod测试完成了吗? ${testLinks.get('prod', '')} ", ok: '通过！下班，困觉！'
    }
}



#!groovy 

@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)
def playbook = "dsbbc/bbcweb"
def buildProjectName = "builds/Platform"
def targetFile = '' // 'jifu-platform-server/target/jifu-platform-server-1.0.1-SNAPSHOT.jar'
def archivePublisher = true
def tags = ["update"]
def targetSettings = [
    site:   'jifu_site/target/jifu_site-0.0.1-SNAPSHOT.war', 
    mobile: 'jifu_mobile_site/target/jifu_mobile_site-0.0.1-SNAPSHOT.war', 
    boss:   'jifu_newboss_site/target/jifu_boss_site-0.0.1-SNAPSHOT.war',
    third:  'jifu_newthird_site/target/jifu_third_site-0.0.1-SNAPSHOT.war'
]


def testLinks = [
  "test": "https://bbctest.91dbq.com:8443/",
  "uat": "https://bbcuat.91dbq.com:8443/",
  "prod": "https://bbc.91dbq.com:8443/"
]

milestone 1
stage('Dev') {
    def userInput = input(
        id: 'userInput', 
        message: '请选择需要上线的子系统', 
        ok: '开始发布', 
        parameters: [
          [$class: 'ChoiceParameterDefinition', choices: 'site\nmobile\nboss\nthird', description: '', name: 'target'],
          [$class: 'BooleanParameterDefinition', defaultValue: false, description: '重启？', name: 'allowRestart']
        ], 
        submitter: 'qa,dev'
    )

    tags << userInput['target']

    targetFile = targetSettings[userInput['target']]

    node('master') {
        utils.copyTarget(buildProjectName, targetFile, archivePublisher)
    }


    // node {
    //     dir(".") {
    //         step([$class: 'hudson.plugins.copyartifact.CopyArtifact',
    //              filter: targetFile, 
    //              fingerprintArtifacts: true, 
    //              projectName: buildProjectName
    //         ])
    //         if ( archivePublisher ) {
    //             archiveArtifacts artifacts:targetFile, fingerprint: true
    //         }
    //         stash name:'targetArchive', includes: targetFile
    //     }
    // }
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
    remote = new Remote(steps, 'test')
    remote.deployProcess(playbook, targetFile, BUILD_ID, tags)
}

milestone 4
stage('UAT') {
    remote = new Remote(steps, 'uat')
    remote.deployProcess(playbook, targetFile, BUILD_ID, tags)
}


milestone 5
stage ('Production') {
    remote = new Remote(steps, 'prod')
    remote.deployProcess(playbook, targetFile, BUILD_ID, tags)
}




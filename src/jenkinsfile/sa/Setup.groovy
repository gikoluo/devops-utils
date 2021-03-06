#!groovy 
//filename: Deploy.groovy

//--Part1. Include the library and make utils.
@Library('luochunhui')

import com.luochunhui.lib.Utilities
import com.luochunhui.lib.Remote

def utils = new Utilities(steps)


//--Part2. Get the variables from Jenkins job settings.

//--Part3. workflow for deploy.
milestone 1
stage('Parameters') {

    echo "============ ${inventory} ============" 
    echo "============ ${tasks} ============" 
    echo "============ ${machines_limit} ============" 
    echo "============ ${customize_playbook} ============" 
    echo "============ ${setupTag} ============"
}


milestone 2
stage('Setup') {
    remote = new Remote(steps, inventory)
    def arr = tasks.split(",")

    echo "============ ${arr.join("#")} ============" 

    for (int i = 0; i < arr.size(); i++) {
        def task = arr[i]
        echo "==== task: ${task} ==="
        def extra_vars = []
        def playbook = ""
        extra_vars << "-l ${machines_limit}"
        if (debug == true) {
            extra_vars << "-v"
        }
        if (setupTag == false) {
            extra_vars << "--tags=setup"
        }
        switch (task) {
            case "installkey":
                playbook = "setups/10-installkey";
                break;
            case "setup":
                playbook = "setups/00-setup";
                break;
            case "jdk7":
                playbook = "setups/01-oracle-jdk7";
                break;
            case "jdk8":
                playbook = "setups/01-oracle-jdk8";
                break;
            case "customize":
                playbook = customize_playbook;
                break;
            default: 
                echo "No defined action for ${task}";
        }
        check_vars = extra_vars + [ "--list-hosts" ]
        remote.deploySetup(playbook, check_vars)

        if (inventory != "test") {
            input id: 'ex', message: "在任务输出中检查确认影响的IP列表，如无问题，点确认执行", ok: '确认执行', submitter: 'sa,scm'
        }

        
        if (task == "installkey") {
            node("ansible-${inventory}") {
                //sh "ip a"
                def inventoryFile = "/tmp/hosts_$BUILD_ID"
                sh "echo '' > ${inventoryFile}"
                sh "echo '[installkeyhost]' >> ${inventoryFile}"
                //add sh -e to avoid the password output in console.log
                sh "#!/bin/sh -e \n echo '${machines_limit} ansible_ssh_pass=${installkey_password} ansible_sudo_pass=${installkey_password}' >> ${inventoryFile}"
                extra_vars << "-i ${inventoryFile}"
            }
            
            //extra_vars << "-e ansible_sudo_pass=${installkey_password}"
            //extra_vars << "-e ansible_ssh_pass=${installkey_password}"
        }
        remote.deploySetup(playbook, extra_vars)
    }
}




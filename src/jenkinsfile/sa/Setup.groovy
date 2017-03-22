#!groovy 
//filename: Deploy.groovy

//--Part1. Include the library and make utils.
@Library('jfpal')

import com.jfpal.lib.Utilities
import com.jfpal.lib.Remote

def utils = new Utilities(steps)


//--Part2. Get the variables from Jenkins job settings.

//--Part3. workflow for deploy.
milestone 1
stage('Parameters') {

    echo "============ ${inventory} ============" 
    echo "============ ${tasks} ============" 
    echo "============ ${machines_limit} ============" 
    echo "==== ${installkey_password} ==="
    //echo "============ ${userInput['tasks']} ============" 
    //echo "============ ${userInput['machines_limit']} ============" 
    //echo "============ ${userInput['inventory']} ============" 


}


milestone 2
stage('Check') {
    remote = new Remote(steps, inventory)
    def arr = tasks.split(",")

    echo "============ ${arr.join("#")} ============" 

    for (int i = 0; i < arr.size(); i++) {
        def task = arr[i]
        echo "==== task: ${task} ==="
        def extra_vars = []
        def playbook = ""
        extra_vars << "-l ${machines_limit}"
        switch (task) {
            case "installkey":
                //ansible-playbook setups/10-installkey.yml -i "${inventory}" -l "${machines_limit}" -kK
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
            default: 
                echo "No defined action for ${it}";
        }
        check_vars = extra_vars + [ "--list-hosts" ]
        remote.deploySetup(playbook, check_vars)

        input id: 'ex', message: "在任务输出中检查确认影响的IP列表，如无问题，点确认执行", ok: '确认执行', submitter: 'sa,scm'

        if (task == "installkey") {
            extra_vars << "ansible_sudo_pass=${installkey_password}"
            extra_vars << "ansible_ssh_pass=${installkey_password}"
        }
        remote.deploySetup(playbook, extra_vars)

    }
//installkey,setup,jdk7,jdk8
//ansible-playbook  -i $inventory -l "openpay || dspay"

// ansible-playbook setups/01-oracle-jdk7.yml -i $inventory -l "openpay || dspay"

// ansible-playbook sos/70-restart-supervisor.yml -i $inventory -l "openpay || dspay” -e “hosts=openpay || dspay"
// ansible-playbook sos/71-remove-remi-repo.yml -i $inventory -l "openpay || dspay" -e "hosts=openpay || dspay"

}


// milestone 3
// stage('Setup') {
//     remote = new Remote(steps, inventory)
//     tasks.split(',').each {
//       //installkey,setup,jdk7,jdk8
//       //ansible-playbook  -i $inventory -l "openpay || dspay"

// // ansible-playbook setups/01-oracle-jdk7.yml -i $inventory -l "openpay || dspay"

// // ansible-playbook sos/70-restart-supervisor.yml -i $inventory -l "openpay || dspay” -e “hosts=openpay || dspay"
// // ansible-playbook sos/71-remove-remi-repo.yml -i $inventory -l "openpay || dspay" -e "hosts=openpay || dspay"

        
        

//       remote.deploySetup(playbook, tags)
//     }
    
    
// }






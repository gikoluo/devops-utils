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
    echo "============ Parameters ============" 
    echo "============ SERVERS=${SERVERS} ============" 
    echo "============ SERVICE_NAME=${SERVICE_NAME} ============"
}


milestone 2
stage('Setup') {
    remote = new Remote(steps, inventory)
    remote.restart(SERVERS, SERVICE_NAME)
}




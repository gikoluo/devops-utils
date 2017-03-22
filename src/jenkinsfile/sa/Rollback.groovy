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

    echo "============ ${PROJECT_NAME} ============" 
    echo "============ ${SERVICE_NAME} ============" 
    echo "============ ${RUN_NAME} ============" 
    echo "============ ${WORKSPACE} ============" 
    echo "============ ${ROLLBACK_TO} ============" 
}


milestone 2
stage('Setup') {
    remote = new Remote(steps, inventory)

    remote.reLink(PROJECT_NAME, SERVICE_NAME, RUN_NAME, WORKSPACE, ROLLBACK_TO)
}




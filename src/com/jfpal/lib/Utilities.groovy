package com.jfpal.lib


class Utilities implements Serializable  {
  def steps

  Utilities(steps) {
    this.steps = steps
  }

  def copyTarget(buildProjectName, targetFile, archive=true) {
    steps.with {
      
        dir(".") {
          step([$class: 'hudson.plugins.copyartifact.CopyArtifact',
             filter: targetFile, 
             fingerprintArtifacts: true, 
             projectName: buildProjectName
          ])
          if ( archive ) {
            archiveArtifacts artifacts:targetFile, fingerprint: true
          }
          stash name:'targetArchive', includes: targetFile
        }
    }
  }

  def qaCheck() {
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
  }

  // This method sets up the Maven and JDK tools, puts them in the environment along
  // with whatever other arbitrary environment variables we passed in, and runs the
  // body we passed in within that environment.
  def mvn(def args) {
    /* Get jdk tool. */

    String jdktool = steps.tool name: "jdk8", type: 'hudson.model.JDK'
    /* Get the maven tool. */
    def mvnHome = steps.tool name: 'maven', type: 'hudson.tasks.Maven$MavenInstallation'

    /* Set JAVA_HOME, and special PATH variables. */
    List javaEnv = [
        "PATH+JDK=${jdktool}/bin", "JAVA_HOME=${jdktool}",
        '_JAVA_OPTIONS=-XX:MaxPermSize=160m -Xmx256m -Djava.awt.headless=true',
        'GIT_COMMITTER_EMAIL=lch@jfpal.com','GIT_COMMITTER_NAME=LCH@Jenkins','GIT_AUTHOR_NAME=LCH','GIT_AUTHOR_EMAIL=lch@jfpal.com', 'LOGNAME=LCH@Jenkins'
    ]

    /* Call maven tool with java envVars. */
    steps.withEnv(javaEnv) {
        if (steps.isUnix()) {
          steps.sh "${mvnHome}/bin/mvn ${args}"
        } else {
          steps.bat "${mvnHome}\\bin\\mvn ${args}"
        }
    }
  }

  // This method sets up the Maven and JDK tools, puts them in the environment along
  // with whatever other arbitrary environment variables we passed in, and runs the
  // body we passed in within that environment.
  def mvnWithJdk7(def args) {
    /* Get jdk tool. */

    String jdktool = steps.tool name: "jdk7", type: 'hudson.model.JDK'
    /* Get the maven tool. */
    def mvnHome = steps.tool name: 'maven', type: 'hudson.tasks.Maven$MavenInstallation'

    /* Set JAVA_HOME, and special PATH variables. */
    List javaEnv = [
        "PATH+JDK=${jdktool}/bin", "JAVA_HOME=${jdktool}",
        '_JAVA_OPTIONS=-XX:MaxPermSize=160m -Xmx256m -Djava.awt.headless=true',
        'GIT_COMMITTER_EMAIL=lch@jfpal.com','GIT_COMMITTER_NAME=LCH@Jenkins','GIT_AUTHOR_NAME=LCH','GIT_AUTHOR_EMAIL=lch@jfpal.com', 'LOGNAME=LCH@Jenkins'
    ]

    /* Call maven tool with java envVars. */
    steps.withEnv(javaEnv) {
        if (steps.isUnix()) {
          steps.sh "${mvnHome}/bin/mvn ${args}"
        } else {
          steps.bat "${mvnHome}\\bin\\mvn ${args}"
        }
    }
  }

  def ant(def args) {
    def antHome = steps.tool name: 'ant', type: 'hudson.tasks.Ant$AntInstallation'
    steps.sh "${antHome}/bin/ant ${args}"
  }
}

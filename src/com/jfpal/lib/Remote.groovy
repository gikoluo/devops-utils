/**
 * file: com.jfpal.lib.Remote.groovy
 * 
 * 使用Ansible节点各环境节点，用于上线。
 *
 */
package com.jfpal.lib

class Remote implements Serializable {
    def script
    def inventory
    def user

    Remote() {}

    Remote(script, inventory, user="ops") {
        this.script = script
        this.inventory = inventory
        this.user = user
    }

    def DEBUG_PRINT(String msg) {
      script.echo "============ ${msg} ============"
    }

    

    /**
     * Deploy Entry
     */
    def deployProcess( String playbook, String file, String BUILD_ID="0", ArrayList tags=['update']  ) {
      script.lock(resource: "${playbook}-prod-server", inversePrecedence: true) {
        try {
          DEBUG_PRINT "DeployProcess ${file} to ${inventory} with playbook ${playbook} tagged by ${tags}, BUILD_ID: ${BUILD_ID}."

          this.unstash (playbook, file, BUILD_ID)

          if(inventory != "test") {
            script.timeout(time:1, unit:'DAYS') {
              script.input message: "可以提交 ${inventory} 了吗?", ok: '准备好了，发布！', submitter: 'qa'
            }
          }
          
          this.deploy (playbook, file, BUILD_ID)

          if(inventory != "test") {
            script.timeout(time:1, unit:'DAYS') {
              script.input message: "${inventory}测试完成了吗? ", ok: '通过！', submitter: 'qa'
            }
          }
        }
        catch (err) {
          DEBUG_PRINT err.toString()
          throw err
        }
        finally {
          this.clean (playbook, file, BUILD_ID)
        }
      }
    }

    /**
     * deploy core
     */
    def deploy( String playbook, String file, String BUILD_ID="0", ArrayList tags=['update'] ) {
      script.node("ansible-${inventory}") {
        DEBUG_PRINT "${inventory} deploy started"

        script.sh 'whoami'
        script.sh '/usr/sbin/ip a'

        //def id = UUID.randomUUID().toString()

        def extraString = " -e TARGET_FILE=/tmp/${playbook}/${BUILD_ID}/${file}"

        if (tags.size() > 0) {
          extraString += " --tags ${tags.join(',')}"
        }
        extraString += " -e BUILD_ID=${BUILD_ID}"

        play(playbook, extraString)

        DEBUG_PRINT "${inventory} deployed end"
      }
    }

    /**
     * Run ansible playbook
     */
    def play(String playbook, String extra) {
      def playCmd = "cd ~/rhasta/ && ansible-playbook ${playbook}.yml -i ${inventory} ${extra}"
      script.sh(playCmd)
    }

    /**
     * unstash will copy the file to jenkins slave node
     */
    def unstash(String playbook, String file, String BUILD_ID="0") {
      script.node("ansible-${inventory}") {
        DEBUG_PRINT("unstash[scp] target to server.")

        script.sh "mkdir -p /tmp/${playbook}/${BUILD_ID}/"

        script.dir("/tmp/${playbook}/${BUILD_ID}/") {
          script.deleteDir()
          script.unstash 'targetArchive'
        }
        DEBUG_PRINT("unstash[scp] finished.")
      }
    }


    /**
     * clean files created in deploy process
     */
    def clean(String playbook, String file, String BUILD_ID="0") {
      script.node("ansible-${inventory}") {
        DEBUG_PRINT "${inventory} clean"
        script.sh  "rm -r /tmp/${playbook}/${BUILD_ID}/"
      }
    }

    def deployAnsible( String file ) {
      DEBUG_PRINT "deploy ${file} to ${inventory} without playbook."
      def playbook = "ansible"

      def filename = file.substring(file.lastIndexOf("/") + 1, file.length());
      script.sh ('whoami')
      script.sh ('/usr/sbin/ip a')
      def id = UUID.randomUUID().toString()

      script.sh  "mkdir -p /tmp/${playbook}/${id}/"

      script.dir("/tmp/${playbook}/${id}/") {
        script.deleteDir()
        script.unstash 'targetArchive'
      }
      
      script.sh "mkdir -p ~/rhasta/; cd ~/rhasta/ && tar zxf /tmp/${playbook}/${id}/${file}"

      script.sh  "rm -r /tmp/${playbook}/${id}/"
    }
}

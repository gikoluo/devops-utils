package com.jfpal


class Remote implements Serializable {
    def script
    def inventory
    def user
    Remote() {}

    Remote(script, inventory, user) {

        this.script = script
        this.inventory = inventory
        this.user = user
    }

    def cmd (String cmd) {
      script.sh "ssh -o StrictHostKeyChecking=no -l ${user} ansible-${inventory} '${cmd}' "
    }

    def scp(String src, String dest) {
      script.sh "scp '${src}' '${user}'@'ansible-${inventory}':${dest}"
    }

    def play(String playbook, String extra) {
      def playCmd = "cd ~/rhasta/ && ansible-playbook ${playbook}.yml -i ${inventory} ${extra}"
      cmd(playCmd)
    }

    // def deploy( Map conf ) {
    //   deploy( conf.playbook, conf.file, [] )
    // }

    def deploy( String playbook, String file, String tag) {
      script.echo "deploy ${file} to ${inventory} with playbook ${playbook} tagged by ${tag} ."

      def filename = file.substring(file.lastIndexOf("/") + 1, file.length());

      script.echo "filename is ${filename}."

      script.sshagent (credentials: ["ansible-${inventory}"]) {
        //remote = new Remote(script, "ansible-${inventory}", remoteUser)
        cmd('whoami')
        cmd('/usr/sbin/ip a')
        def id = UUID.randomUUID().toString()

        cmd "mkdir -p /tmp/${playbook}/"

        script.dir("/tmp/${playbook}/") {
          script.deleteDir()
          script.unstash 'targetArchive'

          scp(file, "/tmp/${playbook}/${id}.${filename}")
        }
        def extraString = " -e local_file=/tmp/${playbook}/${id}.${filename}"
        if (tag.size() > 0) {
          extraString += " --tags ${tag}"
        }
        def BUILD_ID = "0"
        // if (script.params.get("BUILD_ID", "").size() > 0) {
        //   extraString += " -e BUILD_ID=${script.BUILD_ID}"
        // }
        play(playbook, extraString)
      }
    }
}

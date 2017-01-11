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

    def cmd (String cmd) {
      script.sh "ssh -o StrictHostKeyChecking=no -l ${user} ansible-${inventory} '${cmd}' "
    }

    def scp(String src, String dest) {
      script.sh "scp '${src}' '${user}'@'ansible-${inventory}':${dest}"
    }

    def play(String playbook, String extra) {
      def playCmd = "cd ~/rhasta/ && ansible-playbook ${playbook}.yml -i ${inventory} ${extra}"
      script.sh(playCmd)
    }

    // def deploy( Map conf ) {
    //   deploy( conf.playbook, conf.file, [] )
    // }

    def deploy( String playbook, String file, String BUILD_ID="0", ArrayList tags=['update'] ) {
      script.echo "deploy ${file} to ${inventory} with playbook ${playbook} tagged by ${tags} ."

      def filename = file.substring(file.lastIndexOf("/") + 1, file.length());

      script.echo "filename is ${filename}."

      //script.node("ansible-${inventory}") {
        script.sh 'whoami'
        script.sh '/usr/sbin/ip a'

        def id = UUID.randomUUID().toString()

        script.sh "mkdir -p /tmp/${playbook}/${id}/"

        script.dir("/tmp/${playbook}/${id}/") {
          script.deleteDir()
          script.unstash 'targetArchive'
        }

        def extraString = " -e TARGET_FILE=/tmp/${playbook}/${id}/${file}"

        script.echo "BUILD_ID: ${BUILD_ID}"

        play(playbook, extraString)

      //}

      // script.sshagent (credentials: ["ansible-${inventory}"]) {
      //   //remote = new Remote(script, "ansible-${inventory}", remoteUser)
      //   //cmd('whoami')
      //   //cmd('/usr/sbin/ip a')
      //   def id = UUID.randomUUID().toString()

      //   cmd "mkdir -p /tmp/${playbook}./"

      //   script.dir("/tmp/${playbook}/") {
      //     script.deleteDir()
      //     script.unstash 'targetArchive'

      //     scp(file, "/tmp/${playbook}/${id}.${filename}")
      //   }
      //   def extraString = " -e TARGET_FILE=/tmp/${playbook}/${id}.${filename}"
      //   if (tags.size() > 0) {
      //     extraString += " --tags ${tags.join(',')}"
      //   }
      //   extraString += " -e BUILD_ID=${BUILD_ID}"
      //   script.echo "BUILD_ID: ${BUILD_ID}"

      //   play(playbook, extraString)
      // }
    }

    def deployAnsible( String file ) {
      script.echo "deploy ${file} to ${inventory} without playbook."
      def playbook = "ansible"

      //script.sshagent (credentials: ["ansible-${inventory}"]) {

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
      //}
    }
}

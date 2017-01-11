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

    def play(String playbook, String extra) {
      def playCmd = "cd ~/rhasta/ && ansible-playbook ${playbook}.yml -i ${inventory} ${extra}"
      script.sh(playCmd)
    }

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

      if (tags.size() > 0) {
        extraString += " --tags ${tags.join(',')}"
      }
      extraString += " -e BUILD_ID=${BUILD_ID}"


      script.echo "BUILD_ID: ${BUILD_ID}"

      play(playbook, extraString)

      script.sh  "rm -r /tmp/${playbook}/${id}/"
    }

    def deployAnsible( String file ) {
      script.echo "deploy ${file} to ${inventory} without playbook."
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

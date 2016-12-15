package com.jfpal


class Remote implements Serializable {
    def script
    def host
    def user
    Remote() {}

    Remote(script, host, user) {

        this.script = script
        this.host = host
        this.user = user
    }

    public String cmd(String cmd) {
      script.sh "ssh -o StrictHostKeyChecking=no -l ${user} ${host} '${cmd}' "
    }

    public scp(String src, String dest) {
      script.sh "scp '${src}' '${user}'@'${host}':${dest}"
    }

    public play(def playbook, def inventory, def extra) {
      def playCmd = "cd ~/rhasta/ && ansible-playbook ${playbook}.yml -i ${inventory} ${extra}"
      cmd(playCmd)
    }

    def deploy( Map conf ) {
      deploy( conf.playbook, conf.inventory, conf.file, conf.get("tags", []) )
    }

    def deploy( String playbook, String inventory, String file, String[] tags=[]) {
      script.echo "deploy ${f} to ${to} with playbook ${playbook} tagged by ${tag} ."

      def filename = f.substring(f.lastIndexOf("/") + 1, f.length());

      script.echo "filename is ${filename}."

      tag = tags.join()

      script.sshagent (credentials: ["ansible-${inventory}"]) {
        //remote = new Remote(script, "ansible-${inventory}", remoteUser)
        cmd('whoami')
        cmd('/usr/sbin/ip a')
        def id = UUID.randomUUID().toString()

        cmd "mkdir -p /tmp/${playbook}/"

        dir("/tmp/${playbook}/") {
          deleteDir()
          unstash 'targetArchive'

          scp(f, "/tmp/${playbook}/${id}.${filename}")
        }
        play(playbook, to, "--tags ${tag} -e BUILD_ID=${BUILD_ID} -e local_file=/tmp/${playbook}/${id}.${filename}")
      }
    }
}

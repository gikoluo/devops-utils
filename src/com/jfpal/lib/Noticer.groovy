/**
 * file: com.jfpal.lib.Remote.groovy
 * 
 * 使用Ansible节点各环境节点，用于上线。
 *
 */
package com.jfpal.lib


class Noticer implements Serializable {
    def steps

    Noticer(steps) {
      this.steps = steps
    }

    def send(String event, String level="INFO", String inventory="test", String playbook="DevOps/test", String msg="", String actor="SYSTEM") {
      steps.echo "=======${level}== ${event} = ${ playbook } == ${msg} ============"

      def project = playbook.split("/")[0]

      steps.node("master") {
        def fullMsg = "== ${project} / ${inventory} ==\n [${level}]: ${msg}. \n 执行人: ${actor} \n#DevOps #${project} #${inventory}".toString()
        //steps.echo "=======${fullMsg}===${steps.username}==========="
        steps.sh "/usr/bin/bearychat -t '${fullMsg}' -c 'DevOps,${project}' -m"
        if (inventory == "prod") {
          steps.sh "/usr/bin/bearychat -t '${fullMsg}' -c '管理组' -m"
        }
      }
    }
}

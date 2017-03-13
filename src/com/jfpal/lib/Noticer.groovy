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

    def send(String event, String level="INFO", String project="DevOps", String msg="") {
      def fullMsg = "==${project}==\n[${level}]: ${msg}"
      steps.node("master") {
        steps.sh '/usr/bin/bearychat -t "${fullMsg}" -c "DevOps,${project}" -m'
        if (steps.inventory == "prod") {
          steps.sh '/usr/bin/bearychat -t "${fullMsg}" -c "管理组" -m'
        }
      }
    }
}

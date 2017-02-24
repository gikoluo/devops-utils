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

    def send(String msg, String type="INFO") {
      steps.sh 'bearychat -t "' + type + ': ' + msg + '" -c "DevOps"'
    }
}

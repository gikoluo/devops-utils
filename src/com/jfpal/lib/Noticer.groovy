/**
 * file: com.jfpal.lib.Remote.groovy
 * 
 * 使用Ansible节点各环境节点，用于上线。
 *
 */
package com.jfpal.lib

class Noticer implements Serializable {
    def script
    def inventory
    def user

    Noticer() {}

    def send(String type, String msg) {
      script.sh 'bearychat -t "' + type + ': ' + msg + '" -c "DevOps"'
    }
}

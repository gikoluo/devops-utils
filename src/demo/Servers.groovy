package demo

class Servers implements Serializable {

    private Script script

    public Servers(Script script) {
        this.script = script
    }

    public void deploy(String id) {
        script.unstash 'jar'
        script.sh "cp jifu-platform-server-1.0.1-SNAPSHOT.jar /tmp/webapps/${id}.war"
    }

    public void undeploy(String id) {
        script.sh "rm /tmp/webapps/${id}.war"
    }

    public <T> T withDeployment(Closure<T> body) {
        String id = UUID.randomUUID().toString()
        deploy id
        try {
            body.call id
        } finally {
            undeploy id
        }
    }

}

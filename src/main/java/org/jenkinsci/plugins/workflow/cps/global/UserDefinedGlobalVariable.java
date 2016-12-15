package org.jenkinsci.plugins.workflow.cps.global;

import groovy.lang.Binding;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import jenkins.model.Jenkins;

/**
 * Global variable backed by user-supplied script.
 *
 * @author Kohsuke Kawaguchi
 * @see UserDefinedGlobalVariableList
 */
// not @Extension because these are instantiated programmatically
public class UserDefinedGlobalVariable extends GlobalVariable {
    private final File help;
    private final String name;

    /*package*/ UserDefinedGlobalVariable(WorkflowLibRepository repo, String name) {
        this(name, new File(repo.workspace, PREFIX + "/" + name + ".txt"));
    }

    public UserDefinedGlobalVariable(String name, File help) {
        this.name = name;
        this.help = help;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript script) throws Exception {
        Binding binding = script.getBinding();
        Object instance;
        if (binding.hasVariable(getName())) {
            instance = binding.getVariable(getName());
        } else {
            CpsThread c = CpsThread.current();
            if (c==null)
                throw new IllegalStateException("Expected to be called from CpsThread");

            instance = c.getExecution().getShell().getClassLoader().loadClass(getName()).newInstance();
            /* We could also skip registration of vars in GroovyShellDecoratorImpl and use:
                 instance = c.getExecution().getShell().parse(source(".groovy"));
               But then the source will appear in CpsFlowExecution.loadedScripts and be offered up for ReplayAction.
               We might *want* to support replay of global vars & classes at some point, but to make it actually work
               we would also need to start calling LoadStepExecution.Replacer.
            */
            binding.setVariable(getName(), instance);
        }
        return instance;
    }

    /**
     * Loads help from user-defined file, if available.
     */
    public @CheckForNull String getHelpHtml() throws IOException {
        if (!help.exists())     return null;

        return Jenkins.getActiveInstance().getMarkupFormatter().translate(FileUtils.readFileToString(help, Charsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserDefinedGlobalVariable that = (UserDefinedGlobalVariable) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /*package*/ static final String PREFIX = "vars";
}

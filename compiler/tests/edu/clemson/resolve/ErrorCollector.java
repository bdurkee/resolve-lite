package edu.clemson.resolve;

import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.compiler.RESOLVECompilerListener;
import edu.clemson.resolve.compiler.RESOLVEMessage;
import edu.clemson.resolve.misc.Utils;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector implements RESOLVECompilerListener {

    public final RESOLVECompiler compiler;
    public final List<String> infos = new ArrayList<>();
    public final List<RESOLVEMessage> errors = new ArrayList<>();
    public final List<RESOLVEMessage> warnings = new ArrayList<>();
    public final List<RESOLVEMessage> all = new ArrayList<>();

    public ErrorCollector() {
        this(null);
    }

    public ErrorCollector(RESOLVECompiler rc) {
        this.compiler = rc;
    }

    @Override public void info(String msg) {
        infos.add(msg);
    }

    @Override public void error(RESOLVEMessage msg) {
        errors.add(msg);
        all.add(msg);
    }

    @Override public void warning(RESOLVEMessage msg) {
        warnings.add(msg);
        all.add(msg);
    }

    @Override public String toString() {
        return toString(false);
    }

    public String toInfoString() {
        return Utils.join(infos, "\n");
    }

    public String toString(boolean rendered) {
        if (!rendered) {
            return Utils.join(all, "\n");
        }

        if (compiler == null) {
            throw new IllegalStateException(String.format("no %s instance is available.",
                    RESOLVECompiler.class.getName()));
        }

        StringBuilder buf = new StringBuilder();
        List<String> result = new ArrayList<>();
        for (RESOLVEMessage m : all) {
            ST st = compiler.errMgr.getMessageTemplate(m);
            result.add(st.render());
        }
        return Utils.join(result, "\n");
    }
}

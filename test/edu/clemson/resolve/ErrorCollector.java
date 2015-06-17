package edu.clemson.resolve;

import edu.clemson.resolve.compiler.ResolveCompiler;
import edu.clemson.resolve.compiler.ResolveCompilerListener;
import edu.clemson.resolve.compiler.ResolveMessage;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector implements ResolveCompilerListener {

    public final ResolveCompiler compiler;
    public final List<String> infos = new ArrayList<>();
    public final List<ResolveMessage> errors = new ArrayList<>();
    public final List<ResolveMessage> warnings = new ArrayList<>();
    public final List<ResolveMessage> all = new ArrayList<>();

    public ErrorCollector() {
        this(null);
    }

    public ErrorCollector(ResolveCompiler rc) {
        this.compiler = rc;
    }

    @Override public void info(String msg) {
        infos.add(msg);
    }

    @Override public void error(ResolveMessage msg) {
        errors.add(msg);
        all.add(msg);
    }

    @Override public void warning(ResolveMessage msg) {
        warnings.add(msg);
        all.add(msg);
    }
}

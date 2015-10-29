package edu.clemson.resolve.proving.absyn;

import java.io.IOException;

public class PExpTextRenderingListener extends PExpListener {

    private final Appendable output;

    private PAlternatives encounteredAlternative;
    private PExp encounteredResult;

    public PExpTextRenderingListener(Appendable w) {
        output = w;
    }

    @Override public void beginPExp(PExp p) {
        if (encounteredAlternative != null) {
            if (encounteredResult == null) {
                encounteredResult = p;
            }
            else {
                try {
                    encounteredResult = null;
                    output.append(", if ");
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override public void beginPSymbol(PSymbol p) {
        try {
            if (p.isIncoming()) {
                output.append("@");
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

   /* @Override public void beginPrefixPSymbol(PSymbol p) {
        try {
            output.append(p.getName());

            if (p.getArguments().size() > 0) {
                output.append("(");
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    @Override public void beginInfixPSymbol(PSymbol p) {
        try {
            output.append("(");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void beginOutfixPSymbol(PSymbol p) {
        try {
            output.append(p.getLeftPrint());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

  /*  @Override public void beginPostfixPSymbol(PSymbol p) {
        try {
            if (p.getArguments().size() > 0) {
                output.append("(");
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    @Override public void beginPAlternatives(PAlternatives p) {
        try {
            output.append("{{");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void beginPLambda(PLambda l) {
        try {
            output.append("lambda (");
            boolean first = true;
            for (PLambda.Parameter p : l.getParameters()) {
                if (first) {
                    first = false;
                }
                else {
                    output.append(", ");
                }
                output.append("" + p);
            }
            output.append(").");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void fencepostPrefixPSymbol(PSymbol p) {
        try {
            output.append(", ");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void fencepostInfixPSymbol(PSymbol p) {
        try {
            output.append(" " + p.getName() + " ");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void fencepostOutfixPSymbol(PSymbol p) {
        try {
            output.append(", ");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void fencepostPostfixPSymbol(PSymbol p) {
        try {
            output.append(", ");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void fencepostPAlternatives(PAlternatives p) {
        try {
            output.append("; ");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

   /* @Override public void endPrefixPSymbol(PSymbol p) {
        try {
            if (p.getArguments().size() > 0) {
                output.append(")");
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    @Override public void endInfixPSymbol(PSymbol p) {
        try {
            output.append(")");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void endOutfixPSymbol(PSymbol p) {
        try {
            output.append(p.getRightPrint());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*@Override public void endPostfixPSymbol(PSymbol p) {
        try {
            if (p.getArguments().size() > 0) {
                output.append(")");
            }
            output.append(p.getName());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    @Override public void endPAlternatives(PAlternatives p) {
        try {
            output.append(", otherwise}}");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

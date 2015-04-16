package org.resolvelite.semantics.absyn;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.resolvelite.compiler.tree.ResolveToken;
import org.resolvelite.misc.Utils.Builder;
import org.resolvelite.parsing.ResolveLexer;
import org.resolvelite.semantics.MathType;
import org.resolvelite.semantics.symbol.BaseSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * An {@code MSymbolExp} represents a reference to a named element such as a
 * variable, constant, or function. More specifically, all three are represented
 * as function calls, with the former two represented as functions with no
 * arguments.
 */
public class MSymbolExp extends MExp {

    public static enum DisplayStyle {
        INFIX, OUTFIX, PREFIX
    }

    private final Token leftPrint, rightPrint, name;

    private final List<MExp> arguments = new ArrayList<>();
    private final boolean literalFlag, incomingFlag;
    private BaseSymbol.Quantification quantification;
    private final DisplayStyle dispStyle;

    private MSymbolExp(MSymbolExpBuilder builder) {
        super(builder.mathType, builder.mathTypeValue);
        this.name = builder.name;
        this.leftPrint = builder.lprint;
        this.rightPrint = builder.rprint;

        this.arguments.addAll(builder.arguments);
        this.literalFlag = builder.literal;
        this.incomingFlag = builder.incoming;
        this.dispStyle = builder.style;
        this.quantification = builder.quantification;
    }

    /**
     * This class represents function <em>applications</em>. The type of a
     * function application is the type of the range of the function. Often we'd
     * like to think about the type of the <em>function itself</em>, not the
     * type of the result of its application. Unfortunately our AST does not
     * consider that the 'function' part of a FunctionExp (as distinct from its
     * parameters) might be a first-class citizen with a type of its own. This
     * method emulates retrieving the (not actually extant) first-class function
     * part and guessing its type. In this case, the guess is "conservative", in
     * that we guess the smallest set that can't be contradicted by the
     * available information. For nodes without a true, first-class function to
     * consult (which, at the moment, is all of them), this means that for the
     * formal parameter types, we'll guess the types of the actual parameters,
     * and for the return type we'll guess <strong>Empty_Set</strong> (since we
     * have no information about how the return value is used.) This guarantees
     * that the type we return will be a subset of the actual type of the
     * function the RESOLVE programmer intends (assuming she has called it
     * correctly.)
     */
    /*public MTFunction getConservativePreApplicationType(TypeGraph g) {
        List<MTType> subTypes = new LinkedList<MTType>();
        for (ExprAST arg : arguments) {
            subTypes.add(arg.getMathType());
        }
        return new MTFunction(g, g.EMPTY_SET, subTypes);
    }*/

    public Token getName() {
        return name;
    }

    public List<MExp> getArguments() {
        return arguments;
    }

    public boolean isIncoming() {
        return incomingFlag;
    }

    public boolean isFunction() {
        return arguments.size() > 0;
    }

    public DisplayStyle getDispStyle() {
        return dispStyle;
    }

    public void setQuantification(BaseSymbol.Quantification q) {
        quantification = q;
    }

    //Todo: Figure out what qualifiers are going to look like in a
    //mathematical setting.
    public Token getQualifier() {
        return null;
    }

    public BaseSymbol.Quantification getQuantification() {
        return quantification;
    }

    @Override public List<MExp> getSubExpressions() {
        return arguments;
    }

    @Override public void setSubExpression(int index, MExp e) {
        arguments.set(index, e);
    }

    @Override public boolean isLiteral() {
        return literalFlag;
    }

    /* @Override
     protected ExprAST substituteChildren(Map<ExprAST, ExprAST> substitutions) {
         List<ExprAST> newArguments = new ArrayList<ExprAST>();
         for (ExprAST e : arguments) {
             newArguments.add(substitute(e, substitutions));
         }
         MathSymbolAST newName =
                 new MathSymbolExprBuilder(getStart(), getStop(), name, null)
                         .arguments(arguments).literal(literalFlag)
                         .quantification(quantification).incoming(
                         incomingFlag).build();
         if (substitutions.containsKey(newName)) {
             //Note that there's no particular mathematical justification why
             //we can only replace a function with a different function NAME (as
             //opposed to a function-valued expression), but we have no way of
             //representing such a thing.  It doesn't tend to come up, but if it
             //ever did, this would throw a ClassCastException.
             newName =
                     new MathSymbolExprBuilder(getStart(), getStop(),
                             ((MathSymbolAST) substitutions.get(newName))
                                     .getName(), null).arguments(arguments)
                             .literal(literalFlag).quantification(
                             quantification).incoming(incomingFlag)
                             .build();
         }
         MathSymbolAST result =
                 new MathSymbolExprBuilder(getStart(), getStop(), newName
                         .getName(), null).arguments(arguments).literal(
                         literalFlag).quantification(quantification)
                         .incoming(incomingFlag).build();
         result.setMathType(myMathType);
         result.setMathTypeValue(myMathTypeValue);
         return result;
     }
     @Override
     public boolean equivalent(ExprAST e) {
         boolean result = (e instanceof MathSymbolAST);
         if (result) {
             MathSymbolAST eAsSymbol = (MathSymbolAST) e;
             result =
                     name.equals(((MathSymbolAST) e).name)
                             && argumentsEquivalent(arguments,
                             eAsSymbol.arguments)
                             && quantification == eAsSymbol.quantification;
         }
         return result;
     }
     private boolean argumentsEquivalent(List<ExprAST> original,
                                         List<ExprAST> compare) {
         boolean result = true;
         Iterator<ExprAST> args1 = original.iterator();
         Iterator<ExprAST> args2 = compare.iterator();
         while (result && args1.hasNext() && args2.hasNext()) {
             result = args1.next().equivalent(args2.next());
         }
         return result;
     }
     @Override
     public ExprAST copy() {
         Token newName = new ResolveToken(name.getText());
         List<ExprAST> newArgs = new ArrayList<ExprAST>(arguments);
         MathSymbolAST result =
                 new MathSymbolExprBuilder(getStart(), getStop(), newName, null)
                         .arguments(newArgs).quantification(quantification)
                         .dispStyle(dispStyle).literal(literalFlag).incoming(
                         incomingFlag).build();
         result.setMathType(myMathType);
         result.setMathTypeValue(myMathTypeValue);
         return result;
     }
     @Override
     public String toString() {
         StringBuilder result = new StringBuilder();
         boolean first = true;
         if (isFunction()) {
             if (dispStyle == DisplayStyle.INFIX) {
                 result.append(arguments.get(0)).append(" " + name + " ")
                         .append(arguments.get(1));
             }
             else if (dispStyle == DisplayStyle.OUTFIX) {
                 result.append(leftPrint).append(arguments.get(0)).append(
                         rightPrint);
             }
             else {
                 result.append(name.getText()).append("(");
                 result.append(TreeUtil.join(arguments, ", ")).append(")");
             }
         }
         else {
             result.append(name.getText());
         }
         return result.toString();
     }*/

    public static class MSymbolExpBuilder implements Builder<MSymbolExp> {
        protected final Token name, lprint, rprint;
        protected boolean incoming = false;
        protected boolean literal = false;

        protected DisplayStyle style = DisplayStyle.PREFIX;
        protected BaseSymbol.Quantification quantification =
                BaseSymbol.Quantification.NONE;

        protected MathType mathType, mathTypeValue;
        protected final List<MExp> arguments = new ArrayList<MExp>();

        public MSymbolExpBuilder(String name) {
            this(new ResolveToken(ResolveLexer.Identifier, name), null);
        }

        public MSymbolExpBuilder(Token lprint, Token rprint) {
            if ( rprint == null ) {
                if ( lprint == null ) {
                    throw new IllegalStateException("null name; all math "
                            + "symbols must be named.");
                }
                rprint = lprint;
                this.name = lprint;
            }
            else {
                this.name =
                        new ResolveToken(ResolveLexer.Identifier,
                                lprint.getText() + "..." + rprint.getText());
            }
            this.lprint = lprint;
            this.rprint = rprint;
        }

        public MSymbolExpBuilder(ParserRuleContext ctx, Token lprint,
                                 Token rprint) {
            this(lprint, rprint);
        }

        public MSymbolExpBuilder literal(boolean e) {
            literal = e;
            return this;
        }

        public MSymbolExpBuilder mathType(MathType e) {
            this.mathType = e;
            return this;
        }

        public MSymbolExpBuilder mathTypeValue(MathType e) {
            this.mathTypeValue = e;
            return this;
        }

        public MSymbolExpBuilder quantification(BaseSymbol.Quantification q) {
            quantification = q;
            return this;
        }

        public MSymbolExpBuilder style(DisplayStyle e) {
            style = e;
            return this;
        }

        public MSymbolExpBuilder incoming(boolean e) {
            incoming = e;
            return this;
        }

        public MSymbolExpBuilder arguments(MExp... e) {
            arguments(Arrays.asList(e));
            return this;
        }

        public MSymbolExpBuilder arguments(Collection<MExp> args) {
            arguments.addAll(args);
            return this;
        }

        @Override public MSymbolExp build() {
            if ( this.mathType == null ) {
                throw new IllegalStateException("mathtype == null; cannot "
                        + "build mexp with null mathtype");
            }
            return new MSymbolExp(this);
        }
    }
}
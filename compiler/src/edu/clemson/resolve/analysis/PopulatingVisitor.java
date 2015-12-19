/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.HardCoded;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.TypeGraph;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.*;
import org.rsrg.semantics.query.*;
import org.rsrg.semantics.symbol.*;

import java.util.*;
import java.util.stream.Collectors;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private static final boolean EMIT_DEBUG = true;

    //TODO: Many of these flags can probably be eliminate via a antlr4 helper method Trees.*
    private boolean walkingDefParams = false;

    /** Keeps track of the current operationProcedure (and procedure) we're
     *  visiting; {@code null} otherwise. We use this to check whether a
     *  recursive call is being made to an operation procedure decl that hasn't
     *  been marked 'Recursive'.
     */
    //private ResolveParser.OperationProcedureDeclContext currentOpProcedureDecl = null;
    //private ResolveParser.ProcedureDeclContext currentProcedureDecl = null;

    /** Set to {@code true} when we're walking the arguments to a module
     *  (i.e. walking some set of args to a facility decl); or when we're walking
     *  module formal parameters. Should be {@code false} otherwise;
     */
    private boolean walkingModuleArgOrParamList = false;
    private boolean walkingFunctionAppArgs = false;
    private ResolveParser.MathExpContext prevMathSelectorAccess = null;

    private Map<String, MTType> definitionSchematicTypes = new HashMap<>();

    private final ParseTreeProperty<MTType> anonymousFunctionExpectedRangeTypes =
            new ParseTreeProperty<>();

    /**
     * Keeps track of a global type model symbol.  the type representation for some type family
     */
    private TypeModelSymbol curTypeReprModelSymbol = null;

    private RESOLVECompiler compiler;
    private MathSymbolTable symtab;
    private AnnotatedModule tr;
    private TypeGraph g;

    private int typeValueDepth = 0;
    private int globalSpecCount = 0;
    private int anonymousApplicationDepth = 0;

    /** Any quantification-introducing syntactic context (e.g., an
     *  {@link ResolveParser.MathQuantifiedExpContext}),
     *  introduces a level to this stack to reflect the quantification that
     *  should be applied to named variables as they are encountered.
     *  <p>
     *  Note that this may change as the children of the node are processed;
     *  for example, {@link ResolveParser.MathVariableDeclContext}s found in the
     *  declaration portion of a quantified ctx should have quantification
     *  (universal or existential) applied, while those found in the body of
     *  the quantified ctx should have
     *  no quantification (unless there is an embedded quantified ctx). In this
     *  case, ctx should not remove its layer, but rather change it to
     *  {@link Quantification#NONE}.</p>
     *  <p>
     *  This stack is never empty, but rather the bottom layer is always
     *  {@link Quantification#NONE}.</p>
     */
    private Deque<Quantification> activeQuantifications = new LinkedList<>();

    private ModuleScopeBuilder moduleScope = null;

    public PopulatingVisitor(@NotNull RESOLVECompiler rc,
                             @NotNull MathSymbolTable symtab,
                             @NotNull AnnotatedModule annotatedTree) {
        this.activeQuantifications.push(Quantification.NONE);

        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @NotNull public TypeGraph getTypeGraph() {
        return g;
    }

    @Override public Void visitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        moduleScope = symtab.startModuleScope(tr)
                .addImports(tr.semanticallyRelevantUses);
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires a return, even if its 'Void'
    }

    @Override public Void visitConceptImplModuleDecl(
            ResolveParser.ConceptImplModuleDeclContext ctx) {
        try {
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(
                    new ModuleIdentifier(ctx.concept));
            moduleScope.addImports(conceptScope.getImports());
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.concept, ctx.concept.getText());
        }
        super.visitChildren(ctx);
        return null;
    }

    /*@Override public Void visitConceptExtensionModuleDecl(
            ResolveParser.ConceptExtensionModuleDeclContext ctx) {
        try {
            List<String> implicitImports =
                    symtab.getModuleScope(ctx.concept).getImports();
            moduleScope.addImports(implicitImports);
        }
        catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.concept);
        }
        this.visitChildren(ctx);
        return null;
    }

    @Override public Void visitExtensionImplModule(
            ResolveParser.ExtensionImplModuleContext ctx) {
        try {
            moduleScope.addImports(symtab.getModuleScope(ctx.enhancement)
                    .getImports());
        }
        catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.enhancement);
        }
        this.visitChildren(ctx);
        return null;
    }

    @Override public Void visitImplModuleParameterList(
             ResolveParser.ImplModuleParameterListContext ctx) {
         walkingModuleArgOrParamList = true;
         this.visitChildren(ctx);
         walkingModuleArgOrParamList = false;
         return null;
     }*/

    @Override public Void visitSpecModuleParameterList(
            ResolveParser.SpecModuleParameterListContext ctx) {
        walkingModuleArgOrParamList = true;
        this.visitChildren(ctx);
        walkingModuleArgOrParamList = false;
        return null;
    }

    @Override public Void visitGenericTypeParameterDecl(
            ResolveParser.GenericTypeParameterDeclContext ctx) {
        try {
            //all generic params are module params (grammar says so)
            ModuleParameterSymbol moduleParam =
                    new ModuleParameterSymbol(new ProgParameterSymbol(g,
                            ctx.name.getText(),
                            ProgParameterSymbol.ParameterMode.TYPE,
                            new PTElement(g), ctx, getRootModuleIdentifier()));
            symtab.getInnermostActiveScope().define(moduleParam);
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.ID().getText());
        }
        return null;
    }

    @Override public Void visitTypeModelDecl(
            ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathTypeExp());
        MathSymbol exemplarSymbol = null;
        try {
            exemplarSymbol =
                    symtab.getInnermostActiveScope().addBinding(
                            ctx.exemplar.getText(), ctx,
                            tr.mathTypeValues.get(ctx.mathTypeExp()));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
        if (ctx.constraintClause() != null) this.visit(ctx.constraintClause());
        if (ctx.typeModelInit() != null) this.visit(ctx.typeModelInit());
        symtab.endScope();
        try {
            PExp constraint = getPExpFor(ctx.constraintClause());
            PExp initEnsures =
                    getPExpFor(ctx.typeModelInit() != null ? ctx
                            .typeModelInit().ensuresClause() : null);
            MTType modelType = tr.mathTypeValues.get(ctx.mathTypeExp());

            ProgTypeSymbol progType =
                    new TypeModelSymbol(symtab.getTypeGraph(),
                            ctx.name.getText(), modelType,
                            new PTFamily(modelType, ctx.name.getText(),
                                    ctx.exemplar.getText(), constraint,
                                    initEnsures, getRootModuleIdentifier()),
                            exemplarSymbol, ctx, getRootModuleIdentifier());
            symtab.getInnermostActiveScope().define(progType);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        return null;
    }

    /* @Override public Void visitProcedureDecl(
             ResolveParser.ProcedureDeclContext ctx) {
         OperationSymbol correspondingOp = null;
         currentProcedureDecl = ctx;
         try {
             correspondingOp =
                     symtab.getInnermostActiveScope()
                             .queryForOne(new NameQuery(null, ctx.name, false))
                             .toOperationSymbol();
         } catch (NoSuchSymbolException nse) {
             compiler.errMgr.semanticError(ErrorKind.DANGLING_PROCEDURE,
                     ctx.getStart(), ctx.name.getText());
         } catch (DuplicateSymbolException dse) {
             compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                     ctx.getStart(), ctx.getText());
         }
         symtab.startScope(ctx);
         this.visit(ctx.operationParameterList());
         PTType returnType = null;
         if (ctx.type() != null) {
             this.visit(ctx.type());
             returnType = tr.progTypeValues.get(ctx.type());
             try {
                 symtab.getInnermostActiveScope().define(
                         new ProgVariableSymbol(ctx.name.getText(), ctx,
                                 returnType, getRootModuleIdentifier()));
             }
             catch (DuplicateSymbolException dse) {
                 compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                         ctx.name.getText());
             }
         }
         else {
             returnType = PTVoid.getInstance(g);
         }
         ctx.variableDeclGroup().forEach(this::visit);
         ctx.stmt().forEach(this::visit);
         symtab.endScope();
         try {
             symtab.getInnermostActiveScope().define(
                     new ProcedureSymbol(ctx.name.getText(), ctx,
                             getRootModuleIdentifier(), correspondingOp));
         } catch (DuplicateSymbolException dse) {
             compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                     ctx.getStart(), ctx.name.getText());
         }
         currentProcedureDecl = null;
         return null;
     }*/

    @Override public Void visitOperationDecl(
            ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
        if (ctx.type() != null) {
            this.visit(ctx.type());
            try {
                symtab.getInnermostActiveScope().addBinding(ctx.name.getText(),
                        ctx.getParent(), tr.mathTypeValues.get(ctx.type()));
            } catch (DuplicateSymbolException e) {
                //This shouldn't be possible--the operation declaration has a
                //scope all its own and we're the first ones to get to
                //introduce anything
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), ctx.getText());
            }
        }
        if (ctx.requiresClause() != null) this.visit(ctx.requiresClause());
        if (ctx.ensuresClause() != null) this.visit(ctx.ensuresClause());
        symtab.endScope();

        insertFunction(ctx.name, ctx.type(),
                ctx.requiresClause(), ctx.ensuresClause(), ctx);
        return null;
    }

     /*@Override public Void visitOperationProcedureDecl(
             ResolveParser.OperationProcedureDeclContext ctx) {
         symtab.startScope(ctx);
         currentOpProcedureDecl = ctx;
         ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
         if (ctx.type() != null) {
             this.visit(ctx.type());
             try {
                 symtab.getInnermostActiveScope().define(
                         new ProgVariableSymbol(ctx.name.getText(), ctx,
                                 tr.progTypeValues.get(ctx.type()),
                                 getRootModuleIdentifier()));
             } catch (DuplicateSymbolException e) {
                 //This shouldn't be possible--the operation declaration has a
                 //scope all its own and we're the first ones to get to
                 //introduce anything
                 compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                         ctx.getStart(), ctx.getText());
             }
         }
         if (ctx.requiresClause() != null) this.visit(ctx.requiresClause());
         if (ctx.ensuresClause() != null) this.visit(ctx.ensuresClause());

         ctx.variableDeclGroup().forEach(this::visit);
         ctx.stmt().forEach(this::visit);

         symtab.endScope();
         currentOpProcedureDecl = null;
         insertFunction(ctx.name, ctx.type(),
                 ctx.requiresClause(), ctx.ensuresClause(), ctx);
         return null;
     }*/

    private void insertFunction(Token name,
                                ResolveParser.TypeContext type,
                                ResolveParser.RequiresClauseContext requires,
                                ResolveParser.EnsuresClauseContext ensures,
                                ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.getScope(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            PTType returnType;
            if (type == null) {
                returnType = PTVoid.getInstance(g);
            } else {
                returnType = tr.progTypeValues.get(type);
            }

            PExp requiresExp = getPExpFor(requires);
            PExp ensuresExp = getPExpFor(ensures);
            //TODO: this will need to be wrapped in a ModuleParameterSymbol
            //if we're walking a specmodule param list
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(name.getText(), ctx, requiresExp,
                            ensuresExp, returnType, getRootModuleIdentifier(), params,
                            walkingModuleArgOrParamList));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    @Override public Void visitParameterDeclGroup(
            ResolveParser.ParameterDeclGroupContext ctx) {
        this.visit(ctx.type());
        PTType groupType = tr.progTypeValues.get(ctx.type());
        for (TerminalNode term : ctx.ID()) {
            try {
                ProgParameterSymbol.ParameterMode mode =
                        ProgParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                symtab.getInnermostActiveScope().define(
                        new ProgParameterSymbol(symtab.getTypeGraph(), term
                                .getText(), mode, groupType,
                                ctx, getRootModuleIdentifier()));
            } catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        term.getSymbol(), term.getText());
            }
        }
        return null;
    }

    @Override public Void visitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        //Todo: visit the generic arg types too
        ctx.moduleArgumentList().forEach(this::visit);
        ParseTreeProperty<List<ProgTypeSymbol>> facOrEnhToGenericArgs =
                new ParseTreeProperty<>();
        try {
            //map the base facility to any generic symbols parameterizing it
            facOrEnhToGenericArgs.put(ctx, new ArrayList<>());
            //getGenericArgumentSymsForFacilityOrEnh(ctx.type()));

            //now do the same for each enhancement pair
            /* for (ResolveParser.EnhancementPairDeclContext enh :
                     ctx.enhancementPairDecl()) {
                 //Todo: visit the generic arg types too
                 enh.moduleArgumentList().forEach(this::visit);
                 facOrEnhToGenericArgs.put(enh,
                         getGenericArgumentSymsForFacilityOrEnh(enh.type()));
             }*/
            symtab.getInnermostActiveScope().define(
                    new FacilitySymbol(ctx, getRootModuleIdentifier(),
                            facOrEnhToGenericArgs, symtab));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        return null;
    }
/*
     private List<ProgTypeSymbol> getGenericArgumentSymsForFacilityOrEnh(
             List<ResolveParser.TypeContext> actualTypes) {
         List<ProgTypeSymbol> result = new ArrayList<>();
         for (ResolveParser.TypeContext generic : actualTypes) {
             try {
                 result.add(symtab.getInnermostActiveScope()
                         .queryForOne(new NameQuery(generic.qualifier,
                                 generic.name, true)).toProgTypeSymbol());
             }
             catch (DuplicateSymbolException | NoSuchSymbolException e) {
                 compiler.errMgr.semanticError(e.getErrorKind(), generic.name,
                         generic.name.getText());
             }
             catch (UnexpectedSymbolException use) {
                 compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                         generic.getStart(), "a program type", generic.getText(),
                         use.getTheUnexpectedSymbolsDescription());
             }
         }
         return result;
     }*/

    @Override public Void visitNamedType(ResolveParser.NamedTypeContext ctx) {
        try {
            Token qualifier = ctx.qualifier;
            ProgTypeSymbol type =
                    symtab.getInnermostActiveScope()
                            .queryForOne(
                                    new NameQuery(qualifier, ctx.name, true))
                            .toProgTypeSymbol();

            tr.progTypeValues.put(ctx, type.getProgramType());
            tr.mathTypes.put(ctx, g.CLS);
            tr.mathTypeValues.put(ctx, type.getModelType());
            return null;
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    ctx.name.getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a type", ctx.name.getText(),
                    use.getTheUnexpectedSymbolsDescription());
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.progTypeValues.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
        tr.mathTypeValues.put(ctx, MTInvalid.getInstance(g));
        return null;
    }

    @Override public Void visitRecordType(ResolveParser.RecordTypeContext ctx) {
        Map<String, PTType> fields = new LinkedHashMap<>();
        for (ResolveParser.RecordVariableDeclGroupContext fieldGrp : ctx
                .recordVariableDeclGroup()) {
            this.visit(fieldGrp);
            PTType grpType = tr.progTypeValues.get(fieldGrp.type());
            for (TerminalNode t : fieldGrp.ID()) {
                fields.put(t.getText(), grpType);
            }
        }
        PTRecord record = new PTRecord(g, fields);
        tr.progTypeValues.put(ctx, record);
        tr.mathTypes.put(ctx, g.CLS);
        tr.mathTypeValues.put(ctx, record.toMath());
        return null;
    }

    @Override public Void visitTypeRepresentationDecl(
            ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
        ParseTree reprTypeNode = ctx.type();
        this.visit(reprTypeNode);

        try {
            curTypeReprModelSymbol = symtab.getInnermostActiveScope()
                    .queryForOne(new NameQuery(null, ctx.name,
                            false)).toTypeModelSymbol();
        } catch (NoSuchSymbolException | UnexpectedSymbolException nsse) {
            //this is actually ok for now. Facility module bound type reprs
            //won't have a model.
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(nsme.getErrorKind(),
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        }

        PTRepresentation reprType =
                new PTRepresentation(g, tr.progTypeValues.get(reprTypeNode),
                        ctx.name.getText(), curTypeReprModelSymbol,
                        getRootModuleIdentifier());
        try {
            String exemplarName = curTypeReprModelSymbol != null ?
                    curTypeReprModelSymbol.getExemplar().getName() :
                    ctx.name.getText().substring(0, 1).toUpperCase();
            symtab.getInnermostActiveScope().define(new ProgVariableSymbol(
                    exemplarName, ctx, reprType, getRootModuleIdentifier()));

        } catch (DuplicateSymbolException dse) {
            //This shouldn't be possible--the type declaration has a
            //scope all its own and we're the first ones to get to
            //introduce anything
            throw new RuntimeException(dse);
        }
        if (ctx.conventionClause() != null) this.visit(ctx.conventionClause());
        if (ctx.correspondenceClause() != null)
            this.visit(ctx.correspondenceClause());
        if (ctx.typeImplInit() != null) this.visit(ctx.typeImplInit());
        symtab.endScope();
        PExp convention = getPExpFor(ctx.conventionClause());
        PExp correspondence = getPExpFor(ctx.correspondenceClause());
        try {
            ProgReprTypeSymbol rep = new ProgReprTypeSymbol(g,
                    ctx.name.getText(), ctx, getRootModuleIdentifier(),
                    curTypeReprModelSymbol, reprType, convention,
                    correspondence);
            reprType.setReprTypeSymbol(rep);
            symtab.getInnermostActiveScope().define(rep);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }
        curTypeReprModelSymbol = null;
        return null;
    }
/*
     @Override public Void visitVariableDeclGroup(
             ResolveParser.VariableDeclGroupContext ctx) {
         this.visit(ctx.type());
         insertVariables(ctx, ctx.ID(), ctx.type());
         return null;
     }

     @Override public Void visitMathAssertionDecl(
             ResolveParser.MathAssertionDeclContext ctx) {
         symtab.startScope(ctx);
         this.visit(ctx.mathAssertionExp());
         symtab.endScope();
         checkMathTypes(ctx.mathAssertionExp(), g.BOOLEAN);
         try {
             PExp assertion = getPExpFor(ctx.mathAssertionExp());
             symtab.getInnermostActiveScope().define(
                     new TheoremSymbol(g, ctx.name.getText(), assertion,
                             ctx, getRootModuleIdentifier()));
         } catch (DuplicateSymbolException dse) {
             compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                     ctx.name, ctx.name.getText());
         }
         emit("new theorem: " + ctx.name.getText());
         return null;
     }

     private void checkMathTypes(ParserRuleContext ctx, MTType expected) {
         MTType foundType = tr.mathTypes.get(ctx);
         if (!foundType.equals(expected)) {
             compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_TYPE,
                     ctx.getStart(), expected, foundType);
         }
     }*/

    @Override public Void visitMathCategoricalDefinitionDecl(
            ResolveParser.MathCategoricalDefinitionDeclContext ctx) {
        for (ResolveParser.MathPrefixDefinitionSigContext sig :
                ctx.mathPrefixDefinitionSig()) {
            symtab.startScope(sig);
            this.visit(sig);
            symtab.endScope();
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, sig.name.getText(),
                                tr.mathTypes.get(sig), null, ctx,
                                getRootModuleIdentifier()));
            } catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        sig.name.getStart(), sig.name.getText());
            }
        }
        //visit the rhs of our categorical defn
        this.visit(ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInductiveDefinitionDecl(
            ResolveParser.MathInductiveDefinitionDeclContext ctx) {
        symtab.startScope(ctx);
        ResolveParser.MathDefinitionSigContext sig = ctx.mathDefinitionSig();
        ParserRuleContext baseCase = ctx.mathAssertionExp(0);
        ParserRuleContext indHypo = ctx.mathAssertionExp(1);

        //note that 'sig' adds a binding for the name to the active scope
        //so baseCase and indHypo will indeed be able to see the symbol we're
        //introducing here.
        this.visit(sig);
        this.visit(baseCase);
        this.visit(indHypo);

        checkMathTypes(baseCase, g.BOOLEAN);
        checkMathTypes(indHypo, g.BOOLEAN);
        symtab.endScope();
        MTType defnType = tr.mathTypes.get(ctx.mathDefinitionSig());
        Token name = getSignatureName(ctx.mathDefinitionSig());
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, name.getText(),
                            defnType, null, ctx, getRootModuleIdentifier()));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
        definitionSchematicTypes.clear();
        return null;
    }

    private void checkMathTypes(ParserRuleContext ctx, MTType expected) {
        MTType foundType = tr.mathTypes.get(ctx);
        if (!foundType.equals(expected)) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_TYPE,
                    ctx.getStart(), expected, foundType);
        }
    }

    @Override public Void visitMathStandardDefinitionDecl(
            ResolveParser.MathStandardDefinitionDeclContext ctx) {
        ResolveParser.MathDefinitionSigContext sig = ctx.mathDefinitionSig();
        symtab.startScope(ctx);
        this.visit(sig);

        MTType defnType = tr.mathTypes.get(sig);
        MTType defnTypeValue = null;

        Token name = getSignatureName(ctx.mathDefinitionSig());

        if (ctx.mathAssertionExp() != null) {
            //Note: We DO have to visit the rhs assertion explicitly here,
            //as it exists a level above the signature.
            this.visit(ctx.mathAssertionExp());
            defnTypeValue = tr.mathTypeValues.get(ctx.mathAssertionExp());
        }
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, name.getText(),
                            definitionSchematicTypes, defnType, defnTypeValue,
                            ctx, getRootModuleIdentifier()));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
        definitionSchematicTypes.clear();
        return null;
    }

    private Token getSignatureName(
            @NotNull ResolveParser.MathDefinitionSigContext signature) {
        CommonToken result;
        if (signature.mathPrefixDefinitionSig() != null) {
            result = new CommonToken(signature
                    .mathPrefixDefinitionSig().name.getStart());
            result.setText(signature.mathPrefixDefinitionSig().name.getText());
        } else if (signature.mathInfixDefinitionSig() != null) {
            result = new CommonToken(signature
                    .mathInfixDefinitionSig().name.getStart());
            result.setText(signature.mathInfixDefinitionSig().name.getText());
        } else if (signature.mathOutfixDefinitionSig() != null) {
            ResolveParser.MathOutfixDefinitionSigContext o =
                    signature.mathOutfixDefinitionSig();
            result = new CommonToken(o.leftSym.getStart());
            result.setText(o.leftSym.getText() + "..." + o.rightSym.getText());
        } else {
            throw new UnsupportedOperationException("odd looking definition " +
                    "signature..: " + signature.getText());
        }
        return result;
    }

    /** Since 'MathDefinitionSig' appears all over the place within our three
     *  styles of definitions (categorical, standard, and inductive), we simply
     *  use this signature visitor method to visit and type all relevant
     *  children. This way the top level definition nodes can simply grab the type
     *  of the signature and build/populate the appropriate object. However,
     *  know that in the defn top level nodes, we must remember to start scope,
     *  visit the signature, and end scope. We don't do this in the signature
     *  because certain information (i.e. body) is rightfully not present.
     *  <p>
     *  <p>Note also that here we also add a binding for the name of this
     *  sig to the active scope (so inductive and implicit definitions may
     *  reference themselves).</p>
     */
    @Override public Void visitMathDefinitionSig(
            ResolveParser.MathDefinitionSigContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathPrefixDefinitionSig(
            ResolveParser.MathPrefixDefinitionSigContext ctx) {
        typeMathDefinitionSignature(ctx, ctx.mathVariableDeclGroup(),
                ctx.mathTypeExp(), ctx.name.getStart());
        return null;
    }

    @Override public Void visitMathInfixDefinitionSig(
            ResolveParser.MathInfixDefinitionSigContext ctx) {
        typeMathDefinitionSignature(ctx, ctx.mathVariableDecl(),
                ctx.mathTypeExp(), ctx.name.getStart());
        return null;
    }

    @Override public Void visitMathOutfixDefinitionSig(
            ResolveParser.MathOutfixDefinitionSigContext ctx) {
        List<ResolveParser.MathVariableDeclContext> formals = new ArrayList<>();
        formals.add(ctx.mathVariableDecl());
        typeMathDefinitionSignature(ctx, formals,
                ctx.mathTypeExp(), getSignatureName(
                        (ResolveParser.MathDefinitionSigContext) ctx.getParent()));
        return null;
    }

    private void typeMathDefinitionSignature(@NotNull ParserRuleContext ctx,
                                             @NotNull List<? extends ParseTree> formals,
                                             @NotNull ResolveParser.MathTypeExpContext type,
                                             @NotNull Token name) {
        //first visit the formal params
        activeQuantifications.push(Quantification.UNIVERSAL);
        walkingDefParams = true;
        formals.forEach(this::visit);
        walkingDefParams = false;
        activeQuantifications.pop();

        //next, visit the definition's 'return type' to give it a type
        this.visit(type);

        //finally, build the full type of this definition's signature
        //If there are no params, then it is just the sym after the ':'
        MTType defnType = tr.mathTypeValues.get(type);
        MTFunction.MTFunctionBuilder builder =
                new MTFunction.MTFunctionBuilder(g, defnType);

        //if there ARE params, then our type needs to be an MTFunction.
        //this if check needs to be here or else, even if there were no params,
        //our type would end up MTFunction: Void -> T (which we don't want)
        if (!formals.isEmpty()) {

            //It's either going to be a list of MathvariableDecl's or
            //MathVariableDeclGroups
            if (formals.get(0) instanceof ResolveParser.MathVariableDeclContext) {
                for (ParseTree formal : formals) {
                    ResolveParser.MathVariableDeclContext var =
                            (ResolveParser.MathVariableDeclContext) formal;
                    MTType varType = tr.mathTypeValues.get(var.mathTypeExp());
                    builder.paramTypes(varType);
                    builder.paramNames(var.ID().getText());
                }
            } else {
                for (ParseTree formal : formals) {
                    ResolveParser.MathVariableDeclGroupContext grp =
                            (ResolveParser.MathVariableDeclGroupContext) formal;
                    MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
                    for (TerminalNode t : grp.ID()) {
                        builder.paramTypes(grpType);
                        builder.paramNames(t.getText());
                    }

                }
            }
            //if the definition has parameters then it's type should be an
            //MTFunction (e.g. something like a * b ... -> ...)
            defnType = builder.build();
        }
        try {
            //Because we're a decl, we can safely say that this will only apply
            //when walking a definition declared in a formal module param list
            //(not the actual arg list!)
            Symbol defnSym = new MathSymbol(g, name.getText(),
                    defnType, null, ctx, getRootModuleIdentifier());
            if (walkingModuleArgOrParamList) {
                defnSym = new ModuleParameterSymbol((MathSymbol) defnSym);
            }
            symtab.getInnermostActiveScope().define(defnSym);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
        tr.mathTypes.put(ctx, defnType);
    }

    @Override public Void visitMathVariableDecl(
            ResolveParser.MathVariableDeclContext ctx) {
        insertMathVariables(ctx, ctx.mathTypeExp(), ctx.ID());
        return null;
    }

    @Override public Void visitMathVariableDeclGroup(
            ResolveParser.MathVariableDeclGroupContext ctx) {
        insertMathVariables(ctx, ctx.ID(), ctx.mathTypeExp());
        return null;
    }

    private void insertMathVariables(ParserRuleContext ctx,
                                     ResolveParser.MathTypeExpContext type,
                                     TerminalNode... terms) {
        insertMathVariables(ctx, Arrays.asList(terms), type);
    }

    private void insertMathVariables(ParserRuleContext ctx,
                                     List<TerminalNode> terms,
                                     ResolveParser.MathTypeExpContext type) {
        this.visit(type);
        MTType mathTypeValue = tr.mathTypeValues.get(type);
        for (TerminalNode term : terms) {
            if (walkingDefParams
                    && mathTypeValue.isKnownToContainOnlyMTypes()) {
                definitionSchematicTypes.put(term.getText(), mathTypeValue);
            }
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, term.getText(), activeQuantifications
                                .peek(), mathTypeValue, null, ctx,
                                getRootModuleIdentifier()));
            } catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        term.getSymbol(), term.getText());
            }
            emit("  new math var: " + term.getText() + " of type "
                    + mathTypeValue.toString() + " with quantification " +
                    activeQuantifications.peek());
            tr.mathTypes.put(ctx, mathTypeValue);
        }
    }

    /*private void insertVariables(@NotNull ParserRuleContext ctx,
                                 List<TerminalNode> terminalGroup,
                                 ResolveParser.TypeContext type) {
        PTType progType = tr.progTypeValues.get(type);
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol vs =
                        new ProgVariableSymbol(t.getText(), ctx, progType,
                                getRootModuleIdentifier());
                symtab.getInnermostActiveScope().define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public Void visitRequiresClause(
            ResolveParser.RequiresClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        if ( ctx.entailsClause() != null ) this.visit(ctx.entailsClause());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        if ( ctx.getParent().getParent() instanceof
                ResolveParser.ModuleContext ) {
            insertGlobalAssertion(ctx,
                    GlobalMathAssertionSymbol.ClauseType.REQUIRES,
                    ctx.mathAssertionExp());
        }
        return null;
    }

    @Override public Void visitEntailsClause(
            ResolveParser.EntailsClauseContext ctx) {
        this.visitChildren(ctx);    //Todo : For now.
        return null;
    }

    @Override public Void visitEnsuresClause(
            ResolveParser.EnsuresClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitConstraintClause(
            ResolveParser.ConstraintClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        if ( ctx.getParent().getParent().getParent() instanceof
                ResolveParser.ModuleContext ) {
            insertGlobalAssertion(ctx,
                    GlobalMathAssertionSymbol.ClauseType.CONSTRAINT,
                    ctx.mathAssertionExp());
        }
        return null;
    }

    @Override public Void visitCorrespondenceClause(
            ResolveParser.CorrespondenceClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    //---------------------------------------------------
    // P R O G    E X P    T Y P I N G
    //---------------------------------------------------

    @Override public Void visitProgNestedExp(ResolveParser.ProgNestedExpContext ctx) {
        this.visit(ctx.progExp());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progExp()));
        return null;
    }

    @Override public Void visitProgPrimaryExp(
            ResolveParser.ProgPrimaryExpContext ctx) {
        this.visit(ctx.progPrimary());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progPrimary()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progPrimary()));
        return null;
    }

    @Override public Void visitProgPrimary(ResolveParser.ProgPrimaryContext ctx) {
        this.visit(ctx.getChild(0));
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.getChild(0)));
        return null;
    }

    @Override public Void visitProgVarExp(ResolveParser.ProgVarExpContext ctx) {
        this.visit(ctx.getChild(0));
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.getChild(0)));
        return null;
    }

    @Override public Void visitProgNamedExp(
            ResolveParser.ProgNamedExpContext ctx) {
        try {
            ProgVariableSymbol variable =
                    symtab.getInnermostActiveScope().queryForOne(
                            new ProgVariableQuery(ctx.qualifier, ctx.name,
                                    false));
            tr.progTypes.put(ctx, variable.getProgramType());
            typeMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
            return null;
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
        catch (UnexpectedSymbolException use) {
            //ok, maybe we're dealing with a reference to an operation then?
            if (walkingModuleArgOrParamList) {
                Symbol s = null;
                try {
                    s = symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(ctx.qualifier,
                                    ctx.name, true));
                    //TODO: get rid of ModuleParameterizableSymbol and just use OperationSymbol here.
                    if (s instanceof ModuleParameterizableSymbol) {
                        tr.progTypes.put(ctx, ((ModuleParameterizableSymbol) s).getProgramType());
                        tr.mathTypes.put(ctx, ((ModuleParameterizableSymbol) s).getMathType());
                        return null;
                    }
                } catch (NoSuchSymbolException|DuplicateSymbolException e) {
                    compiler.errMgr.semanticError(e.getErrorKind(), ctx.name,
                            ctx.name.getText());
                }
            }
            //nope. not expecting this at all if we got here. Let's indicate this
            //and type the thing as invalid.
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.name, "a variable reference", ctx.name.getText(),
                    use.getTheUnexpectedSymbolsDescription());
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
        return null;
    }

    @Override public Void visitModuleArgument(
            ResolveParser.ModuleArgumentContext ctx) {
        this.visit(ctx.progExp());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progExp()));
        return null;
    }

    @Override public Void visitModuleArgumentList(
            ResolveParser.ModuleArgumentListContext ctx) {
        walkingModuleArgOrParamList = true;
        this.visitChildren(ctx);
        walkingModuleArgOrParamList = false;
        return null;
    }

    @Override public Void visitProgMemberExp(
            ResolveParser.ProgMemberExpContext ctx) {
        ParseTree firstRecordRef = ctx.getChild(0);
        this.visit(firstRecordRef);
        PTType first = tr.progTypes.get(firstRecordRef);

        //start by checking the first to ensure we're dealing with a record
        if ( !first.isAggregateType() ) {
            compiler.errMgr.semanticError(
                    ErrorKind.ILLEGAL_MEMBER_ACCESS, ctx.getStart(),
                    ctx.getText(), ctx.getChild(0).getText());
            tr.progTypes.put(ctx, PTInvalid.getInstance(g));
            tr.mathTypes.put(ctx, g.INVALID);
            return null;
        }
        PTRepresentation curAggregateType = (PTRepresentation) first;

        //note this will represent the rightmost field type when finished.
        PTType curFieldType = curAggregateType;

        //now we need to make sure our mem accesses aren't nonsense.
        for (TerminalNode term : ctx.ID()) {
            PTRecord recordType = (PTRecord) curAggregateType.getBaseType();
            curFieldType = recordType.getFieldType(term.getText());
            if ( curFieldType == null ) {
                compiler.errMgr.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                        term.getSymbol(), term.getText());
                curFieldType = PTInvalid.getInstance(g);
                tr.progTypes.put(term, curFieldType);
                tr.mathTypes.put(term, curFieldType.toMath());
                break;
            }
            tr.progTypes.put(term, curFieldType);
            tr.mathTypes.put(term, curFieldType.toMath());

            if ( curFieldType.isAggregateType() ) {
                curAggregateType = (PTRepresentation) curFieldType;
            }
        }
        tr.progTypes.put(ctx, curFieldType);
        tr.mathTypes.put(ctx, curFieldType.toMath());
        return null;
    }*/

    /*@Override public Void visitProgInfixExp(
            ResolveParser.ProgInfixExpContext ctx) {
        ctx.progExp().forEach(this::visit);
        List<PTType> argTypes = ctx.progExp().stream()
                .map(tr.progTypes::get).collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes attr =
                HardCodedProgOps.convert(ctx.op, argTypes);
        typeOperationRefExp(ctx, attr.qualifier, attr.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgUnaryExp(ResolveParser.ProgUnaryExpContext ctx) {
        this.visit(ctx.progExp());
        if (ctx.NOT() != null) {
            HardCodedProgOps.BuiltInOpAttributes attr =
                    HardCodedProgOps.convert(ctx.op, tr.progTypes.get(ctx.progExp()));
            typeOperationRefExp(ctx, attr.qualifier, attr.name, ctx.progExp());
        }
        else {
            //minus is overloaded WITHIN integer template, so for now we'll just handle it this way.
            Token qualifier =  Utils.createTokenFrom(ctx.getStart(), "Std_Integer_Fac");
            Token name =  Utils.createTokenFrom(ctx.getStart(), "Negate");
            typeOperationRefExp(ctx, qualifier, name, ctx.progExp());
        }

        return null;
    }

    @Override public Void visitProgPostfixExp(ResolveParser.ProgPostfixExpContext ctx) {
        this.visit(ctx.progExp());
        HardCodedProgOps.BuiltInOpAttributes attr =
                HardCodedProgOps.convert(ctx.op, tr.progTypes.get(ctx.progExp()));
        typeOperationRefExp(ctx, attr.qualifier, attr.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgParamExp(
            ResolveParser.ProgParamExpContext ctx) {
        ctx.progExp().forEach(this::visit);
        typeOperationRefExp(ctx, ctx.qualifier, ctx.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgBooleanLiteralExp(
            ResolveParser.ProgBooleanLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Boolean_Fac", "Boolean");
    }

    @Override public Void visitProgIntegerLiteralExp(
            ResolveParser.ProgIntegerLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Integer_Fac", "Integer");
    }

    @Override public Void visitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Character_Fac", "Character");
    }

    @Override public Void visitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Char_Str_Fac", "Char_Str");
    }

    private Void typeProgLiteralExp(ParserRuleContext ctx,
                                    String typeQualifier, String typeName) {
        ProgTypeSymbol p =
                getProgTypeSymbol(ctx, typeQualifier, typeName);
        tr.progTypes.put(ctx, p != null ? p.getProgramType() :
                PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, p != null ? p.getModelType() :
                MTInvalid.getInstance(g));
        return null;
    }

    private ProgTypeSymbol getProgTypeSymbol(ParserRuleContext ctx,
            String typeQualifier, String typeName) {
        CommonToken qualifierToken = new CommonToken(ctx.getStart());
        qualifierToken.setText(typeQualifier);
        qualifierToken.setType(ResolveLexer.ID);

        CommonToken nameToken = new CommonToken(ctx.getStart());
        nameToken.setText(typeName);
        nameToken.setType(ResolveLexer.ID);

        ProgTypeSymbol result = null;
        try {
            result = symtab.getInnermostActiveScope().queryForOne(
                            new NameQuery(qualifierToken, nameToken,false))
                            .toProgTypeSymbol();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(),
                    ctx.getStart(), typeName);
        }
        return result;
    }

    protected void typeOperationRefExp(ParserRuleContext ctx,
                                       Token qualifier, Token name,
                                       ResolveParser.ProgExpContext... args) {
        typeOperationRefExp(ctx, qualifier, name, Arrays.asList(args));
    }

    protected void typeOperationRefExp(ParserRuleContext ctx,
                                       Token qualifier, Token name,
                                       List<ResolveParser.ProgExpContext> args) {
        List<PTType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        //for recursive calls
        if (currentOpProcedureDecl != null || currentProcedureDecl != null) {
            String currentName = currentOpProcedureDecl != null ?
                    currentOpProcedureDecl.name.getText() :
                    currentProcedureDecl.name.getText();

            boolean isMarkedRecursive = currentOpProcedureDecl != null ?
                    currentOpProcedureDecl.recursive != null :
                    currentProcedureDecl.recursive != null;

            ResolveParser.OperationParameterListContext formalParamListNode =
                    currentOpProcedureDecl != null ?
                            currentOpProcedureDecl.operationParameterList() :
                            currentProcedureDecl.operationParameterList();

            ResolveParser.TypeContext declaredTypeCtx =
                    currentOpProcedureDecl != null ?
                            currentOpProcedureDecl.type() :
                            currentProcedureDecl.type();

            if (currentName.equals(name.getText()) && qualifier == null) {
                if (formalParamListNode.parameterDeclGroup().size() !=
                        argTypes.size()) {
                    compiler.errMgr.semanticError(ErrorKind.MALFORMED_RECURSIVE_OP_CALL,
                            ctx.getStart(), ctx.getText(),
                            currentOpProcedureDecl.name.getText());
                    tr.progTypes.put(ctx, PTInvalid.getInstance(g));
                    tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
                    return;
                }
                PTType t = declaredTypeCtx != null ?
                        tr.progTypeValues.get(declaredTypeCtx) :
                        PTVoid.getInstance(g);
                tr.progTypes.put(ctx, t);
                tr.mathTypes.put(ctx, t.toMath());
                return;
            }
        }

        //every other call
        try {
            OperationSymbol opSym = symtab.getInnermostActiveScope().queryForOne(
                    new OperationQuery(qualifier, name, argTypes,
                            MathSymbolTable.FacilityStrategy.FACILITY_INSTANTIATE,
                            MathSymbolTable.ImportStrategy.IMPORT_NAMED));

            tr.progTypes.put(ctx, opSym.getReturnType());
            tr.mathTypes.put(ctx, opSym.getReturnType().toMath());
            return;
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            List<String> argStrList = args.stream()
                    .map(ResolveParser.ProgExpContext::getText)
                    .collect(Collectors.toList());
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_OPERATION,
                    ctx.getStart(), name.getText(), argStrList, argTypes);
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
    }
    private void handleRecursiveOperationRef(ParserRuleContext ctx,
                                             Token qualifier, Token name,
                                             List<PTType> actualArgTypes) {
    }

    //---------------------------------------------------
    //  M A T H   E X P   T Y P I N G
    //---------------------------------------------------
*/
    @Override public Void visitMathTypeExp(
            ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth++;
        this.visit(ctx.mathExp());
        typeValueDepth--;

        MTType type = tr.mathTypes.get(ctx.mathExp());
        MTType typeValue = tr.mathTypeValues.get(ctx.mathExp());
        if (typeValue == null) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            typeValue = g.INVALID;
        }
        tr.mathTypes.put(ctx, type);
        tr.mathTypeValues.put(ctx, typeValue);
        return null;
    }

    @Override public Void visitMathCrossTypeExp(
            ResolveParser.MathCrossTypeExpContext ctx) {
        typeValueDepth++;
        ctx.mathVariableDeclGroup().forEach(this::visit);

        List<MTCartesian.Element> fieldTypes = new ArrayList<>();
        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
            for (TerminalNode t : grp.ID()) {
                fieldTypes.add(new MTCartesian.Element(t.getText(), grpType));
            }
        }
        tr.mathTypes.put(ctx, g.CLS);
        tr.mathTypeValues.put(ctx, new MTCartesian(g, fieldTypes));
        typeValueDepth--;
        return null;
    }

    @Override public Void visitMathTypeAssertionExp(
            ResolveParser.MathTypeAssertionExpContext ctx) {
        if (typeValueDepth == 0) {
            this.visit(ctx.mathExp());
        }
        this.visit(ctx.mathTypeExp());
        if (typeValueDepth > 0) {
            try {
                //Todo: Check to ensure mathExp is in fact a variableExp
                MTType assertedType = tr.mathTypes.get(ctx.mathTypeExp());
                symtab.getInnermostActiveScope().addBinding(
                        ctx.mathExp().getText(), Quantification.UNIVERSAL,
                        ctx.mathExp(), tr.mathTypes.get(ctx.mathTypeExp()));

                tr.mathTypes.put(ctx, assertedType);
                tr.mathTypeValues.put(ctx,
                        new MTNamed(g, ctx.mathExp().getText()));

                //Don't forget to set the type for the var on the lhs of ':'!
                //Todo: Don't know a better way of getting the bottommost rulectx.
                //maybe write a utils method for that? Or read more about the api.
                ParseTree x =
                        ctx.mathExp().getChild(0).getChild(0);
                tr.mathTypes.put(x, assertedType);

                definitionSchematicTypes.put(ctx.mathExp().getText(),
                        tr.mathTypes.get(ctx.mathTypeExp()));
                emit("Added schematic variable: "
                        + ctx.mathExp().getText());
            } catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.mathExp().getStart(), ctx.mathExp().getText());
            }
        }
        return null;
    }

    @Override public Void visitMathNestedExp(
            ResolveParser.MathNestedExpContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathAssertionExp(
            ResolveParser.MathAssertionExpContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathPrimeExp(
            ResolveParser.MathPrimeExpContext ctx) {
        this.visit(ctx.mathPrimaryExp());
        chainMathTypes(ctx, ctx.mathPrimaryExp());
        return null;
    }

    @Override public Void visitMathPrimaryExp(
            ResolveParser.MathPrimaryExpContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathSetComprehensionExp(
            ResolveParser.MathSetComprehensionExpContext ctx) {
        this.visit(ctx.mathVariableDecl());
        this.visit(ctx.mathAssertionExp());
        checkMathTypes(ctx.mathAssertionExp(), g.BOOLEAN);
        MTType comprehensionType = new MTPowersetApplication(g,
                tr.mathTypeValues.get(ctx.mathVariableDecl().mathTypeExp()));
        tr.mathTypes.put(ctx, g.SSET);
        tr.mathTypeValues.put(ctx, comprehensionType);
        emit("expression: " + ctx.getText() + " of type: " + comprehensionType);
        return null;
    }

    @Override public Void visitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        if (ctx.mathExp().isEmpty()) {
            tr.mathTypes.put(ctx, g.EMPTY_SET);
        } else {
            //Temp, until we figure out down from up
            MTFunction setType =
                    new MTFunction.MTFunctionBuilder(g, g.SSET)
                            .paramTypes(tr.mathTypes.get(ctx.mathExp().get(0)))
                            .build();
            tr.mathTypes.put(ctx, g.SSET);
            tr.mathTypeValues.put(ctx, setType);
        }
        return null;
    }

    /* @Override public Void visitMathLambdaExp(
            ResolveParser.MathLambdaExpContext ctx) {
        symtab.startScope(ctx);
        emit("lambda exp: " + ctx.getText());

        walkingDefParams = true;
        activeQuantifications.push(Quantification.UNIVERSAL);
        ctx.mathVariableDeclGroup().forEach(this::visit);
        activeQuantifications.pop();
        walkingDefParams = false;

        this.visit(ctx.mathExp());
        symtab.endScope();

        List<MTType> parameterTypes = new LinkedList<>();
        for (ResolveParser.MathVariableDeclGroupContext grp :
                ctx.mathVariableDeclGroup()) {
            MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
            parameterTypes.addAll(grp.ID().stream()
                    .map(term -> grpType).collect(Collectors.toList()));
        }
        tr.mathTypes.put(ctx, new MTFunction.MTFunctionBuilder(g, tr.mathTypes
                .get(ctx.mathExp())).paramTypes(parameterTypes).build());
        return null;
    }

    @Override public Void visitMathAlternativeExp(
            ResolveParser.MathAlternativeExpContext ctx) {

        MTType establishedType = null;
        MTType establishedTypeValue = null;
        for (ResolveParser.MathAlternativeItemExpContext alt : ctx
                .mathAlternativeItemExp()) {
            this.visit(alt.result);
            if (alt.condition != null) this.visit(alt.condition);
            if ( establishedType == null ) {
                establishedType = tr.mathTypes.get(alt.result);
                establishedTypeValue = tr.mathTypeValues.get(alt.result);
            }
            else {
                if ( alt.condition != null ) {
                    // expectType(alt, establishedType);
                }
            }
        }
        tr.mathTypes.put(ctx, establishedType);
        tr.mathTypeValues.put(ctx, establishedTypeValue);
        return null;
    }

    @Override public Void visitMathAlternativeItemExp(
            ResolveParser.MathAlternativeItemExpContext ctx) {
        if ( ctx.condition != null ) {
            //expectType(ctx.condition, g.BOOLEAN);
        }
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.result));
        tr.mathTypeValues.put(ctx, tr.mathTypeValues.get(ctx.result));
        return null;
    }*/

    @Override public Void visitMathQuantifiedExp(
            ResolveParser.MathQuantifiedExpContext ctx) {
        emit("entering mathQuantifiedExp...");
        symtab.startScope(ctx);
        Quantification quantification;

        switch (ctx.q.getType()) {
            case ResolveLexer.FORALL:
                quantification = Quantification.UNIVERSAL;
                break;
            case ResolveLexer.EXISTS:
                quantification = Quantification.EXISTENTIAL;
                break;
            default:
                throw new RuntimeException("unrecognized quantification type: "
                        + ctx.q.getText());
        }
        activeQuantifications.push(quantification);
        this.visit(ctx.mathVariableDeclGroup());
        activeQuantifications.pop();

        activeQuantifications.push(Quantification.NONE);
        this.visit(ctx.mathAssertionExp());
        activeQuantifications.pop();
        emit("exiting mathQuantifiedExp.");
        symtab.endScope();
        tr.mathTypes.put(ctx, g.BOOLEAN);
        return null;
    }

    @Override public Void visitMathInfixApplyExp(
            ResolveParser.MathInfixApplyExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
        return null;
    }

    @Override public Void visitMathOutfixApplyExp(
            ResolveParser.MathOutfixApplyExpContext ctx) {
        this.visit(ctx.mathExp());
        typeMathFunctionLikeThing(ctx, null, new CommonToken(ResolveLexer.ID,
                ctx.lop.getText() + "..." + ctx.rop.getText()), ctx.mathExp());
        return null;
    }

    @Override public Void visitMathPrefixApplyExp(
            ResolveParser.MathPrefixApplyExpContext ctx) {
        //System.out.println("mth prefix apply ctx text= "+ctx.getText());
        anonymousApplicationDepth++;
        this.visit(ctx.functionExp);
        anonymousApplicationDepth--;
        //looks weird cause the 0th is now the expr representing the
        //'function's first class' name (and  type)
        List<ResolveParser.MathExpContext> args =
                ctx.mathExp().subList(1, ctx.mathExp().size());
        walkingFunctionAppArgs = true;
        args.forEach(this::visit);
        walkingFunctionAppArgs = false;
        typeMathFunctionLikeThing(ctx, ctx.functionExp, args);
        return null;
    }

    @Override public Void visitMathBooleanLiteralExp(
            ResolveParser.MathBooleanLiteralExpContext ctx) {
        typeMathSymbolExp(ctx, null, ctx.getText());
        return null;
    }

    @Override public Void visitMathIntegerLiteralExp(
            ResolveParser.MathIntegerLiteralExpContext ctx) {
        typeMathSymbolExp(ctx, ctx.qualifier, ctx.num.getText());
        return null;
    }

    @Override public Void visitMathSymbolExp(
            ResolveParser.MathSymbolExpContext ctx) {
        if (prevMathSelectorAccess != null) {
            typeMathSelectorAccessExp(ctx, prevMathSelectorAccess,
                    ctx.name.getText());
        }
        else {
            typeMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
        }
        return null;
    }

    private void typeMathSelectorAccessExp(@NotNull ParserRuleContext ctx,
                                           @NotNull ResolveParser.MathExpContext prevAccessExp,
                                           @NotNull String symbolName) {

        MTType type = g.INVALID;
        MTType prevMathAccessType = tr.mathTypes.get(prevAccessExp);
        //Todo: This can't go into {@link TypeGraph#getMetaFieldType()} since
        //it starts the access chain, rather than say terminating it.
        if (prevAccessExp.getText().equals("conc")) {
            tr.mathTypes.put(ctx, curTypeReprModelSymbol.getModelType());
            return;
        }
        try {
            MTCartesian typeCartesian = (MTCartesian) prevMathAccessType;
            type = typeCartesian.getFactor(symbolName);
        }
        catch (ClassCastException cce) {
            type = HardCoded.getMetaFieldType(g, symbolName);
            if (type == null) {
                compiler.errMgr.semanticError(
                        ErrorKind.VALUE_NOT_TUPLE, ctx.getStart(),
                        symbolName);
                type = g.INVALID;
            }
        }
        catch (NoSuchElementException nsee) {
            type = HardCoded.getMetaFieldType(g, symbolName);
            if (type == null) {
                compiler.errMgr.semanticError(
                        ErrorKind.NO_SUCH_FACTOR, ctx.getStart(),
                        symbolName);
                type = g.INVALID;
            }
        }
        tr.mathTypes.put(ctx, type);
    }

    @Override public Void visitMathSelectorExp(
            ResolveParser.MathSelectorExpContext ctx) {
        this.visit(ctx.lhs);
        prevMathSelectorAccess = ctx.mathExp(0);
        this.visit(ctx.rhs);
        prevMathSelectorAccess = null;

        MTType finalType = tr.mathTypes.get(ctx.rhs);
        compiler.info("expression: " + ctx.getText() + " of type " + finalType);
        tr.mathTypes.put(ctx, finalType);
        return null;
    }

    private MathSymbol typeMathSymbolExp(ParserRuleContext ctx,
                                         Token qualifier,
                                         String symbolName) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);
        if (intendedEntry == null) {
            tr.mathTypes.put(ctx, g.INVALID);
        } else {
            tr.mathTypes.put(ctx, intendedEntry.getType());
            setSymbolTypeValue(ctx, symbolName, intendedEntry);
        }
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
                                        ParserRuleContext ctx) {
        try {
            return symtab.getInnermostActiveScope()
                    .queryForOne(new MathSymbolQuery(qualifier,
                            symbolName, ctx.getStart()));
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    symbolName);
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(nsme.getErrorKind(),
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a math symbol", symbolName,
                    use.getTheUnexpectedSymbolsDescription());
        }
        return null;
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
                                    MathSymbol intendedEntry) {
        try {
            if (anonymousApplicationDepth > 0) return; //hmmm..

            if (intendedEntry.getQuantification() == Quantification.NONE) {
                tr.mathTypeValues.put(ctx, intendedEntry.getTypeValue());
            } else {
                if (intendedEntry.getType().isKnownToContainOnlyMTypes()) {
                    tr.mathTypeValues.put(ctx, new MTNamed(g, symbolName));
                }
            }
        } catch (SymbolNotOfKindTypeException snokte) {
            if (typeValueDepth > 0) {
                //TODO: So in <something> : Sp_Loc(k), the k clearly WOULDN't have a value,
                //so I'm just going to make it Z. I don't understand why arguments to
                //a function application serving as a type designator necessarily need values...

                //Another thought, why the hell would Powerset(Z) typecheck then... Z wouldn't have a value...
                if (walkingFunctionAppArgs) {
                    tr.mathTypeValues.put(ctx, new MTNamed(g, symbolName));
                } else {
                    tr.mathTypeValues.put(ctx, g.INVALID);
                }
            }
        }
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
                                           @Nullable Token qualifier,
                                           @NotNull Token name,
                                           @NotNull ParserRuleContext... args) {

        typeMathFunctionLikeThing(ctx, qualifier, name, Arrays.asList(args));
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
                                           @NotNull ParserRuleContext functionPortion,
                                           @NotNull List<? extends ParserRuleContext> args) {

        emitPreApplicationType(ctx, args);
        ParseTree kid = functionPortion.getChild(0).getChild(0);

        //We're dealing with a simple function application: e.g.: Powerset(Z);
        if (kid instanceof ResolveParser.MathSymbolExpContext) {
            ResolveParser.MathSymbolExpContext kidAsSym =
                    (ResolveParser.MathSymbolExpContext) kid;

            typeMathFunctionLikeThing(ctx, kidAsSym.qualifier,
                    kidAsSym.mathSymbolName().getStart(), args);
        }
        //we're dealing with a more exotic, curried, anonymous
        //application: i.e.: SS(k)(Cen(k))
        else {
            MTType expectedType = anonymousFunctionExpectedRangeTypes.get(functionPortion);
            if (expectedType == null || !(expectedType instanceof MTFunction)) {
                tr.mathTypes.put(ctx, g.INVALID);
                return;
            }
            MTFunction expectedAsFxn = (MTFunction) expectedType;
            tr.mathTypes.put(ctx, expectedAsFxn.getRange());

            //I had better identify a type
            if (typeValueDepth > 0) {
                tr.mathTypeValues.put(ctx, formRealApplicationType(
                        functionPortion.getText(), expectedAsFxn, args));
            }
        }
        if (anonymousApplicationDepth > 0) {
            anonymousFunctionExpectedRangeTypes.put(functionPortion.getParent(),
                    tr.mathTypes.get(ctx));
        }
    }

    private void emitPreApplicationType(ParserRuleContext ctx,
                                        List<? extends ParseTree> args) {
        MTFunction foundExpType;
        foundExpType = PApply.getConservativePreApplicationType(g, args, tr.mathTypes);
        emit("expression: " + ctx.getText() + "("
                + ctx.getStart().getLine() + ","
                + ctx.getStop().getCharPositionInLine() + ") of type "
                + foundExpType.toString());
    }

    private void typeMathFunctionLikeThing(ParserRuleContext ctx,
                                           Token qualifier, Token name,
                                           List<? extends ParserRuleContext> args) {
        emitPreApplicationType(ctx, args);
        MathSymbol intendedEntry =
                getIntendedFunction(ctx, qualifier, name, args);
        if (intendedEntry == null) {
            tr.mathTypes.put(ctx, g.INVALID);
            return;
        }
        MTFunction expectedType = (MTFunction) intendedEntry.getType();
        List<MTType> formalParameterTypes =
                MathSymbol.getParameterTypes(expectedType);
        if (args.size() != formalParameterTypes.size()) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MATH_FUNCTION,
                    ctx.getStart(), expectedType.toString());
        }
        //We know we match expectedType--otherwise the above would have thrown
        //an exception.
        tr.mathTypes.put(ctx, expectedType.getRange());
        //I had better identify a type
        if (typeValueDepth > 0) {
            MTType realAppType = formRealApplicationType(
                    intendedEntry.getName(), expectedType, args);
            tr.mathTypeValues.put(ctx, realAppType);
        }
    }

    private MTType formRealApplicationType(String functionName,
                                           MTFunction expectedType,
                                           List<? extends ParserRuleContext> args) {
        List<MTType> arguments = new ArrayList<>();
        MTType argTypeValue;
        for (ParserRuleContext arg : args) {
            argTypeValue = tr.mathTypeValues.get(arg);

            if (argTypeValue == null) {
                compiler.errMgr.semanticError(
                        ErrorKind.INVALID_MATH_TYPE, arg.getStart(),
                        arg.getText());
                argTypeValue = g.INVALID;
            }
            arguments.add(argTypeValue);
        }
        return expectedType.getApplicationType(functionName, arguments);
    }

    @Nullable private MathSymbol getIntendedFunction(@NotNull ParserRuleContext ctx,
                                           @Nullable Token qualifier,
                                           @NotNull Token name,
                                           @NotNull List<? extends ParserRuleContext> args) {
        MTType preAppType = PApply.getConservativePreApplicationType(g,
                args, tr.mathTypes);
        tr.mathTypes.put(ctx, preAppType);
        MTFunction eType = (MTFunction) tr.mathTypes.get(ctx);
        MathSymbol intendedFunction = null;

        List<MathSymbol> sameNameFunctions = new ArrayList<>();

        try {
            sameNameFunctions.addAll(
                    symtab.getInnermostActiveScope()
                            .query(new MathFunctionNamedQuery(qualifier, name))
                            .stream().filter(s -> s.getType() instanceof MTFunction)
                            .collect(Collectors.toList()));
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        } catch (UnexpectedSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    name, "a math symbol", name.getText(),
                    e.getTheUnexpectedSymbolsDescription());
        }

        if (sameNameFunctions.isEmpty()) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MATH_FUNCTION,
                    ctx.getStart(), name.getText());
        } else {
            //TODO: For now, we can at least check arg counts, etc for now.
            intendedFunction = sameNameFunctions.get(0);
        }
        if (intendedFunction == null) return null;
        MTFunction intendedEntryType = (MTFunction) intendedFunction.getType();
        emit("matching " + name.getText() + " : " + eType
                + " to " + intendedFunction.getName() + " : " + intendedEntryType);
        return intendedFunction;
    }

    private void insertGlobalAssertion(ParserRuleContext ctx,
                                       GlobalMathAssertionSymbol.ClauseType type,
                                       ResolveParser.MathAssertionExpContext assertion) {
        String name = ctx.getText() + "_" + globalSpecCount++;
        PExp assertionAsPExp = getPExpFor(assertion);
        try {
            symtab.getInnermostActiveScope().define(
                    new GlobalMathAssertionSymbol(name, assertionAsPExp, type,
                            ctx, getRootModuleIdentifier()));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
    }

    @NotNull protected final PExp getPExpFor(ParseTree ctx) {
        if (ctx == null) {
            return g.getTrueExp();
        }
        PExpBuildingListener<PExp> builder = new PExpBuildingListener<>(g, tr);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        PExp result = builder.getBuiltPExp(ctx);
        return result == null ? g.getTrueExp() : result;
    }

    private void chainMathTypes(ParseTree current, ParseTree child) {
        tr.mathTypes.put(current, tr.mathTypes.get(child));
        tr.mathTypeValues.put(current, tr.mathTypeValues.get(child));
    }

    private void chainProgTypes(ParseTree current, ParseTree child) {
        tr.progTypes.put(current, tr.progTypes.get(child));
        tr.progTypeValues.put(current, tr.progTypeValues.get(child));
    }

    private ModuleIdentifier getRootModuleIdentifier() {
        return symtab.getInnermostActiveScope().getModuleIdentifier();
    }

    private void emit(String msg) {
        if (EMIT_DEBUG) {
            compiler.info(msg);
        }
    }
}

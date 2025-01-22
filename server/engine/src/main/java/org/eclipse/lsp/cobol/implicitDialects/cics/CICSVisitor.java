/*
 * Copyright (c) 2022 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Broadcom, Inc. - initial API and implementation
 *
 */

package org.eclipse.lsp.cobol.implicitDialects.cics;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.antlr.v4.runtime.Lexer.HIDDEN;
import static org.eclipse.lsp.cobol.AntlrRangeUtils.constructRange;

import com.google.common.collect.ImmutableList;
import java.util.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp.cobol.AntlrRangeUtils;
import org.eclipse.lsp.cobol.common.dialects.CobolDialect;
import org.eclipse.lsp.cobol.common.dialects.DialectProcessingContext;
import org.eclipse.lsp.cobol.common.error.ErrorSeverity;
import org.eclipse.lsp.cobol.common.error.ErrorSource;
import org.eclipse.lsp.cobol.common.error.SyntaxError;
import org.eclipse.lsp.cobol.common.message.MessageService;
import org.eclipse.lsp.cobol.common.model.Locality;
import org.eclipse.lsp.cobol.common.model.tree.CodeBlockUsageNode;
import org.eclipse.lsp.cobol.common.model.tree.CompilerDirectiveNode;
import org.eclipse.lsp.cobol.common.model.tree.Node;
import org.eclipse.lsp.cobol.common.model.tree.variable.QualifiedReferenceNode;
import org.eclipse.lsp.cobol.common.model.tree.variable.VariableUsageNode;
import org.eclipse.lsp.cobol.common.utils.ThreadInterruptionUtil;
import org.eclipse.lsp.cobol.core.visitor.VisitorHelper;
import org.eclipse.lsp.cobol.implicitDialects.cics.nodes.ExecCicsAbendNode;
import org.eclipse.lsp.cobol.implicitDialects.cics.nodes.ExecCicsHandleNode;
import org.eclipse.lsp.cobol.implicitDialects.cics.nodes.ExecCicsNode;
import org.eclipse.lsp.cobol.implicitDialects.cics.nodes.ExecCicsReturnNode;
import org.eclipse.lsp.cobol.implicitDialects.cics.utility.CICSOptionsCheckUtility;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * This visitor analyzes the parser tree for CICS and returns its semantic context as a syntax tree
 */
@Slf4j
// @AllArgsConstructor
class CICSVisitor extends CICSParserBaseVisitor<List<Node>> {

    private final DialectProcessingContext context;
    private final MessageService messageService;
    private final CICSOptionsCheckUtility cicsOptionsCheckUtility;

    CICSVisitor(DialectProcessingContext context, MessageService messageService) {
        this.context = context;
        this.messageService = messageService;
        this.cicsOptionsCheckUtility = new CICSOptionsCheckUtility(context, errors);
    }

    @Getter
    private final List<SyntaxError> errors = new LinkedList<>();

    @Override
    public List<Node> visitQualifiedDataName(CICSParser.QualifiedDataNameContext ctx) {
        return addTreeNode(ctx, QualifiedReferenceNode::new);
    }

    @Override
    public List<Node> visitCics_handle_abend(CICSParser.Cics_handle_abendContext ctx) {
        boolean isProgram =
            Optional.ofNullable(ctx)
                .map(CICSParser.Cics_handle_abendContext::PROGRAM)
                .filter(s -> s.size() > 0)
                .isPresent();

        boolean isLabel =
            Optional.ofNullable(ctx)
                .map(CICSParser.Cics_handle_abendContext::LABEL)
                .filter(s -> s.size() > 0)
                .isPresent();

        boolean isReset =
            Optional.ofNullable(ctx)
                .map(CICSParser.Cics_handle_abendContext::RESET)
                .filter(s -> s.size() > 0)
                .isPresent();

        ExecCicsHandleNode.HandleAbendType type;
        if (isProgram) {
            type = ExecCicsHandleNode.HandleAbendType.PROGRAM;
        } else if (isLabel) {
            type = ExecCicsHandleNode.HandleAbendType.LABEL;
        } else if (isReset) {
            type = ExecCicsHandleNode.HandleAbendType.RESET;
        } else {
            type = ExecCicsHandleNode.HandleAbendType.CANCEL;
        }

        Node node = new ExecCicsHandleNode(getOriginalLocality(ctx.getParent().getParent().getParent()), type);
        visitChildren(ctx).forEach(node::addChild);
        return ImmutableList.of(node);
    }

    @Override
    public List<Node> visitCics_abend(CICSParser.Cics_abendContext ctx) {
        boolean isCancel = !ctx.cics_abend_cancel().isEmpty();
        boolean isNodump = !ctx.cics_abend_nodump().isEmpty();
        String[] abcodeArray = Optional.ofNullable(ctx.cics_abend_abcode())
            .filter(l -> l.size() > 0)
            .map(l -> l.get(0))
            .map(a -> a.cics_name())
            .map(a -> a.name())
            .map(a -> a.variableNameUsage())
            .orElse(ImmutableList.of())
            .stream()
            .map(a -> a.getText())
            .toArray(String[]::new);

        String abcode = String.join(",", abcodeArray);
        return addTreeNode(ctx, locality -> new ExecCicsAbendNode(locality, abcode, isCancel, isNodump));
    }

    @Override
    public List<Node> visitCics_return(CICSParser.Cics_returnContext ctx) {
        Node node = new ExecCicsReturnNode(getOriginalLocality(ctx.getParent().getParent()));
        visitChildren(ctx).forEach(node::addChild);
        return ImmutableList.of(node);
    }

    @Override
    public List<Node> visitCicsExecBlock(CICSParser.CicsExecBlockContext ctx) {
        areaBWarning(ctx);
        changeContextToDialectStatement(ctx);
        if (ctx.stop.getType() != CICSLexer.END_EXEC) {
            SyntaxError error = SyntaxError.syntaxError()
                    .errorSource(ErrorSource.PARSING)
                    .location(getTokenEndLocality(ctx.stop).toOriginalLocation())
                    .suggestion(messageService.getMessage("cicsParser.missingEndExec"))
                    .severity(ErrorSeverity.ERROR)
                    .build();
            errors.add(error);
        }

        List<Node> nodes = visitChildren(ctx);
        if (nodes.isEmpty()) {
            return addTreeNode(ctx, ExecCicsNode::new);
        }
        return nodes;
    }

    @Override
    public List<Node> visitCicsDfhRespLiteral(CICSParser.CicsDfhRespLiteralContext ctx) {
        addReplacementContext(ctx);
        return visitChildren(ctx);
    }

    @Override
    public List<Node> visitCompilerDirective(CICSParser.CompilerDirectiveContext ctx) {
        cicsOptionsCheckUtility.setExciOptionsEnabled(false);
        cicsOptionsCheckUtility.setSpOptionsEnabled(false);

        for (CICSParser.CompilerOptsContext options : ctx.compilerOpts()) {
            if (options.cicsOptions() != null) {
                if (options.cicsOptions().getText().contains("SP"))
                    cicsOptionsCheckUtility.setSpOptionsEnabled(true);
                if (options.cicsOptions().getText().contains("EXCI"))
                    cicsOptionsCheckUtility.setExciOptionsEnabled(true);
            }
        }

        return visitChildren(ctx);
    }

    @Override
    public List<Node> visitAllExciRules(CICSParser.AllExciRulesContext ctx) {
        // TODO: uncomment and adjust below when we decide to support this feature based on compiler
        // directive
        //    boolean isExciModeEnabled = context
        //            .getConfig()
        //            .getCompilerOptions()
        //            .stream()
        //            .anyMatch(str -> str.equalsIgnoreCase("EXCI"));
        //    if (!isExciModeEnabled) {
        //      Locality tokenLocality = getTokenLocality(ctx.start);
        //      errors.add(SyntaxError.syntaxError()
        //              .errorSource(ErrorSource.PARSING)
        //              .location(tokenLocality.toOriginalLocation())
        //              .suggestion(messageService.getMessage("cics.exci.errormessage"))
        //              .severity(ErrorSeverity.WARNING)
        //              .build());
        //    }
        return visitChildren(ctx);
    }

    @Override
    public List<Node> visitCompilerXOptsOption(CICSParser.CompilerXOptsOptionContext ctx) {
        if (Objects.nonNull(ctx.EXCI())) {
            return addTreeNode(
                    ctx,
                    locality ->
                            new CompilerDirectiveNode(locality, ctx.EXCI().getText(), CICSDialect.DIALECT_NAME));
        }
        return visitChildren(ctx);
    }

    @Override
    public List<Node> visitCicsDfhValueLiteral(CICSParser.CicsDfhValueLiteralContext ctx) {
        addReplacementContext(ctx);
        return visitChildren(ctx);
    }

    @Override
    public List<Node> visitDataName(CICSParser.DataNameContext ctx) {
        return addTreeNode(ctx, locality -> new VariableUsageNode(getName(ctx), locality));
    }

    // TODO : correct the implementation
    @Override
    public List<Node> visitParagraphNameUsage(CICSParser.ParagraphNameUsageContext ctx) {
        String name = getName(ctx);
        Locality locality = VisitorHelper.buildNameRangeLocality(ctx, name, context.getProgramDocumentUri());
        Location location = context.getExtendedDocument().mapLocation(locality.getRange());

        Node node =
                new CodeBlockUsageNode(
                        Locality.builder().range(location.getRange()).uri(location.getUri()).build(), name);
        visitChildren(ctx).forEach(node::addChild);
        return ImmutableList.of(node);
    }

    /**
     * Traverses children of the parse tree.
     *
     * <p>Inspects CICS Rules to make sure mandatory options exist since the Parser Rules only enforce
     * if any combination of possible inputs exist. Also checks for Invalid options given the other
     * provided optionals and checks for duplicates.
     *
     * @param node the node under inspection
     * @return List of children nodes
     */
    @Override
    public List<Node> visitChildren(RuleNode node) {
        if (node.getRuleContext().parent != null)
            cicsOptionsCheckUtility.checkOptions((ParserRuleContext) node.getRuleContext());
        ThreadInterruptionUtil.checkThreadInterrupted();
        return super.visitChildren(node);
    }

    @Override
    protected List<Node> defaultResult() {
        return ImmutableList.of();
    }

    @Override
    protected List<Node> aggregateResult(List<Node> aggregate, List<Node> nextResult) {
        return Stream.concat(aggregate.stream(), nextResult.stream()).collect(toList());
    }

    private List<Node> addTreeNode(ParserRuleContext ctx, Function<Locality, Node> nodeConstructor) {
        Node node = nodeConstructor.apply(getOriginalLocality(ctx));
        visitChildren(ctx).forEach(node::addChild);
        return ImmutableList.of(node);
    }

    private Locality getOriginalLocality(ParserRuleContext ctx) {
        Location location = context.getExtendedDocument().mapLocation(AntlrRangeUtils.constructRange(ctx));
        return Locality.builder().uri(location.getUri()).range(location.getRange()).build();
    }

    private String getName(ParserRuleContext context) {
        return ofNullable(context).map(RuleContext::getText).map(String::toUpperCase).orElse("");
    }

    /**
     * Builds context name locality based on the name and uri of the document
     *
     * @param ctx  is a parse rule context
     * @param name is a name of the entity
     * @param uri  is an uri of the document
     * @return locality object
     */
    private Locality buildNameRangeLocality(ParserRuleContext ctx, String name, String uri) {
        return VisitorHelper.buildNameRangeLocality(ctx, name, uri);
    }

    private void changeContextToDialectStatement(ParserRuleContext ctx) {
        context.getExtendedDocument().fillArea(AntlrRangeUtils.constructRange(ctx), CobolDialect.FILLER.charAt(0));
    }

    private void addReplacementContext(ParserRuleContext ctx) {
        getAllTerminalNodes(ctx)
                .forEach(
                        node ->
                                context
                                        .getExtendedDocument()
                                        .replace(
                                                constructRange(node.getSymbol()),
                                                StringUtils.repeat(CobolDialect.FILLER, node.getText().length())));
    }

    private List<TerminalNode> getAllTerminalNodes(ParserRuleContext ctx) {
        List<TerminalNode> result = new ArrayList<>();
        for (int childNodes = 0; childNodes < ctx.getChildCount(); childNodes++) {
            ParseTree child = ctx.getChild(childNodes);
            if (child instanceof TerminalNode) {
                result.add((TerminalNode) child);
            } else {
                result.addAll(getAllTerminalNodes((ParserRuleContext) child));
            }
        }
        return result;
    }

    private void areaBWarning(ParserRuleContext ctx) {
        List<Token> tokenList =
                getAllTerminalNodes(ctx).stream().map(TerminalNode::getSymbol).collect(toList());
        areaBWarning(tokenList);
    }

    private void areaBWarning(@NonNull List<Token> tokenList) {
        tokenList.forEach(
                token ->
                        Optional.ofNullable(getTokenLocality(token))
                                .filter(startsInAreaA(token))
                                .ifPresent(
                                        locality ->
                                                throwException(
                                                        token.getText(),
                                                        locality,
                                                        messageService.getMessage("CobolVisitor.AreaBWarningMsg"))));
    }

    private Locality getTokenLocality(Token token) {
        return Locality.builder()
                .uri(context.getProgramDocumentUri())
                .range(buildTokenRange(token))
                .build();
    }

    private Locality getTokenEndLocality(Token token) {
        return Locality.builder()
                .uri(context.getProgramDocumentUri())
                .range(buildTokenEndRange(token))
                .build();
    }

    private Predicate<Locality> startsInAreaA(Token token) {
        return it -> {
            int charPosition = it.getRange().getStart().getCharacter();
            return charPosition > 6 && charPosition < 11 && token.getChannel() != HIDDEN;
        };
    }

    private void throwException(String wrongToken, @NonNull Locality locality, String message) {
        SyntaxError error =
                SyntaxError.syntaxError()
                        .errorSource(ErrorSource.PARSING)
                        .location(locality.toOriginalLocation())
                        .suggestion(message + wrongToken)
                        .severity(ErrorSeverity.WARNING)
                        .build();

        LOG.debug("Syntax error by CobolVisitor#throwException: {}", error);
        if (!errors.contains(error)) {
            errors.add(error);
        }
    }

    /**
     * Builds context name locality based on the name and uri of the document
     *
     * @param token is a token
     * @return range object
     */
    private Range buildTokenRange(Token token) {
        return new Range(
                new Position(token.getLine() - 1, token.getCharPositionInLine()),
                new Position(
                        token.getLine() - 1, token.getCharPositionInLine() + token.getText().length()));
    }

    private Range buildTokenEndRange(Token token) {
        Position p = new Position(token.getLine() - 1, token.getCharPositionInLine() + token.getStopIndex() - token.getStartIndex() + 1);
        return new Range(p, p);
    }
}

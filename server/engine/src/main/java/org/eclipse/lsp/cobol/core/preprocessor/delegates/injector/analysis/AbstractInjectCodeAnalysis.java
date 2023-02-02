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

package org.eclipse.lsp.cobol.core.preprocessor.delegates.injector.analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.lsp.cobol.common.ResultWithErrors;
import org.eclipse.lsp.cobol.common.copybook.CopybookConfig;
import org.eclipse.lsp.cobol.common.copybook.CopybookModel;
import org.eclipse.lsp.cobol.common.copybook.CopybookName;
import org.eclipse.lsp.cobol.common.error.ErrorSeverity;
import org.eclipse.lsp.cobol.common.error.ErrorSource;
import org.eclipse.lsp.cobol.common.error.SyntaxError;
import org.eclipse.lsp.cobol.common.message.MessageService;
import org.eclipse.lsp.cobol.common.model.Locality;
import org.eclipse.lsp.cobol.core.model.*;
import org.eclipse.lsp.cobol.core.preprocessor.CopybookHierarchy;
import org.eclipse.lsp.cobol.core.preprocessor.TextPreprocessor;
import org.eclipse.lsp.cobol.core.preprocessor.delegates.copybooks.PreprocessorStack;
import org.eclipse.lsp.cobol.common.utils.ImplicitCodeUtils;
import org.eclipse.lsp.cobol.core.preprocessor.delegates.injector.providers.CopybookContentProvider;
import org.eclipse.lsp.cobol.core.preprocessor.delegates.util.LocalityUtils;
import org.eclipse.lsp.cobol.core.semantics.CopybooksRepository;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.eclipse.lsp.cobol.common.error.ErrorCode.MISSING_COPYBOOK;
import static org.eclipse.lsp.cobol.common.error.ErrorSeverity.ERROR;
import static org.eclipse.lsp.cobol.common.error.ErrorSeverity.INFO;
import static org.eclipse.lsp.cobol.core.preprocessor.ProcessingConstants.*;

/**
 * This class is a framework for the copybook analysis. The actual implementations may change the
 * behavior overriding the methods.
 */
@Slf4j
abstract class AbstractInjectCodeAnalysis implements InjectCodeAnalysis {
  protected static final int MAX_COPYBOOK_NAME_LENGTH_DEFAULT = Integer.MAX_VALUE;
  private static final String HYPHEN = "-";
  private static final String UNDERSCORE = "_";
  private static final String SYNTAX_ERROR_CHECK_COPYBOOK_NAME =
      "Syntax error by checkCopybookName: {}";

  private final TextPreprocessor preprocessor;
  private final MessageService messageService;
  private final int maxCopybookNameLength;

  AbstractInjectCodeAnalysis(
      TextPreprocessor preprocessor,
      MessageService messageService,
      int maxCopybookNameLength) {
    this.preprocessor = preprocessor;
    this.messageService = messageService;
    this.maxCopybookNameLength = maxCopybookNameLength;
  }

  @Override
  public PreprocessorFunctor injectCode(
      CopybookContentProvider copybookContentProvider,
      CopybookName injectedSourceName,
      ParserRuleContext context,
      ParserRuleContext copySource,
      CopybookConfig config,
      String documentUri) {
    return hierarchy -> {
      List<SyntaxError> errors = new ArrayList<>();
      CopybookMetaData metaData =
          validateMetaData(
                  CopybookMetaData.builder()
                      .copybookName(injectedSourceName)
                      .context(context)
                      .documentUri(documentUri)
                      .copybookId(randomUUID().toString())
                      .config(config)
                      .nameLocality(
                          LocalityUtils.buildLocality(
                              copySource, documentUri, hierarchy.getCurrentCopybookId()))
                      .contextLocality(
                          LocalityUtils.buildLocality(
                              context, documentUri, hierarchy.getCurrentCopybookId()))
                      .build())
              .unwrap(errors::addAll);

      OldExtendedDocument copybookDocument =
          buildExtendedDocumentForCopybook(copybookContentProvider, metaData).apply(hierarchy).unwrap(errors::addAll);
      return stack -> {
        writeText(metaData, copybookDocument).accept(stack);
        return subContext -> {
          storeCopyStatementSemantics(metaData, copybookDocument).accept(subContext);
          return nestedMappings -> {
            collectNestedSemanticData(metaData, copybookDocument).accept(nestedMappings);
            return allErrors -> allErrors.addAll(errors);
          };
        };
      };
    };
  }

  private ResultWithErrors<CopybookMetaData> validateMetaData(CopybookMetaData metaData) {
    List<SyntaxError> errors = new ArrayList<>();
    final String copybookName = metaData.getCopybookName().getDisplayName();
    final Locality locality = metaData.getNameLocality();
    if (copybookName.length() > maxCopybookNameLength && !ImplicitCodeUtils.isImplicit(locality.getUri())) {
      errors.add(
          addCopybookError(
              copybookName,
              maxCopybookNameLength,
              locality,
              INFO,
              "GrammarPreprocessorListener.copyBkOverMaxChars",
              SYNTAX_ERROR_CHECK_COPYBOOK_NAME));
    }
    // The first or last character must not be a hyphen.
    if (copybookName.startsWith(HYPHEN) || copybookName.endsWith(HYPHEN)) {
      errors.add(
          addCopybookError(
              metaData.getCopybookName(),
              locality,
              ERROR,
              "GrammarPreprocessorListener.copyBkStartsOrEndsWithHyphen",
              SYNTAX_ERROR_CHECK_COPYBOOK_NAME));
    }

    // copybook Name can't contain _
    if (copybookName.contains(UNDERSCORE))
      errors.add(
          addCopybookError(
              metaData.getCopybookName(),
              locality,
              ERROR,
              "GrammarPreprocessorListener.copyBkContainsUnderScore",
              SYNTAX_ERROR_CHECK_COPYBOOK_NAME));

    return new ResultWithErrors<>(metaData, errors);
  }

  private Consumer<PreprocessorStack> writeText(
      CopybookMetaData metaData, OldExtendedDocument copybookDocument) {
    return beforeWriting()
        .andThen(writeCopybook(metaData.getCopybookId(), copybookDocument.getText()))
        .andThen(afterWriting(metaData.getContext()));
  }

  protected Consumer<CopybooksRepository> storeCopyStatementSemantics(
      CopybookMetaData metaData, OldExtendedDocument copybookDocument) {
    return addCopybookUsage(metaData)
        .andThen(addCopybookDefinition(metaData, copybookDocument.getUri()))
        .andThen(collectCopybookStatement(metaData))
        .andThen(addNestedCopybook(copybookDocument));
  }

  private Function<CopybookHierarchy, ResultWithErrors<OldExtendedDocument>>
      buildExtendedDocumentForCopybook(CopybookContentProvider copybookContentProvider,
                                       CopybookMetaData metaData) {
    List<SyntaxError> errors = new ArrayList<>();
    return hierarchy -> {
      CopybookModel model = getCopyBookContent(copybookContentProvider, metaData, hierarchy).unwrap(errors::addAll);
      String text = preprocessor.cleanUpCode(model.getUri(), model.getContent())
              .unwrap(errors::addAll).calculateExtendedText();
      String replaced = handleReplacing(metaData, hierarchy, text)
              .unwrap(errors::addAll);
      ResultWithErrors<OldExtendedDocument> oldExtendedDocumentResultWithErrors = processCopybook(metaData, hierarchy, model.getUri(), replaced).accumulateErrors(errors);
      return oldExtendedDocumentResultWithErrors;
    };
  }

  private Consumer<Map<String, DocumentMapping>> collectNestedSemanticData(
      CopybookMetaData metaData, OldExtendedDocument copybookDocument) {
    return nestedMapping -> {
      nestedMapping.putAll(copybookDocument.getDocumentMapping());
      nestedMapping.putIfAbsent(
          metaData.getCopybookId(),
          Optional.ofNullable(nestedMapping.get(copybookDocument.getUri()))
              .orElseGet(() -> new DocumentMapping(ImmutableList.of(), ImmutableMap.of())));
    };
  }

  protected ResultWithErrors<OldExtendedDocument> processCopybook(
      CopybookMetaData metaData, CopybookHierarchy hierarchy, String uri, String content) {
    hierarchy.push(metaData.toCopybookUsage());
    final ResultWithErrors<OldExtendedDocument> result =
            preprocessor.processCleanCode(uri, content, metaData.getConfig(), hierarchy);
    hierarchy.pop();
    return result;
  }

  protected ResultWithErrors<CopybookModel> getCopyBookContent(
      CopybookContentProvider copybookContentProvider,
      CopybookMetaData copybookMetaData, CopybookHierarchy hierarchy) {
    if (copybookMetaData.getCopybookName().getDisplayName().isEmpty())
      return emptyModel(copybookMetaData.getCopybookName(), ImmutableList.of());

    if (hierarchy.hasRecursion(copybookMetaData.getCopybookName()))
      return emptyModel(
          copybookMetaData.getCopybookName(), hierarchy.mapCopybooks(this::reportRecursiveCopybook));

    String programDocumentUri = hierarchy.getRootDocumentUri().orElse(copybookMetaData.getDocumentUri());

    CopybookModel copybookModel = copybookContentProvider
        .read(copybookMetaData.getConfig(), copybookMetaData.getCopybookName(), programDocumentUri, copybookMetaData.getDocumentUri())
        .orElse(null);

    if (copybookModel != null) {
      return new ResultWithErrors<>(copybookModel, ImmutableList.of());
    }

    return emptyModel(
            copybookMetaData.getCopybookName(), ImmutableList.of(reportMissingCopybooks(copybookMetaData)));
  }

  protected Consumer<PreprocessorStack> beforeWriting() {
    return PreprocessorStack::pop;
  }

  protected Consumer<PreprocessorStack> afterWriting(ParserRuleContext context) {
    return it -> it.accumulateTokenShift(context);
  }

  private Consumer<CopybooksRepository> collectCopybookStatement(CopybookMetaData metaData) {
    return it -> it.addStatement(metaData.getCopybookId(), metaData.getCopybookName().getDialectType(), metaData.getContextLocality());
  }

  protected Consumer<CopybooksRepository> addCopybookUsage(CopybookMetaData metaData) {
    return copybooks ->
        copybooks.addUsage(
            metaData.getCopybookName().getQualifiedName(),
                metaData.getCopybookName().getDialectType(),
                metaData.getNameLocality().toLocation());
  }

  protected Consumer<CopybooksRepository> addCopybookDefinition(CopybookMetaData metaData, String uri) {
    return copybooks -> {
      if (!(metaData.getCopybookName() == null
          || isEmpty(metaData.getCopybookName().getQualifiedName())
          || isEmpty(uri)
          || ImplicitCodeUtils.isImplicit(uri)))
        copybooks.define(
            metaData.getCopybookName().getQualifiedName(),
                metaData.getCopybookName().getDialectType(), new Location(uri, new Range(new Position(), new Position())));
    };
  }

  protected Consumer<CopybooksRepository> addNestedCopybook(OldExtendedDocument copybookDocument) {
    return copybooks -> copybooks.merge(copybookDocument.getCopybooks());
  }

  @SuppressWarnings("unused")
  protected ResultWithErrors<String> handleReplacing(
      CopybookMetaData metaData, CopybookHierarchy hierarchy, String text) {
    return new ResultWithErrors<>(text, ImmutableList.of());
  }

  private Consumer<PreprocessorStack> writeCopybook(String copybookId, String copybookContent) {
    return it ->
        it.write(CPY_ENTER_TAG + copybookId + CPY_URI_CLOSE + copybookContent + CPY_EXIT_TAG);
  }

  private SyntaxError reportMissingCopybooks(CopybookMetaData metaData) {
    SyntaxError error =
        SyntaxError.syntaxError()
            .errorSource(ErrorSource.COPYBOOK)
            .location(metaData.getNameLocality().toOriginalLocation())
            .suggestion(
                messageService.getMessage(
                    "GrammarPreprocessorListener.errorSuggestion",
                    metaData.getCopybookName().getQualifiedName()))
            .severity(ERROR)
            .errorCode(MISSING_COPYBOOK)
            .build();
    LOG.debug("Syntax error by reportMissingCopybooks: {}", error.toString());
    return error;
  }

  protected ResultWithErrors<CopybookModel> emptyModel(
      CopybookName copybookName, List<SyntaxError> errors) {
    return new ResultWithErrors<>(new CopybookModel(copybookName.toCopybookId(""), copybookName,
            "", ""), errors);
  }

  protected SyntaxError addCopybookError(
      CopybookName copybookName,
      Locality locality,
      ErrorSeverity info,
      String messageID,
      String logMessage) {
    SyntaxError error =
        SyntaxError.syntaxError().errorSource(ErrorSource.COPYBOOK)
            .severity(info)
            .suggestion(messageService.getMessage(messageID, copybookName.getDisplayName()))
            .location(locality.toOriginalLocation())
            .build();
    LOG.debug(logMessage, error.toString());
    return error;
  }

  protected SyntaxError addCopybookError(
      String copybookName,
      int maxNameLength,
      Locality locality,
      ErrorSeverity info,
      String messageID,
      String logMessage) {
    SyntaxError error =
        SyntaxError.syntaxError().errorSource(ErrorSource.COPYBOOK)
            .severity(info)
            .suggestion(messageService.getMessage(messageID, maxNameLength, copybookName))
            .location(locality.toOriginalLocation())
            .build();
    LOG.debug(logMessage, error.toString());
    return error;
  }

  private SyntaxError reportRecursiveCopybook(CopybookUsage usage) {
    return addCopybookError(
        usage.getName(),
        usage.getLocality(),
        ERROR,
        "GrammarPreprocessorListener.recursionDetected",
        "Syntax error by reportRecursiveCopybook: {}");
  }
}

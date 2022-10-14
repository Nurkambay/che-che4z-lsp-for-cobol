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
package org.eclipse.lsp.cobol.core.model.tree.logic;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp.cobol.core.messages.MessageTemplate;
import org.eclipse.lsp.cobol.core.model.ErrorSeverity;
import org.eclipse.lsp.cobol.core.model.ErrorSource;
import org.eclipse.lsp.cobol.core.model.Locality;
import org.eclipse.lsp.cobol.core.model.SyntaxError;

import java.util.List;

/**
 * Processor utility class
 */
@Slf4j
@UtilityClass
public class ProcessorUtils {

  /**
   * Adds a parsing error to the collection
   * @param errors collection to add
   * @param locality is a locality of the error
   * @param dataName is a name of the statement
   * @param errorType is an error message template
   */
  public void addParsingError(List<SyntaxError> errors, Locality locality, String dataName, String errorType) {
    SyntaxError error = createParsingError(locality, dataName, errorType);
    errors.add(error);
    LOG.debug("Syntax error: " + error.toString());
  }

  /**
   * Creates a parsing error
   * @param locality of the error
   * @param dataName is a name of the statement
   * @param errorType is an error message template
   * @return the syntax error
   */
  public SyntaxError createParsingError(Locality locality, String dataName, String errorType) {
    return SyntaxError.syntaxError()
        .errorSource(ErrorSource.PARSING)
        .severity(ErrorSeverity.ERROR)
        .locality(locality)
        .messageTemplate(MessageTemplate.of(errorType, dataName))
        .build();
  }
}

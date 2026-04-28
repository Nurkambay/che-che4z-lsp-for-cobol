/*
 * Copyright (c) 2026 Broadcom.
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
package org.eclipse.lsp.cobol.core.model.extendedapi.ast;

import org.eclipse.lsp.cobol.common.model.tree.ProgramNode;

/** Exports analysis result program node to ASTProgram transport data object */
public interface ASTExporter {
  /**
   * Exports program node to ASTProgram transport data object
   *
   * @param programNode is the program node
   * @return the ASTProgram data object
   */
  ASTProgram export(ProgramNode programNode);
}

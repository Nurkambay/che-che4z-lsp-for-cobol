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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.eclipse.lsp.cobol.core.model.extendedapi.Location;

/** Data transport object of AST for Compiler Directive node. */
@Value
@EqualsAndHashCode(callSuper = true)
public class ASTCompilerDirectiveNode extends ASTNode {
  String directiveText;
  String dialect;

  public ASTCompilerDirectiveNode(Location location, String directiveText, String dialect) {
    super("COMPILER_DIRECTIVE", location);
    this.directiveText = directiveText;
    this.dialect = dialect;
  }
}

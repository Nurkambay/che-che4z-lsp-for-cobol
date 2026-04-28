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
import org.eclipse.lsp.cobol.common.model.ProcedureName;
import org.eclipse.lsp.cobol.core.model.extendedapi.Location;

/** Data transport object of AST for Alter node. */
@Value
@EqualsAndHashCode(callSuper = true)
public class ASTAlter extends ASTNode {
  ProcedureName from;
  ProcedureName to;

  public ASTAlter(Location location, ProcedureName from, ProcedureName to) {
    super("ALTER", location);
    this.from = from;
    this.to = to;
  }
}

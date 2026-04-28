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

import java.util.List;
import lombok.Data;
import org.eclipse.lsp.cobol.core.model.extendedapi.Location;

/** AST Node transport data object */
@Data
public class ASTNode {
  final String type;
  Location location;
  List<ASTNode> children;

  public ASTNode(String type, Location location) {
    this.type = type;
    this.location = location;
  }
}

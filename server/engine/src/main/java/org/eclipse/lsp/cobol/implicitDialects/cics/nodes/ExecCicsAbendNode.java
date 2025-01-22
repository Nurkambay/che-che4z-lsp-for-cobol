/*
 * Copyright (c) 2025 Broadcom.
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

package org.eclipse.lsp.cobol.implicitDialects.cics.nodes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.lsp.cobol.common.model.Locality;
import org.eclipse.lsp.cobol.common.model.NodeType;
import org.eclipse.lsp.cobol.common.model.tree.Node;
import org.eclipse.lsp.cobol.implicitDialects.cics.CICSDialect;

/** EXEC CICS ABEND node */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExecCicsAbendNode extends Node {

  @Getter
  private final boolean abcode;

  @Getter
  private final boolean cancel;

  @Getter
  private final boolean nodump;

  public ExecCicsAbendNode(Locality location, boolean abcode, boolean cancel, boolean nodump) {
    super(location, NodeType.STATEMENT, CICSDialect.DIALECT_NAME);
    this.abcode = abcode;
    this.cancel = cancel;
    this.nodump = nodump;
  }
}

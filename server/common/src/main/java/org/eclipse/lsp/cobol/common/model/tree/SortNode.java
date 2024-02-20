/*
 * Copyright (c) 2024 Broadcom.
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
package org.eclipse.lsp.cobol.common.model.tree;

import lombok.Getter;
import lombok.ToString;
import org.eclipse.lsp.cobol.common.model.Locality;
import org.eclipse.lsp.cobol.common.model.NodeType;

/**
 * The class represents SORT statement in COBOL.
 * <a href="https://www.ibm.com/docs/en/cobol-zos/6.4?topic=statements-sort-statement">...</a>
 */
@Getter
@ToString(callSuper = true)
public class SortNode extends Node {
  boolean ascending;
  String key;
  public SortNode(Locality location, boolean ascending, String key) {
    super(location, NodeType.SORT);
    this.ascending = ascending;
    this.key = key;
  }
}

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
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.eclipse.lsp.cobol.common.model.tree.variable.VariableType;
import org.eclipse.lsp.cobol.core.model.extendedapi.Location;

/** Data transport object of AST for Variable Definition node. */
@Value
@EqualsAndHashCode(callSuper = true)
public class ASTVariableDefinition extends ASTNode {
  String name;
  List<Location> definitions;
  List<Location> usages;
  int level;
  boolean redefines;
  boolean specifiedGlobal;
  String variableType;

  public ASTVariableDefinition(
      Location location,
      String name,
      int level,
      boolean redefines,
      boolean specifiedGlobal,
      VariableType variableType,
      List<Location> definition,
      List<Location> usages) {
    super("VARIABLE_DEFINITION", location);
    this.name = name;
    this.level = level;
    this.redefines = redefines;
    this.specifiedGlobal = specifiedGlobal;
    this.variableType = variableType.name();
    this.definitions = definition;
    this.usages = usages;
  }
}

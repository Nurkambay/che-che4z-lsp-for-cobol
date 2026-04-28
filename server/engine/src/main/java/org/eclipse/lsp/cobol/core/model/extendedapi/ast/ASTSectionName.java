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
import org.eclipse.lsp.cobol.core.model.extendedapi.Location;

/** Data transport object of AST for Section Name node. */
@Value
@EqualsAndHashCode(callSuper = true)
public class ASTSectionName extends ASTNode {
  String name;
  List<Location> definitions;
  List<Location> usages;

  public ASTSectionName(
      Location location, String name, List<Location> definition, List<Location> usages) {
    super("SECTION_NAME", location);
    this.name = name;
    this.definitions = definition;
    this.usages = usages;
  }
}

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

/** Data transport object of AST for Copy node. */
@Value
@EqualsAndHashCode(callSuper = true)
public class ASTCopy extends ASTNode {
  String uri;
  String name;
  Location nameLocation;
  List<Location> definitions;
  List<Location> usages;

  public ASTCopy(
      Location location,
      String uri,
      String name,
      Location nameLocation,
      List<Location> definitions,
      List<Location> usages) {
    super("COPY", location);
    this.uri = uri;
    this.name = name;
    this.nameLocation = nameLocation;
    this.definitions = definitions;
    this.usages = usages;
  }
}

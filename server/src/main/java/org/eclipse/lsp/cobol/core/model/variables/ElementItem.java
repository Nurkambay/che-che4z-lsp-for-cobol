/*
 * Copyright (c) 2020 Broadcom.
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

package org.eclipse.lsp.cobol.core.model.variables;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp.cobol.core.model.Locality;

import static org.eclipse.lsp.cobol.core.model.variables.StructureType.ELEMENT_ITEM;

/**
 * This value class represents an element item COBOL variable. It has a PIC clause representing its
 * type, and an optional VALUE clause that stores an explicitly defined value; both as Strings.
 * Element items cannot have nested variables, but may be a top element with level 01. Allowed
 * levels for this element are 01-49.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ElementItem extends AbstractVariable {

  String picClause;
  String value;
  UsageFormat usageFormat;

  public ElementItem(
      int levelNumber,
      String name,
      Locality definition,
      boolean global,
      Variable parent,
      String picClause,
      String value,
      UsageFormat usageFormat) {
    super(levelNumber, name, definition, global, parent);
    this.picClause = picClause;
    this.value = value;
    this.usageFormat = usageFormat;
  }

  @Override
  public Variable rename(RenameItem newParent) {
    return new ElementItem(
        levelNumber, name, definition, newParent.global, newParent, picClause, value, usageFormat);
  }

  @Override
  public StructureType getStructureType() {
    return ELEMENT_ITEM;
  }

  @Override
  public String getFormattedDisplayLine() {
    StringBuilder stringBuilder = new StringBuilder(getFormattedSuffix());
    if (picClause != null) stringBuilder.append(" PIC ").append(picClause);
    if (usageFormat != UsageFormat.UNDEFINED) stringBuilder.append(" USAGE ").append(usageFormat);
    if (StringUtils.isNoneBlank(value)) stringBuilder.append(" VALUE ").append(value);
    return stringBuilder.append(".").toString();
  }
}

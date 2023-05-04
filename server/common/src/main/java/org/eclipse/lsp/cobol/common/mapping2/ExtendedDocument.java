/*
 * Copyright (c) 2023 Broadcom.
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
package org.eclipse.lsp.cobol.common.mapping2;

import lombok.Getter;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

/**
 * Extended document class
 */
public class ExtendedDocument {
  @Getter
  private final String originalText;
  private final ExtendedText baseText;
  private final ExtendedText currentText;
  @Getter
  private boolean dirty;

  public ExtendedDocument(String text, String uri) {
    originalText = text;
    baseText = new ExtendedText("", uri);
    currentText = new ExtendedText(text, uri);
    dirty = true;
    commitTransformations();
  }

  public String getUri() {
    return baseText.getUri();
  }

  /**
   * Commit changes
   */
  public void commitTransformations() {
    if (isDirty()) {
      baseText.clear();
      for (ExtendedTextLine line : currentText.getLines()) {
        baseText.add(line.shadowCopy());
      }
      dirty = false;
    }
  }

  public Location mapLocation(Range range) {
    return baseText.mapLocation(range);
  }

  public void insertCopybook(Range copyStatementRange, ExtendedText copybook) {
    currentText.insert(copyStatementRange, copybook);
    dirty = true;
  }

  public void insertCopybook(int line, ExtendedText copybook) {
    currentText.insert(line, copybook);
    dirty = true;
  }

  public void replace(Range range, String newText) {
    currentText.replace(range, newText);
    dirty = true;
  }

  public void clear(Range range) {
    currentText.clear(range);
    dirty = true;
  }

  @Override
  public String toString() {
    return baseText.toString();
  }
}

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
package org.eclipse.lsp.cobol.common.mapping;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test for rangeIn method of the {@link MappingHelper} class **/
class MappingHelperRangeText {
  @Test
  void testFirstLine_in() {
    Range testRange = new Range(new Position(0, 0), new Position(0, 80));
    Range bigRange = new Range(new Position(0, 0), new Position(20, 50));
    assertTrue(MappingHelper.rangeIn(testRange, bigRange));
  }

  @Test
  void testMiddleLine_in() {
    Range testRange = new Range(new Position(1, 60), new Position(1, 80));
    Range bigRange = new Range(new Position(0, 0), new Position(20, 50));
    assertTrue(MappingHelper.rangeIn(testRange, bigRange));
  }

  @Test
  void testFirstLinePoint_out() {
    Range testRange = new Range(new Position(0, 80), new Position(0, 80));
    Range bigRange = new Range(new Position(0, 0), new Position(20, 50));
    assertTrue(MappingHelper.rangeIn(testRange, bigRange));
  }

  @Test
  void testMiddleLinePoint_out() {
    Range testRange = new Range(new Position(1, 60), new Position(1, 60));
    Range bigRange = new Range(new Position(0, 0), new Position(20, 50));
    assertTrue(MappingHelper.rangeIn(testRange, bigRange));
  }

  @Test
  void testBottomLinePoint_out() {
    Range testRange = new Range(new Position(12, 83), new Position(12, 83));
    Range bigRange = new Range(new Position(8, 0), new Position(12, 80));
    assertTrue(MappingHelper.rangeIn(testRange, bigRange));
  }

}

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

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for ExtendedText class
 */
class ExtendedTextTest {

  private static final String TEXT = "     0 LINE\r\n"
      + "     1 LINE\r\n"
      + "     2 LINE\r\n"
      + "     3 LINE\r\n";

  @Test
  void testToString() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    assertEquals(TEXT.substring(0, TEXT.length() - 2), extendedText.toString());

    extendedText = new ExtendedText("TEXT\r\nTEXT", "uri");
    assertEquals("TEXT\r\nTEXT", extendedText.toString());
  }

  @Test
  void testInsertToMiddle() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    extendedText.insert(2, new ExtendedText("NEW LINE", "copybook"));

    Range range = new Range(new Position(4, 0), new Position(4, 5));
    Location location = extendedText.mapLocation(range);
    assertEquals(new Range(new Position(3, 0), new Position(3, 5)).toString(), location.getRange().toString());

    range = new Range(new Position(2, 0), new Position(2, 5));
    location = extendedText.mapLocation(range);
    assertEquals(new Range(new Position(0, 0), new Position(0, 5)).toString(), location.getRange().toString());
    assertEquals("copybook", location.getUri());
  }

  @Test
  void testInsertToBottom() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    extendedText.insert(4, new ExtendedText("NEW LINE", "copybook"));

    Range range = new Range(new Position(4, 0), new Position(4, 5));
    Location location = extendedText.mapLocation(range);
    assertEquals(new Range(new Position(0, 0), new Position(0, 5)).toString(), location.getRange().toString());
    assertEquals("copybook", location.getUri());
  }

  @Test
  void testInsertToTop() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    extendedText.insert(0, new ExtendedText("NEW LINE", "copybook"));

    Range range = new Range(new Position(0, 0), new Position(0, 5));
    Location location = extendedText.mapLocation(range);
    assertEquals(new Range(new Position(0, 0), new Position(0, 5)).toString(), location.getRange().toString());
    assertEquals("copybook", location.getUri());

    range = new Range(new Position(1, 0), new Position(1, 5));
    location = extendedText.mapLocation(range);
    assertEquals(new Range(new Position(0, 0), new Position(0, 5)).toString(), location.getRange().toString());
    assertEquals("uri", location.getUri());
  }

  @Test
  void insertExtendedText() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    extendedText.insert(new Range(new Position(1, 6), new Position(2, 4)), new ExtendedText("COPYBOOK 1 LINE\r\nCOPYBOOK 2 LINE\r\nCOPYBOOK 3 LINE", "copybook"));

    Range range = new Range(new Position(1, 0), new Position(1, 3));
    Location location = extendedText.mapLocation(range);

    assertEquals("     0 LINE\r\n"
        + "COPYBOOK 1 LINE\r\n"
        + "COPYBOOK 2 LINE\r\n"
        + "COPYBOOK 3 LINE\r\n"
        + "     3 LINE", extendedText.toString());

    assertEquals(new Range(new Position(0, 0), new Position(0, 3)).toString(), location.getRange().toString());
    assertEquals("copybook", location.getUri());

    range = new Range(new Position(4, 0), new Position(4, 3));
    location = extendedText.mapLocation(range);

    assertEquals(new Range(new Position(3, 0), new Position(3, 3)).toString(), location.getRange().toString());
    assertEquals("uri", location.getUri());
  }

  @Test
  void testDeleteLines() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    extendedText.insert(4, new ExtendedText("NEW LINE", "copybook"));

    extendedText.deleteLines(new Range(new Position(1, 5), new Position(3, 8)));
    assertEquals("     0 LINE\r\nNEW LINE", extendedText.toString());
  }

  @Test
  void testAddLineBreak() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    extendedText.addLineBreak(new Position(1, 6));
    assertEquals("     0 LINE\r\n"
        + "     1\r\n"
        + " LINE\r\n"
        + "     2 LINE\r\n"
        + "     3 LINE", extendedText.toString());
  }

  @Test
  void testDeleteInsideTheLine() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");
    Range range = new Range(new Position(1, 4), new Position(1, 9));
    Location location = extendedText.mapLocation(range);

    assertEquals(range.toString(), location.getRange().toString());
    assertEquals("uri", location.getUri());

    Range deleteRange = new Range(new Position(1, 1), new Position(1, 3));
    extendedText.delete(deleteRange);

    Range newRange = new Range(new Position(1, 1), new Position(1, 6));
    location = extendedText.mapLocation(newRange);

    assertEquals("     0 LINE\r\n"
        + "  1 LINE\r\n"
        + "     2 LINE\r\n"
        + "     3 LINE", extendedText.toString());
    assertEquals(range.toString(), location.getRange().toString());
    assertEquals("uri", location.getUri());
  }

  @Test
  void testDeleteTwoLines() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");

    Range deleteRange = new Range(new Position(1, 6), new Position(2, 3));
    extendedText.delete(deleteRange);

    Range newRange = new Range(new Position(2, 1), new Position(2, 6));
    Location location = extendedText.mapLocation(newRange);

    assertEquals("     0 LINE\r\n"
        + "     1\r\n"
        + " 2 LINE\r\n"
        + "     3 LINE", extendedText.toString());

    assertEquals(new Range(new Position(2, 5), new Position(2, 10)).toString(), location.getRange().toString());
  }

  @Test
  void testDeleteRange() {
    ExtendedText extendedText = new ExtendedText(TEXT, "uri");

    Range deleteRange = new Range(new Position(1, 6), new Position(3, 3));
    extendedText.delete(deleteRange);

    Range newRange = new Range(new Position(2, 1), new Position(2, 6));
    Location location = extendedText.mapLocation(newRange);

    assertEquals("     0 LINE\r\n"
        + "     1\r\n"
        + " 3 LINE", extendedText.toString());

    assertEquals(new Range(new Position(3, 5), new Position(3, 10)).toString(), location.getRange().toString());
  }

  @Test
  void testClearInsideOneLine() {

  }

  @Test
  void testClearTwoLinesRange() {

  }

  @Test
  void testClearBigRange() {

  }

  @Test
  void testReplaceInsideOneLine() {

  }

  @Test
  void testReplaceTwoLines() {

  }

  @Test
  void testReplaceBigRange() {

  }

  @Test
  void testReplaceInsideOneLine_multipleLinesText() {

  }

  @Test
  void testReplaceTwoLines_multipleLinesText() {

  }

  @Test
  void testReplaceBigRange_multipleLinesText() {

  }

}

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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extended Text contains information about original text and current text with the mapping
 */
public class ExtendedText {
  private final List<ExtendedTextLine> lines = new ArrayList<>();
  @Getter
  private final String uri;

  public ExtendedText(String text, String uri) {
    String[] textLines = MappingHelper.split(text);
    for (int i = 0; i < textLines.length; i++) {
      String line = textLines[i];
      lines.add(new ExtendedTextLine(line, i, uri));
    }
    this.uri = uri;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    int index = 0;
    for (ExtendedTextLine line : lines) {
      String lineText = line.toString();
      if (!lineText.isEmpty()) {
        builder.append(lineText);
      }
      if (++index != lines.size()) {
        builder.append("\r\n");
      }
    }
    return builder.toString();
  }

  public Location mapLocation(Range range) {
    MappedCharacter startCharacter = getCharacterAt(range.getStart());
    MappedCharacter endCharacter = getCharacterAt(range.getEnd());
    if (!startCharacter.getUri().equals(endCharacter.getUri())) {
      throw new RuntimeException("Original location has 2 different urls");
    }

    if (startCharacter.getOriginalPosition() == null || endCharacter.getOriginalPosition() == null) {
      if (startCharacter.getInstantLocation() == null && endCharacter.getInstantLocation() == null) {
        throw new RuntimeException("Cannot find original position");
      }
      return Optional.ofNullable(startCharacter.getInstantLocation()).orElse(endCharacter.getInstantLocation());
    }
    Position startPosition = new Position(startCharacter.getOriginalPosition().getLine(), startCharacter.getOriginalPosition().getCharacter());
    Position endPosition = new Position(endCharacter.getOriginalPosition().getLine(), endCharacter.getOriginalPosition().getCharacter());

    Range originalRange = new Range(startPosition, endPosition);
    return new Location(startCharacter.getUri(), originalRange);
  }

  public void insert(int line, ExtendedText text) {
    for (ExtendedTextLine textLine : text.lines) {
      lines.add(line++, textLine);
    }
  }

  public void insert(Position position, ExtendedTextLine newLine) {
    lines.get(position.getLine()).insert(position.getCharacter(), newLine);
  }

  public void insert(int line, ExtendedTextLine newLine) {
    lines.add(line, newLine);
  }

  public void insert(Range range, ExtendedText copybook) {
    deleteLines(range);
    insert(range.getStart().getLine(), copybook);
  }

  public void delete(Range range) {
    if (range.getStart().getLine() == range.getEnd().getLine()) {
      lines.get(range.getStart().getLine()).delete(range.getStart().getCharacter(), range.getEnd().getCharacter());
    } else {
      lines.get(range.getStart().getLine()).trim(range.getStart().getCharacter());
      lines.get(range.getEnd().getLine()).delete(0, range.getEnd().getCharacter());
      if (range.getStart().getLine() + 1 <= range.getEnd().getLine() - 1) {
        lines.subList(range.getStart().getLine() + 1, range.getEnd().getLine()).clear();
      }
    }
  }

  public void deleteLines(Range range) {
    if (range.getEnd().getLine() >= range.getStart().getLine()) {
      lines.subList(range.getStart().getLine(), range.getEnd().getLine() + 1).clear();
    }
  }

  public void delete(int lineNumber) {
    lines.remove(lineNumber);
  }

  public void append(int lineNumber, ExtendedTextLine line) {
    lines.get(lineNumber).append(line);
  }

  public void clear(Range range) {
    if (range.getStart().getLine() == range.getEnd().getLine()) {
      lines.get(range.getStart().getLine()).clear(range.getStart().getCharacter(), range.getEnd().getCharacter());
    } else {
      ExtendedTextLine line = lines.get(range.getStart().getLine());
      line.clear(range.getStart().getCharacter(), line.size() - 1);

      if (range.getStart().getLine() + 1 <= range.getEnd().getLine() - 1) {
        lines.subList(range.getStart().getLine() + 1, range.getEnd().getLine() - 1).forEach(ExtendedTextLine::clear);
      }
      lines.get(range.getEnd().getLine()).clear(0, range.getEnd().getCharacter());
    }
  }

  public void addLineBreak(Position position) {
    ExtendedTextLine line = lines.get(position.getLine());
    ExtendedTextLine newLine = line.subline(position.getCharacter(), line.size() - 1);
    line.trim(position.getCharacter());

    lines.add(position.getLine() + 1, newLine);
  }

  void clear() {
    lines.clear();
  }

  List<ExtendedTextLine> getLines() {
    return lines;
  }

  void add(ExtendedTextLine newLine) {
    lines.add(newLine);
  }

  public void replace(Range range, String newText) {
    String[] newLines = MappingHelper.split(newText);

    delete(range);
    Location instantLocation = new Location(uri, range);

    // Single line
    if (range.getEnd().getLine() == range.getStart().getLine()) {
      addLines(range.getStart(), newLines, instantLocation);
    } else {
      // Multiple lines
      addLines(range.getStart().getLine(), newLines, instantLocation);
    }
  }

  private void addLines(Position position, String[] newLines, Location instantLocation) {
    if (newLines.length == 1) {
      insert(position, new ExtendedTextLine(newLines[0], instantLocation, uri));
    } if (newLines.length > 1) {
      addLineBreak(position);
      append(position.getLine(), new ExtendedTextLine(newLines[0], instantLocation, uri));
      insert(new Position(position.getLine() + 1, 0), new ExtendedTextLine(newLines[newLines.length - 1], instantLocation, uri));
      for (int i = 1; i < newLines.length - 2; i++) {
        insert(position.getLine() + i, new ExtendedTextLine(newLines[i], instantLocation, uri));
      }
    }
  }

  private void addLines(int firstLine, String[] newLines, Location instantLocation) {
    if (newLines.length == 1) {
      append(firstLine, new ExtendedTextLine(newLines[0], instantLocation, uri));
    } else if (newLines.length > 1) {
      append(firstLine, new ExtendedTextLine(newLines[0], instantLocation, uri));
      insert(new Position(firstLine + 1, 0), new ExtendedTextLine(newLines[newLines.length - 1], instantLocation, uri));
      for (int i = 1; i < newLines.length - 2; i++) {
        insert(firstLine + i, new ExtendedTextLine(newLines[i], instantLocation, uri));
      }
    } else {
      append(firstLine, getLines().get(firstLine + 1));
      delete(firstLine + 1);
    }
  }

  private MappedCharacter getCharacterAt(Position position) {
    int lineNumber = position.getLine();
    int character = position.getCharacter();
    ExtendedTextLine line = lines.get(lineNumber);
    return line.getCharacterAt(character);
  }
}

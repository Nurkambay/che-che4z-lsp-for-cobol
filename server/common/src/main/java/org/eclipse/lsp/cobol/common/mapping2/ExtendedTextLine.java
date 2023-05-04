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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extended Text Line with symbols
 */
class ExtendedTextLine {
  @Getter
  private final List<MappedCharacter> characters = new ArrayList<>();

  private ExtendedTextLine() {
  }

  ExtendedTextLine(String line, Location instantLocation, String uri) {
    for (int i = 0; i < line.length(); i++) {
      char character = line.charAt(i);
      checkCharacter(character);
      characters.add(new MappedCharacter(character, null, uri, this, instantLocation));
    }
  }

  ExtendedTextLine(String line, int lineNumber, String uri) {
    this(line, new Position(lineNumber, 0), uri);
  }

  ExtendedTextLine(String line, Position start, String uri) {
    for (int i = 0; i < line.length(); i++) {
      char character = line.charAt(i);
      checkCharacter(character);
      Position position = new Position(start.getLine(), start.getCharacter() + i);
      characters.add(new MappedCharacter(character, position, uri, this, null));
    }
  }

  /**
   * Returns size of the line
   * @return the size
   */
  int size() {
    return characters.size();
  }

  /**
   * Returns character for given position
   * @param position - position of the character in the line
   * @return a character at the given position
   */
  MappedCharacter getCharacterAt(int position) {
    return characters.get(position);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (MappedCharacter character : characters) {
      builder.append(character.getCharacter());
    }
    return builder.toString();
  }

  /**
   * Removes characters from line
   * @param start - start position
   * @param end - end position
   */
  void delete(int start, int end) {
    characters.subList(start, end + 1).clear();
  }

  /**
   * Trim characters
   * @param pos - position to start trimming
   */
  void trim(int pos) {
    characters.subList(pos, characters.size()).clear();
  }

  /**
   * Insert new text line to the given character position
   * @param pos - character position for insert, text will be inserted before this position
   * @param line - Extended Text Line
   */
  void insert(int pos, ExtendedTextLine line) {
    characters.addAll(pos, line.characters);
  }

  /**
   * Creates a new line objects with characters in the given range
   * @param start - start position of the range
   * @param end - end position of the range
   * @return a new line object
   */
  ExtendedTextLine subline(int start, int end) {
    List<MappedCharacter> newCharacters = characters.subList(start, end + 1).stream()
        .map(MappedCharacter::shadowCopy).collect(Collectors.toList());

    ExtendedTextLine result = new ExtendedTextLine();
    result.characters.addAll(newCharacters);
    return result;
  }

  /**
   * Appends the line with given line
   * @param line - line that will be added to the end of this line
   */
  void append(ExtendedTextLine line) {
    for (int i = 0; i < line.size(); i++) {
      characters.add(line.getCharacterAt(i));
    }
  }

  /**
   * Clears the line from start to end position
   * @param start - start position
   * @param end - end position
   */
  void clear(int start, int end) {
    characters.subList(start, end + 1).forEach(c -> c.setCharacter(' '));
  }

  /**
   * Clears whole line
   */
  void clear() {
    clear(0, size() - 1);
  }

  /**
   * Creates a shadow copy of the line object
   * @return a line object
   */
  ExtendedTextLine shadowCopy() {
    ExtendedTextLine result = new ExtendedTextLine();
    result.characters.clear();
    for (MappedCharacter character : characters) {
      result.characters.add(character.shadowCopy());
    }
    return result;
  }

  private void checkCharacter(Character character) {
    if (character.equals('\n')) {
      throw new RuntimeException("Text line contains a new line characters");
    }
  }

}

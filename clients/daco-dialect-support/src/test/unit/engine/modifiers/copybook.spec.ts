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
 *   Broadcom - initial API and implementation
 */

import { CopybookContentVisitor } from "../../../../engine/modifiers/copybook";

describe("Copybook Modifier Test", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should skip when entryName is missing", () => {
    const visitor = new CopybookContentVisitor();

    const levelNumber = {
      getText: () => "01",
      start: { line: 1, column: 0, start: 0 },
      stop: { line: 1, column: 1, start: 0, stop: 1 },
    };
    const ctx = {
      entryName: () => null,
      levelNumber: () => levelNumber,
      getChildCount: () => 0,
      getChild: () => null,
    } as any;

    const result = visitor.visitDataDescriptionEntryFormat1(ctx);
    expect(result).toEqual([]);
  });

  it("should add variable redefine descriptor", () => {
    const visitor = new CopybookContentVisitor();

    const dataName = {
      getText: () => "ENTRY TEXT",
      start: { line: 1, column: 0, start: 0 },
      stop: { line: 1, column: 1, start: 0, stop: 1 },
    };

    const ctx = {
      dataName: () => dataName,
      getChildCount: () => 0,
      getChild: () => null,
    } as any;

    const result = visitor.visitDataRedefinesClause(ctx);
    expect(result).toHaveLength(1);
    expect(result[0].type).toEqual("REDEFINITION");
  });
});

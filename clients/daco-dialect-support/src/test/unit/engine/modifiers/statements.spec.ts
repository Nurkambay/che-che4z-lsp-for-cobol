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

import { DaCoStatementsVisitor } from "../../../../engine/modifiers/statements";

describe("Statements Modifier Test", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should construct range for the context", () => {
    const visitor = new DaCoStatementsVisitor();

    const ctx = {
      start: { line: 1, column: 0, start: 0 },
      stop: { line: 1, column: 1, start: 0, stop: 1 },
      getChildCount: () => 0,
      getChild: () => null,
      dfldRcu: () => null,
      ifRowCondition: () => null,
      tableDMLStatement: () => null,
      execStatement: () => null,
    } as any;

    const result = visitor.visitDacoStatements(ctx);
    expect(result).toHaveLength(1);
  });

  it("should create diagnostic for the SORT TABLE context", () => {
    const visitor = new DaCoStatementsVisitor();

    const ctx = {
      start: { line: 1, column: 0, start: 0 },
      stop: { line: 1, column: 1, start: 0, stop: 1 },
      getChildCount: () => 0,
      getChild: () => null,
      dfldRcu: () => null,
      ifRowCondition: () => null,
      tableDMLStatement: () => {
        return {
          sortTableStatement: () => ({}),
        };
      },
      execStatement: () => null,
    } as any;

    const result = visitor.visitDacoStatements(ctx);
    expect(result).toHaveLength(1);
    expect(result[0].diagnostics).toHaveLength(1);
  });

  it("should substitute with value the IF ROW clause context", () => {
    const visitor = new DaCoStatementsVisitor();

    const ctx = {
      start: { line: 1, column: 0, start: 0 },
      stop: { line: 1, column: 1, start: 0, stop: 1 },
      getChildCount: () => 0,
      getChild: () => null,
      dfldRcu: () => null,
      ifRowCondition: jest.fn().mockReturnValue({}),
      tableDMLStatement: () => null,
      execStatement: () => null,
    } as any;

    const result = visitor.visitDacoStatements(ctx);
    expect(result).toHaveLength(1);
    expect(result[0].filler).toEqual("ZERO");
  });

  it("should substitute with space the EXEC statement context", () => {
    const visitor = new DaCoStatementsVisitor();

    const ctx = {
      start: { line: 1, column: 0, start: 0 },
      stop: { line: 1, column: 1, start: 0, stop: 1 },
      getChildCount: () => 0,
      getChild: () => null,
      dfldRcu: () => null,
      ifRowCondition: () => null,
      tableDMLStatement: () => null,
      execStatement: jest.fn().mockReturnValue({}),
    } as any;

    const result = visitor.visitDacoStatements(ctx);
    expect(result).toHaveLength(1);
    expect(result[0].filler).toEqual(" ");
  });
});

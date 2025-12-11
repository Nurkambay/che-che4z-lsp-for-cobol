/*
 * Copyright (c) 2025 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */

import * as vscode from "vscode";
import { IDocumentProcessingContext } from "@code4z/cobol-dialect-api";

export interface TextChanger {
  apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void;
}

export function getChangers(): TextChanger[] {
  return [
    new Level01Changer(),
    new Level05Changer(),
    new SDataChanger(),
    new XxxChanger(),
    new VoidChanger(),
  ];
}

abstract class AbstractChanger implements TextChanger {
  abstract apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void;

  protected replace(
    context: IDocumentProcessingContext,
    line: number,
    character: number,
    text: string,
  ) {
    const range = new vscode.Range(
      new vscode.Position(line, character),
      new vscode.Position(line, character + text.length),
    );

    context.replace(range, text);
  }

  protected replaceEx(
    context: IDocumentProcessingContext,
    line: number,
    start: number,
    end: number,
    text: string,
  ) {
    const range = new vscode.Range(
      new vscode.Position(line, start),
      new vscode.Position(line, end),
    );

    context.replace(range, text);
  }
}

class Level01Changer extends AbstractChanger {
  apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void {
    const index = lines[line].indexOf(" AA ");
    if (index > 0) {
      this.replace(context, line, index, " 01 ");
    }
  }
}

class Level05Changer extends AbstractChanger {
  apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void {
    const index = lines[line].indexOf(" BB ");
    if (index > 0) {
      this.replace(context, line, index, " 05 ");
    }
  }
}

class SDataChanger extends AbstractChanger {
  apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void {
    const index = lines[line].indexOf(" SDATA");
    if (index > 0) {
      this.replaceEx(
        context,
        line,
        index,
        index + " SDATA".length,
        " PIC X(9)",
      );
    }
  }
}

class XxxChanger extends AbstractChanger {
  apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void {
    if (param) {
      const index = lines[line].indexOf("XXX");
      if (index > 0) {
        this.replace(context, line, index, `${param}`);
      }
    }
  }
}

class VoidChanger extends AbstractChanger {
  private readonly REGEX =
    /^\s*VOID\s+([A-Z0-9_-]+)\s+THRU\s+([A-Z0-9_-]+)\.\s*$/i;

  apply(
    context: IDocumentProcessingContext,
    line: number,
    lines: string[],
    param?: string,
  ): void {
    const result = this.parseVoidThru(lines[line]);

    if (result) {
      const start = lines[line].indexOf("VOID");
      const end = lines[line].length - 1;
      const range = new vscode.Range(
        new vscode.Position(line, start),
        new vscode.Position(line, end),
      );
      context.replace(
        range,
        `           {!VOID} {$${result[0]}} THRU {#${result[1]}.`,
        `          MOVE 0 TO {$${result[0]}}.\r\n          GO TO {#${result[1]}}`,
      );
    }
  }

  private parseVoidThru(line: string): [string, string] | null {
    const match = this.REGEX.exec(line);
    if (!match) return null;
    const [, variable, procedure] = match;
    return [variable, procedure];
  }
}

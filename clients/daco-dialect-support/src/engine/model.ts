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
import * as vscode from "vscode";

import {
  ATNSimulator,
  BaseErrorListener,
  RecognitionException,
  Recognizer,
  Token,
} from "antlr4ng";

export interface ParseError {
  line: number;
  column: number;
  message: string;
  range: vscode.Range;
}

export type RedefinitionVariableDescriptor = {
  type: "REDEFINITION";
  nameRange: vscode.Range;
  name: string;
};

export type RegularVariableDescriptor = {
  type: "DEFINITION";
  levelRange: vscode.Range;
  level: number;
  nameRange: vscode.Range;
  name: string;
  options: string;
};

export type CopyFromVariableDescriptor = {
  type: "COPY-FROM";
  levelRange: vscode.Range;
  level: number;
  nameRange: vscode.Range;
  name: string;
  copyFromRange: vscode.Range;
  suffix: string;
};

export type VariableDescriptor =
  | RedefinitionVariableDescriptor
  | RegularVariableDescriptor
  | CopyFromVariableDescriptor;

export class CopybookDescriptor {
  constructor(
    public readonly statementRange: vscode.Range,
    public readonly nameRange: vscode.Range,
    public readonly level: number,
    public readonly name: string,
    public readonly suffix?: string,
    public readonly suffixRange?: vscode.Range,
    public readonly parentName?: string,
    public readonly parentNameRange?: vscode.Range,
  ) {}
}

export class CopybookDescriptorPD extends CopybookDescriptor {
  constructor(statementRange: vscode.Range) {
    super(statementRange, statementRange, 0, "");
  }
}

export class CollectingErrorListener extends BaseErrorListener {
  public readonly errors: ParseError[] = [];

  syntaxError<S extends Token, T extends ATNSimulator>(
    _recognizer: Recognizer<T>,
    offendingSymbol: S | null,
    line: number,
    charPositionInLine: number,
    msg: string,
    _e: RecognitionException | null,
  ): void {
    this.errors.push({
      line,
      column: charPositionInLine,
      message: msg,
      range: this.getRangeForSyntaxError(
        offendingSymbol,
        line,
        charPositionInLine,
      ),
    });
  }

  private getRangeForSyntaxError(
    offendingSymbol: Token | null,
    line: number,
    charPositionInLine: number,
  ) {
    const tokenLength = offendingSymbol
      ? offendingSymbol.stop - offendingSymbol.start + 1
      : 0;
    return new vscode.Range(
      line - 1,
      charPositionInLine,
      line - 1,
      charPositionInLine + tokenLength,
    );
  }
}

export class VariableAccumulator {
  private readonly descriptors: (VariableDescriptor | CopybookDescriptor)[] =
    [];
  private readonly copybookDescriptors: Map<
    CopybookDescriptor,
    VariableDescriptor[]
  > = new Map();

  public generateDescriptors(): VariableDescriptor[] {
    const result: VariableDescriptor[] = [];

    for (const descriptor of this.descriptors) {
      if (descriptor instanceof CopybookDescriptor) {
        result.push(...(this.copybookDescriptors.get(descriptor) ?? []));
      } else {
        result.push(descriptor);
      }
    }

    return result;
  }

  public add(descriptor: VariableDescriptor) {
    this.descriptors.push(descriptor);
  }

  public addCopybookPlaceholder(descriptor: CopybookDescriptor) {
    this.descriptors.push(descriptor);
  }

  public insertCopybookVariables(
    descriptor: CopybookDescriptor,
    variables: VariableDescriptor[],
  ) {
    this.copybookDescriptors.set(descriptor, variables);
  }
}

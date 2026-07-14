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
import * as antlr from "antlr4ng";
import { IDocumentProcessingContext } from "@code4z/cobol-dialect-api";
import { MessageService } from "./services/MessageService";
import { processCopyFrom } from "./modifiers/copyfrom";
import { CopybookModifier } from "./modifiers/copybook";
import { generatePredefinedSections } from "./modifiers/predefined";
import { StatementsModifier } from "./modifiers/statements";
import { addParsingErrors } from "./util";
import {
  concatResults,
  constructRange,
  constructRangeFromTokens,
  createOptionsStr,
} from "./modifiers/modifier.utils";
import {
  CopyMaidContext,
  ProcedureDivisionContext,
  ProcedureSectionContext,
  ProgramParser,
  SkipCopyMaidContext,
  VariableEntryContext,
} from "../generated/ProgramParser";
import { ProgramLexer } from "../generated/ProgramLexer";
import {
  CollectingErrorListener,
  CopybookDescriptor,
  CopybookDescriptorPD,
  ParseError,
  VariableAccumulator,
} from "./model";
import { ProgramParserVisitor } from "../generated/ProgramParserVisitor";

type CopyFromOptions =
  | { kind: "COPY_FROM"; suffix: string }
  | { kind: "REGULAR_OPTIONS"; options: string };

export class DaCoPreprocessor {
  private readonly copybookModifier: CopybookModifier;
  private readonly statementsModifier: StatementsModifier;

  constructor(
    private readonly outputChannel: vscode.OutputChannel,
    private readonly messageService: MessageService,
  ) {
    this.copybookModifier = new CopybookModifier(
      this.messageService,
      outputChannel,
    );
    this.statementsModifier = new StatementsModifier(
      this.messageService,
      outputChannel,
    );
  }

  public async execute(
    context: IDocumentProcessingContext,
    _programUri: vscode.Uri,
    text: string,
  ) {
    text = this.cleanup(context, text);

    const { programInfo, errors } = this.analyzeProgram(text);
    addParsingErrors(context, errors);

    const variables = await this.copybookModifier.execute(
      context,
      programInfo.copybooks,
      programInfo.variableAccumulator,
    );

    processCopyFrom(context, variables, this.messageService);
    generatePredefinedSections(
      context,
      programInfo.sections,
      programInfo.procedureDivisionNameEnd,
    );

    this.statementsModifier.execute(context, text);
  }

  private cleanup(context: IDocumentProcessingContext, text: string): string {
    const dcdbPattern = /^[\s\d]{7}D-[BC]/gm;

    let lastPosition = { position: new vscode.Position(0, 0), absPosition: 0 };
    text = text.replace(dcdbPattern, (match, offset) => {
      const startPosition = this.findPosition(
        text,
        lastPosition.absPosition,
        lastPosition.position.line,
        lastPosition.position.character,
        offset,
      );
      const endPosition = this.findPosition(
        text,
        startPosition.absPosition,
        startPosition.position.line,
        startPosition.position.character,
        offset + match.length - 1,
      );
      lastPosition = endPosition;

      const replacement = " ".repeat(match.length);
      context.replace(
        new vscode.Range(startPosition.position, endPosition.position),
        replacement,
      );

      return replacement;
    });
    return text;
  }

  private findPosition(
    text: string,
    absPosition: number,
    startLine: number,
    startColumn: number,
    finishAbsPosition: number,
  ): { position: vscode.Position; absPosition: number } {
    let line = startLine;
    let col = startColumn;

    while (absPosition < finishAbsPosition) {
      if (text.charAt(absPosition) === "\n") {
        ++line;
        col = 1;
      } else {
        ++col;
      }
      absPosition++;
    }
    return {
      position: new vscode.Position(line, col),
      absPosition,
    };
  }

  private analyzeProgram(text: string): {
    programInfo: ProgramInfo;
    errors: ParseError[];
  } {
    const charStream = antlr.CharStream.fromString(text);

    const lexer = new ProgramLexer(charStream);
    const tokenStream = new antlr.CommonTokenStream(lexer);
    const parser = new ProgramParser(tokenStream);
    parser.setMessageService(this.messageService);

    lexer.removeErrorListeners();
    parser.removeErrorListeners();

    const lexerErrors = new CollectingErrorListener();
    const parserErrors = new CollectingErrorListener();

    lexer.addErrorListener(lexerErrors);
    parser.addErrorListener(parserErrors);

    const tree = parser.startRule();

    const visitor = new ProgramVisitor(this.messageService);
    const copybooks = visitor.visit(tree) || [];
    const programInfo = visitor.programInfo;
    programInfo.copybooks = copybooks;

    this.outputChannel.appendLine(
      `Parsing completed with ${lexerErrors.errors.length} lexer errors and ${parserErrors.errors.length} parser errors`,
    );

    console.log(`Found ${copybooks.length} copybook descriptors:`);
    return {
      programInfo,
      errors: [...lexerErrors.errors, ...parserErrors.errors],
    };
  }
}

export class NameResolver {
  private readonly nameStack: {
    level: number;
    name: string;
    range: vscode.Range;
  }[] = [];
  private lastLevel: number = 0;

  public getParentName(
    level: number,
  ): { name: string; range: vscode.Range } | undefined {
    for (let i = this.nameStack.length - 1; i >= 0; i--) {
      if (this.nameStack[i].level < level) {
        return { name: this.nameStack[i].name, range: this.nameStack[i].range };
      }
    }
    return undefined;
  }

  public pushName(level: number, name: string, range: vscode.Range) {
    if (this.lastLevel < level) {
      this.nameStack.push({ level, name, range });
      this.lastLevel = level;
    } else {
      // Pop all names with level greater than or equal to the current level
      while (
        this.nameStack.length > 0 &&
        (this.nameStack.at(-1)?.level ?? 0) >= level
      ) {
        this.nameStack.pop();
      }
      this.nameStack.push({ level, name, range });
      this.lastLevel = level;
    }
  }
}

class ProgramInfo {
  public readonly sections: string[] = [];
  public procedureDivisionNameStart?: number;
  public procedureDivisionNameEnd?: number;
  public variableAccumulator: VariableAccumulator = new VariableAccumulator();
  public copybooks: CopybookDescriptor[] = [];
}

export class ProgramVisitor extends ProgramParserVisitor<CopybookDescriptor[]> {
  public readonly programInfo: ProgramInfo = new ProgramInfo();
  private readonly parentNameResolver: NameResolver = new NameResolver();

  public constructor(private readonly messageService: MessageService) {
    super();
  }

  private parseCopyFromOptions(options: string): CopyFromOptions {
    const trimmed = options.trim();

    const match = /^COPY-FROM\s+([A-Z0-9]+)$/i.exec(trimmed);

    if (!match) {
      return {
        kind: "REGULAR_OPTIONS",
        options,
      };
    }

    const suffix = match[1].toUpperCase();

    if (!/^[A-Z0-9]{2}$/.test(suffix)) {
      const message = this.messageService.get(
        "validation.copy_from_suffix",
        suffix,
      );
      throw new Error(message);
    }

    return {
      kind: "COPY_FROM",
      suffix,
    };
  }

  visitCopyMaid = (ctx: CopyMaidContext): CopybookDescriptor[] => {
    const layoutId = ctx.layoutId();
    if (!layoutId) {
      return super.visitChildren(ctx) ?? [];
    }

    const layoutUsage = ctx.layoutUsage();
    const name = layoutId.getText();
    const suffix = layoutUsage?.getText();

    console.log("Copybook level: " + ctx.LEVEL_NUMBER()?.getText());

    const level = Number.parseInt(ctx.LEVEL_NUMBER()?.getText() ?? "0", 10);
    const statementRange = constructRange(ctx);
    const nameRange = constructRange(layoutId);

    const parentInfo = this.parentNameResolver.getParentName(level);
    const descriptor = new CopybookDescriptor(
      statementRange,
      nameRange,
      level,
      name,
      suffix,
      layoutUsage ? constructRange(layoutUsage) : undefined,
      parentInfo?.name,
      parentInfo?.range,
    );
    this.programInfo.variableAccumulator.addCopybookPlaceholder(descriptor);

    return [descriptor, ...(super.visitChildren(ctx) ?? [])];
  };

  visitVariableEntry = (ctx: VariableEntryContext): CopybookDescriptor[] => {
    const newName = ctx.DACO_COPYBOOK_IDENTIFIER()?.getText()?.toUpperCase();
    const level = Number.parseInt(ctx.LEVEL_NUMBER()?.getText() ?? "0", 10);

    if (newName) {
      const nameRange = constructRangeFromTokens(
        ctx.DACO_COPYBOOK_IDENTIFIER().getSymbol(),
        ctx.DACO_COPYBOOK_IDENTIFIER().getSymbol(),
      );

      this.parentNameResolver.pushName(level, newName, nameRange);

      const optionsText = createOptionsStr(ctx.variableOptionEntry());
      const parsed = this.parseCopyFromOptions(optionsText);

      if (parsed.kind === "COPY_FROM") {
        const copyFromRange = constructRange(ctx.variableOptionEntry());
        this.programInfo.variableAccumulator.add({
          levelRange: constructRangeFromTokens(
            ctx.LEVEL_NUMBER().getSymbol(),
            ctx.LEVEL_NUMBER().getSymbol(),
          ),
          level: level,
          copyFromRange: copyFromRange,
          nameRange: nameRange,
          name: newName,
          suffix: parsed.suffix,
          type: "COPY-FROM",
        });
      } else {
        this.programInfo.variableAccumulator.add({
          levelRange: constructRangeFromTokens(
            ctx.LEVEL_NUMBER().getSymbol(),
            ctx.LEVEL_NUMBER().getSymbol(),
          ),
          level: level,
          nameRange: nameRange,
          name: newName,
          type: "DEFINITION",
          options: optionsText,
        });
      }
    }
    return super.visitChildren(ctx) ?? [];
  };

  visitSkipCopyMaid = (ctx: SkipCopyMaidContext): CopybookDescriptor[] => {
    return [new CopybookDescriptorPD(constructRange(ctx))];
  };

  visitProcedureSection = (
    ctx: ProcedureSectionContext,
  ): CopybookDescriptor[] => {
    this.programInfo.sections.push(ctx.sectionName().getText().toUpperCase());
    return super.visitChildren(ctx) ?? [];
  };

  visitProcedureDivision = (
    ctx: ProcedureDivisionContext,
  ): CopybookDescriptor[] => {
    this.programInfo.procedureDivisionNameStart = ctx.PROCEDURE().symbol.line;
    this.programInfo.procedureDivisionNameEnd = ctx.DOT_FS().symbol.line;
    return super.visitChildren(ctx) ?? [];
  };

  protected aggregateResult = concatResults;
}

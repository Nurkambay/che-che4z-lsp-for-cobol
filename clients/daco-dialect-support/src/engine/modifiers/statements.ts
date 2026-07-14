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
import {
  IDocumentProcessingContext,
  Item,
  Token,
} from "@code4z/cobol-dialect-api";
import {
  concatResults,
  constructRange,
  constructRangeFromTokens,
} from "./modifier.utils";
import {
  DacoSectionsContext,
  DacoStatementsContext,
  DaCoStatementsParser,
  QualifiedDataNameContext,
  VariableUsageNameContext,
} from "../../generated/DaCoStatementsParser";
import { DaCoStatementsLexer } from "../../generated/DaCoStatementsLexer";
import { MessageService } from "../services/MessageService";
import { DaCoStatementsParserVisitor } from "../../generated/DaCoStatementsParserVisitor";
import { addParsingErrors } from "../util";
import { CollectingErrorListener } from "../model";

const BLANK_STATEMENT = "CONTINUE";
const BLANK_VALUE = "ZERO";
const SPACE_VALUE = " ";

export type DiagnosticMessage = {
  severity: vscode.DiagnosticSeverity;
  template: string;
};

class StatementDescriptor {
  constructor(
    public readonly range: vscode.Range,
    public readonly statementRange: vscode.Range,
    public readonly type: "STATEMENT" | "VARIABLE" | "VARIABLE_USAGE",
    public readonly children: StatementDescriptor[],
    public readonly diagnostics: DiagnosticMessage[] = [],
    public readonly filler: string = BLANK_STATEMENT,
  ) {}
}
export class StatementsModifier {
  constructor(
    private readonly messageService: MessageService,
    private readonly outputChannel: vscode.OutputChannel,
  ) {}

  public execute(context: IDocumentProcessingContext, text: string) {
    const statementDescriptors = this.collectStatementsDescriptors(
      context,
      text,
    );
    this.processStatements(statementDescriptors, context);
  }

  private collectStatementsDescriptors(
    context: IDocumentProcessingContext,
    text: string,
  ): StatementDescriptor[] {
    const charStream = antlr.CharStream.fromString(text);
    const lexer = new DaCoStatementsLexer(charStream);
    const tokenStream = new antlr.CommonTokenStream(lexer);
    const parser = new DaCoStatementsParser(tokenStream);
    parser.setMessageService(this.messageService);

    lexer.removeErrorListeners();
    parser.removeErrorListeners();

    const lexerErrors = new CollectingErrorListener();
    const parserErrors = new CollectingErrorListener();

    lexer.addErrorListener(lexerErrors);
    parser.addErrorListener(parserErrors);

    try {
      const tree = parser.startRule();

      addParsingErrors(context, [
        ...lexerErrors.errors,
        ...parserErrors.errors,
      ]);

      return new DaCoStatementsVisitor().visit(tree) || [];
    } catch (e) {
      this.outputChannel.appendLine(
        "Error during parsing: " + JSON.stringify(e),
      );
      return [];
    }
  }

  private processStatements(
    descriptors: StatementDescriptor[],
    context: IDocumentProcessingContext,
  ) {
    descriptors.forEach((descriptor) =>
      console.log(
        `Statement Descriptor: type=${descriptor.type}, range=${JSON.stringify(
          descriptor.statementRange,
        )}`,
      ),
    );
    descriptors.forEach((descriptor) => {
      this.outputChannel.appendLine(
        `Statement Descriptor: ${JSON.stringify(descriptor)}`,
      );
      if (descriptor.children.length > 0) {
        context.replaceWithMap(
          descriptor.range,
          descriptor.statementRange,
          this.traverseChildren(descriptor.children),
          descriptor.filler,
        );
      } else {
        context.replace(descriptor.statementRange, descriptor.filler);
      }
      descriptor.diagnostics.forEach((d) => {
        context.addDiagnostic(
          new vscode.Diagnostic(
            descriptor.statementRange,
            this.messageService.get(d.template),
            d.severity,
          ),
        );
      });
    });
  }

  private traverseChildren(children: StatementDescriptor[]): Item[] {
    const items: Item[] = [];
    let index = 0;
    children.forEach((child) => {
      if (child.type === "VARIABLE") {
        const tokens: Token[] = [];
        const name = `VAR_${index++}`;

        if (child.children.length > 0) {
          this.createTokens(tokens, name, child.children);
        }
        const item: Item = {
          type: "VARIABLE",
          tokens,
        };
        items.push(item);
      }
    });
    return items;
  }

  private createTokens(
    tokens: Token[],
    name: string,
    children: StatementDescriptor[],
  ): Token[] {
    let index = 0;
    children.forEach((child) => {
      if (child.type === "VARIABLE_USAGE") {
        const tokenName = `${name}_USG_${index++}`;
        const token: Token = { name: tokenName, range: child.statementRange };
        tokens.push(token);
        this.createTokens(tokens, tokenName, child.children);
      }
    });
    return tokens;
  }
}

export class DaCoStatementsVisitor extends DaCoStatementsParserVisitor<
  StatementDescriptor[]
> {
  visitDacoSections? = (ctx: DacoSectionsContext): StatementDescriptor[] => {
    const statements: StatementDescriptor[] = [];
    statements.push(
      new StatementDescriptor(
        constructRange(ctx),
        constructRange(ctx),
        "STATEMENT",
        this.visitChildren(ctx) ?? [],
        [],
        SPACE_VALUE,
      ),
    );
    return statements;
  };

  visitDacoStatements?: (ctx: DacoStatementsContext) => StatementDescriptor[] =
    (ctx: DacoStatementsContext): StatementDescriptor[] => {
      const statements: StatementDescriptor[] = [];

      const dfldRcu = ctx.dfldRcu();
      const onSymbol = dfldRcu?.ON()?.symbol;
      const rcuSymbol = dfldRcu?.RCU()?.symbol;
      const isSortTable = ctx.tableDMLStatement()?.sortTableStatement();

      if (onSymbol && rcuSymbol) {
        const range = constructRangeFromTokens(onSymbol, rcuSymbol);
        statements.push(
          new StatementDescriptor(
            range,
            range,
            "STATEMENT",
            [],
            [],
            SPACE_VALUE,
          ),
        );
      } else {
        const diagnostics: DiagnosticMessage[] = [];
        if (isSortTable) {
          diagnostics.push({
            severity: vscode.DiagnosticSeverity.Warning,
            template: "parsers.deprecated",
          });
        }

        statements.push(
          new StatementDescriptor(
            constructRange(ctx),
            constructRange(ctx),
            "STATEMENT",
            this.visitChildren(ctx) ?? [],
            diagnostics,
            this.getFiller(ctx),
          ),
        );
      }
      return statements;
    };

  private getFiller(ctx: DacoStatementsContext): string {
    if (ctx.ifRowCondition()) {
      return BLANK_VALUE;
    }
    if (ctx.execStatement()) {
      return SPACE_VALUE;
    }
    return BLANK_STATEMENT;
  }

  visitQualifiedDataName?: (
    ctx: QualifiedDataNameContext,
  ) => StatementDescriptor[] = (
    ctx: QualifiedDataNameContext,
  ): StatementDescriptor[] => {
    return [
      new StatementDescriptor(
        constructRange(ctx),
        constructRange(ctx),
        "VARIABLE",
        this.visitChildren(ctx) ?? [],
      ),
    ];
  };

  visitVariableUsageName?: (
    ctx: VariableUsageNameContext,
  ) => StatementDescriptor[] = (
    ctx: VariableUsageNameContext,
  ): StatementDescriptor[] => {
    return [
      new StatementDescriptor(
        constructRange(ctx),
        constructRange(ctx),
        "VARIABLE_USAGE",
        this.visitChildren(ctx) ?? [],
      ),
    ];
  };

  protected aggregateResult = concatResults;
}

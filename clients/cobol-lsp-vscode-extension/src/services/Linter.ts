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
 *   Broadcom, Inc. - initial API and implementation
 */
import * as vscode from "vscode";
import * as fs from "node:fs";
import * as vm from "node:vm";
import * as path from "node:path";
import { SETTINGS_UNREACHABLE_CODE_SEVERITY } from "../constants";
import { outputChannel } from "./util/OutputChannel";

export type ASTNodeType = "PROGRAM" | "PARAGRAPH";

export type ApiAstResult = {
  astList: ASTProgram[];
  url: string;
};

export type Position = {
  line: number;
  character: number;
};

export type ProcedureName = {
  name: string;
  inSection?: string;
};

export type Range = {
  start: Position;
  end: Position;
};

export type Location = { uri: string } & Range;

export type ASTNode = {
  children?: ASTNode[];
  type: ASTNodeType;
  location: Location;
};

export type ASTProgram = ASTNode & {
  name: string;
  type: "PROGRAM";
};

export type RuleDiagnostic = {
  message: string;
  range: Range;
  severity: vscode.DiagnosticSeverity;
  source?: string;
};

type LinterRule = {
  run: (ast: unknown, context: RuleContext) => RuleDiagnostic[];
};

export class Rule {
  constructor(
    public name: string,
    public rule: Partial<LinterRule>,
  ) {}
  run(ast: ASTProgram, context: RuleContext): vscode.Diagnostic[] {
    if (!this.rule.run) return [];
    const diagnostics = this.rule.run(ast, context) || [];

    diagnostics.forEach((d) => {
      d.source = name;
    });
    return diagnostics.map((d) => {
      return new vscode.Diagnostic(
        new vscode.Range(
          new vscode.Position(d.range.start.line, d.range.start.character),
          new vscode.Position(d.range.end.line, d.range.end.character),
        ),
        d.message,
        d.severity,
      );
    });
  }
}

export interface RuleContext {
  visit(node: ASTNode, visitor: (node: ASTNode) => void): void;
}

export class Linter implements vscode.Disposable {
  private toDispose: vscode.Disposable[] = [];
  private readonly diagnosticService: DiagnosticService;

  public constructor(
    private readonly mainChannel?: vscode.OutputChannel,
    private readonly logChannel?: vscode.LogOutputChannel,
  ) {
    this.diagnosticService = new DiagnosticService();
    this.toDispose.push(
      vscode.workspace.onDidChangeConfiguration((e) => {
        if (!e.affectsConfiguration(SETTINGS_UNREACHABLE_CODE_SEVERITY)) return;
        this.diagnosticService.clearDiagnostics();
      }),
    );
  }

  public dispose() {
    this.toDispose.forEach((x) => void x.dispose());
    this.toDispose = [];
  }

  public makeAstNotificationHandler() {
    return (result: ApiAstResult) => {
      this.execute(result);
    };
  }

  execute(result: ApiAstResult) {
    this.logChannel?.debug("Handle AST from backend");
    if (result.url) {
      const context: RuleContext = {
        visit(node: ASTNode, visitor: (node: ASTNode) => void) {
          visitor(node);
          node.children?.forEach((child) => this.visit(child, visitor));
        },
      };

      const rules = this.loadRules(this.loadRuleFiles(this.getRulesFolder()));
      const diagnostics: vscode.Diagnostic[] = [];
      result.astList.forEach((ast) => {
        diagnostics.push(...this.runRules(rules, ast, context));
      });

      this.diagnosticService.showDiagnostics(
        vscode.Uri.parse(result.url),
        diagnostics,
      );
    }
  }

  private getRulesFolder(): string | undefined {
    const folders = vscode.workspace.workspaceFolders;
    if (!folders || folders.length === 0) return;

    const root = folders[0].uri.fsPath;
    return path.join(root, ".rules");
  }

  private loadRuleFiles(rulesDir: string | undefined): string[] {
    if (!rulesDir || !fs.existsSync(rulesDir)) return [];

    return fs
      .readdirSync(rulesDir)
      .filter((f) => f.endsWith(".js"))
      .map((f) => path.join(rulesDir, f));
  }

  private loadRules(rulePaths: string[]): Rule[] {
    return rulePaths.map((rulePath) => {
      const code = fs.readFileSync(rulePath, "utf8");

      const sandbox = {
        module: { exports: {} },
        exports: {},
        console,
      };

      vm.createContext(sandbox);
      vm.runInContext(code, sandbox, {
        filename: rulePath,
        timeout: 1000,
      });

      return new Rule(path.basename(rulePath), sandbox.module.exports);
    });
  }

  private runRules(rules: Rule[], ast: ASTProgram, context: RuleContext) {
    const allDiagnostics: vscode.Diagnostic[] = [];

    for (const rule of rules) {
      try {
        outputChannel?.appendLine(`Execute ${rule.name}`);
        const diagnostics = rule.run(ast, context);
        outputChannel?.appendLine(
          `Rule ${rule.name} found ${diagnostics.length} diagnostics`,
        );
        allDiagnostics.push(...diagnostics);
      } catch (e) {
        outputChannel?.appendLine(
          `Rule ${rule.name} failed: ${JSON.stringify(e)}`,
        );
      }
    }
    return allDiagnostics;
  }
}

class DiagnosticService {
  private readonly diagnosticCollection: vscode.DiagnosticCollection;

  public constructor() {
    this.diagnosticCollection = vscode.languages.createDiagnosticCollection(
      "COBOL Language Support Linter",
    );
  }

  public showDiagnostics(
    documentUri: vscode.Uri,
    diagnostics: vscode.Diagnostic[],
  ) {
    this.diagnosticCollection.set(documentUri, diagnostics);
  }

  public showAllDiagnostics(
    documentUri: string,
    diagnostics: Map<string, vscode.Diagnostic[]>,
  ) {
    this.clearDiagnostics(documentUri);
    diagnostics.forEach((v, k) =>
      this.diagnosticCollection.set(vscode.Uri.parse(k), v),
    );
  }

  public clearDiagnostics(documentUri?: string) {
    if (documentUri)
      this.diagnosticCollection.delete(vscode.Uri.parse(documentUri));
    else this.diagnosticCollection.clear();
  }
}

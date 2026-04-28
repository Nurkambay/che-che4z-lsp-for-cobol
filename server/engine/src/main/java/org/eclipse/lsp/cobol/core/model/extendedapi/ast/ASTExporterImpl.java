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
 *    Broadcom, Inc. - initial API and implementation
 *
 */
package org.eclipse.lsp.cobol.core.model.extendedapi.ast;

import java.util.stream.Collectors;
import org.eclipse.lsp.cobol.common.model.tree.*;
import org.eclipse.lsp.cobol.common.model.tree.variable.VariableDefinitionNameNode;
import org.eclipse.lsp.cobol.common.model.tree.variable.VariableUsageNode;
import org.eclipse.lsp.cobol.core.model.extendedapi.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/** AST builder implementation */
public class ASTExporterImpl implements ASTExporter {

  @Override
  public ASTProgram export(ProgramNode programNode) {
    return (ASTProgram) convertNode(programNode);
  }

  private ASTNode convertNode(Node node) {
    ASTNode astNode = null;

    switch (node.getNodeType()) {
      case ALTER:
        AlterNode alterNode = (AlterNode) node;
        astNode = new ASTAlter(convertLocation(alterNode), alterNode.getFrom(), alterNode.getTo());
        break;
      case AT_END:
        astNode = new ASTNode("AT_END", convertLocation(node));
        break;
      case CODE_BLOCK_PARENT:
        astNode = new ASTNode("CODE_BLOCK_PARENT", convertLocation(node));
        break;
      case CODE_BLOCK_USAGE:
        astNode = new ASTNode("CODE_BLOCK_USAGE", convertLocation(node));
        break;
      case COMPILER_DIRECTIVE:
        CompilerDirectiveNode compilerDirectiveNode = (CompilerDirectiveNode) node;
        astNode =
            new ASTCompilerDirectiveNode(
                convertLocation(node),
                compilerDirectiveNode.getDirectiveText(),
                compilerDirectiveNode.getDialect());
        break;
      case COPY:
        CopyNode copyNode = (CopyNode) node;
        astNode =
            new ASTCopy(
                convertLocation(node),
                copyNode.getUri(),
                copyNode.getName(),
                convertLocation(copyNode.getNameLocation()),
                copyNode.getDefinitions().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()),
                copyNode.getUsages().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()));
        break;
      case CUSTOM:
        break;
      case DELETE_STATEMENT:
        break;
      case DIVISION:
        break;
      case EMBEDDED_CODE:
        break;
      case EVALUATE:
        break;
      case EVALUATE_WHEN:
        break;
      case EVALUATE_WHEN_OTHER:
        break;
      case EXEC_CICS_ABEND:
        break;
      case EXIT:
        break;
      case EXIT_SECTION:
        break;
      case EXIT_PARAGRAPH:
        break;
      case EXIT_PERFORM:
        break;
      case FILE_CONTROL_ENTRY:
        break;
      case FILE_USAGE:
        break;
      case FUNCTION_REFERENCE:
        break;
      case FUNCTION_DECLARATION:
        break;
      case GO_BACK:
        break;
      case GO_TO:
        break;
      case IF:
        break;
      case IF_ELSE:
        break;
      case JSON_GENERATE:
        break;
      case JSON_PARSE:
        break;
      case LITERAL:
        break;
      case MERGE:
        break;
      case OBSOLETE:
        break;
      case ON_EXCEPTION:
        break;
      case ON_NOT_EXCEPTION:
        break;
      case OPEN_STATEMENT:
        break;
      case PARAGRAPH:
        astNode = new ASTNode("PARAGRAPH", convertLocation(node));
        break;
      case PARAGRAPH_NAME_NODE:
        ParagraphNameNode paragraphNameNode = (ParagraphNameNode) node;
        astNode =
            new ASTParagraphName(
                convertLocation(node),
                paragraphNameNode.getName(),
                paragraphNameNode.getDefinitions().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()),
                paragraphNameNode.getUsages().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()));
        break;
      case PERFORM:
        break;
      case PERFORM_UNTIL_NODE:
        break;
      case PROCEDURE_SECTION:
        break;
      case PROCEDURE_RETURNING:
        break;
      case PROCEDURE_USING:
        break;
      case PROGRAM:
        astNode = new ASTProgram(((ProgramNode) node).getProgramName(), convertLocation(node));
        break;
      case PROGRAM_END:
        break;
      case PROGRAM_ID:
        break;
      case QUALIFIED_REFERENCE_NODE:
        break;
      case READ_STATEMENT:
        break;
      case REWRITE_STATEMENT:
        break;
      case ROOT:
        break;
      case SECTION:
        SectionNode sectionNode = (SectionNode) node;
        astNode = new ASTSection(convertLocation(node), sectionNode.getSectionType().name());
        break;
      case SECTION_NAME_NODE:
        SectionNameNode sectionNameNode = (SectionNameNode) node;
        astNode =
            new ASTSectionName(
                convertLocation(node),
                sectionNameNode.getName(),
                sectionNameNode.getDefinitions().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()),
                sectionNameNode.getUsages().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()));
        break;
      case SENTENCE:
        break;
      case SORT:
        break;
      case SORT_INPUT:
        break;
      case SORT_OUTPUT:
        break;
      case START_STATEMENT:
        break;
      case STATEMENT:
        break;
      case STOP:
        break;
      case SUBROUTINE:
        astNode = new ASTNode("SUBROUTINE", convertLocation(node));
        break;
      case SUBROUTINE_NAME_NODE:
        SubroutineNameNode subroutineNameNode = (SubroutineNameNode) node;
        astNode =
            new ASTSubroutineName(
                convertLocation(node),
                subroutineNameNode.getName(),
                subroutineNameNode.getDefinitions().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()),
                subroutineNameNode.getUsages().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()));
        break;
      case VARIABLE:
        break;
      case VARIABLE_DEFINITION:
        break;
      case VARIABLE_DEFINITION_NAME:
        VariableDefinitionNameNode variableDefinitionNameNode = (VariableDefinitionNameNode) node;
        astNode =
            new ASTVariableDefinitionName(
                convertLocation(node),
                variableDefinitionNameNode.getName(),
                variableDefinitionNameNode.getDefinitions().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()),
                variableDefinitionNameNode.getUsages().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()));
        break;
      case VARIABLE_USAGE:
        VariableUsageNode variableUsageNode = (VariableUsageNode) node;
        astNode =
            new ASTVariableUsage(
                convertLocation(node),
                variableUsageNode.getName(),
                variableUsageNode.getDefinitions().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()),
                variableUsageNode.getUsages().stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList()));
        break;
      case WRITE_STATEMENT:
        break;
      case XML_PARSE:
        break;
      case XML_GENERATE:
        break;
      default:
        astNode = new ASTNode(node.getNodeType().name(), convertLocation(node));
    }
    if (astNode == null) {
      astNode = new ASTNode(node.getNodeType().name(), convertLocation(node));
    }
    ASTNode finalAstNode = astNode;
    node.getChildren().forEach(ch -> finalAstNode.getChildren().add(convertNode(ch)));
    return astNode;
  }

  private Location convertLocation(Node node) {
    return convertLocation(node.getLocality().toLocation());
  }

  private Location convertLocation(org.eclipse.lsp4j.Location location) {
    final Range range = location.getRange();

    final Position startPosition = new Position();
    startPosition.setLine(range.getStart().getLine() + 1);
    startPosition.setCharacter(range.getStart().getCharacter() + 1);
    final Position endPosition = new Position();
    endPosition.setLine(range.getEnd().getLine() + 1);
    endPosition.setCharacter(range.getEnd().getCharacter() + 1);

    return new Location(location.getUri(), startPosition, endPosition);
  }
}

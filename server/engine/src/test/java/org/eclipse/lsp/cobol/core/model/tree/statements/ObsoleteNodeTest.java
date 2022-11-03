/*
 * Copyright (c) 2022 Broadcom.
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
package org.eclipse.lsp.cobol.core.model.tree.statements;

import com.google.common.collect.ImmutableList;
import org.eclipse.lsp.cobol.core.engine.processor.AstProcessor;
import org.eclipse.lsp.cobol.core.engine.processor.ProcessingContext;
import org.eclipse.lsp.cobol.core.engine.processor.ProcessingPhase;
import org.eclipse.lsp.cobol.core.engine.processor.ProcessorDescription;
import org.eclipse.lsp.cobol.core.messages.MessageTemplate;
import org.eclipse.lsp.cobol.core.model.ErrorSeverity;
import org.eclipse.lsp.cobol.core.model.ErrorSource;
import org.eclipse.lsp.cobol.core.model.Locality;
import org.eclipse.lsp.cobol.core.model.SyntaxError;
import org.eclipse.lsp.cobol.core.model.tree.RemarksNode;
import org.eclipse.lsp.cobol.core.model.tree.RootNode;
import org.eclipse.lsp.cobol.core.model.tree.logic.ObsoleteNodeCheck;
import org.eclipse.lsp.cobol.core.semantics.CopybooksRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test {@link ObsoleteNode} */
class ObsoleteNodeTest {
  @Test
  public void testObsoleteNodeWarning() {
    Locality locality = Locality.builder().build();
    RootNode rootNode = new RootNode(locality, new CopybooksRepository());
    RemarksNode remarksNode = new RemarksNode(locality);
    AstProcessor astProcessor = new AstProcessor();
    List<SyntaxError> errors = new ArrayList<>();
    ProcessingContext ctx = new ProcessingContext(errors);
    ctx.register(
        new ProcessorDescription(
            ObsoleteNode.class, ProcessingPhase.TRANSFORMATION, new ObsoleteNodeCheck()));
    rootNode.addChild(remarksNode);
    astProcessor.process(ProcessingPhase.TRANSFORMATION, rootNode, ctx);

    assertEquals(
        errors,
        ImmutableList.of(
            SyntaxError.syntaxError()
                .errorSource(ErrorSource.PARSING)
                .severity(ErrorSeverity.WARNING)
                .locality(locality)
                .messageTemplate(MessageTemplate.of("cobolParser.ObsoleteCode"))
                .build()));
  }
}

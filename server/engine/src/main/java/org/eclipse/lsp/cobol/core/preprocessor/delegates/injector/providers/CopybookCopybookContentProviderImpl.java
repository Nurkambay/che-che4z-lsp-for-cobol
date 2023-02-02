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
package org.eclipse.lsp.cobol.core.preprocessor.delegates.injector.providers;

import lombok.AllArgsConstructor;
import org.eclipse.lsp.cobol.common.copybook.*;

import java.util.Optional;

/**
 * Provides a content of a copybook for injecting
 */
@AllArgsConstructor
public class CopybookCopybookContentProviderImpl implements CopybookContentProvider {

  private final CopybookService copybookService;

  @Override
  public Optional<CopybookModel> read(CopybookConfig copybookConfig, CopybookName copybookName, String programDocumentUri, String documentUri) {
    CopybookModel copybookModel = copybookService.resolve(
        copybookName.toCopybookId(programDocumentUri),
        copybookName,
        programDocumentUri,
        documentUri,
        copybookConfig,
        false);
    if (copybookModel.getContent() == null) {
      return Optional.empty();
    }
    return Optional.of(copybookModel);
  }
}

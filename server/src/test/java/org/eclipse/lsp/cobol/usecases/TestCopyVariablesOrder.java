/*
 * Copyright (c) 2020 Broadcom.
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
package org.eclipse.lsp.cobol.usecases;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.eclipse.lsp.cobol.core.engine.dialects.idms.IdmsDialect;
import org.eclipse.lsp.cobol.core.preprocessor.delegates.copybooks.DialectType;
import org.eclipse.lsp.cobol.positive.CobolText;
import org.eclipse.lsp.cobol.usecases.engine.UseCaseEngine;
import org.junit.jupiter.api.Test;

/**
 * Test copybook variables + regular variables ordering
 */
class TestCopyVariablesOrder {

  private static final String TEXT_IDMS_MAID_WRK = "000100* BMVG261M,????         S/L-XREF SOURCE-LEVEL: 001 217348\n"
      + "000200 IDENTIFICATION DIVISION.\n"
      + "000300 PROGRAM-ID.    PROG1.                                              D-----\n"
      + "001500 ENVIRONMENT  DIVISION.\n"
      + "001900 CONFIGURATION    SECTION.\n"
      + "002100 DATA   DIVISION.\n"
      + "002200 WORKING-STORAGE SECTION.\n"
      + "106300 01  {$*AREA-XWV}.                                                    41D00000\n"
      + "106500     03 {$*TBLEPL-XWV}.                                               41D00004\n"
      + "106700       05 {$*ROWEPL-XWV}                         OCCURS 999.          41D00006\n"
      + "107000         07 COPY MAID {~EPLREL-XEP`EPLREL-XEP_WRK} WRK.                             41\n"
      + "110600 01  COPY IDMS {~COPY2}.                                             B9D0001C\n"
      + "110700 LINKAGE SECTION.                                                   D0008A\n";

  private static final String COPY_FILLER = "       01  FILLER                      PIC X(0).";

  private static final String TEXT_IDMS_MAID = "000100 IDENTIFICATION DIVISION.\n"
      + "000200 PROGRAM-ID.    PROG4.                                            \n"
      + "000300 ENVIRONMENT    DIVISION.\n"
      + "000400 DATA   DIVISION.\n"
      + "000500 WORKING-STORAGE SECTION.\n"
      + "000600 01  {$*MRB}.                                                           \n"
      + "000700     03 COPY IDMS {~COPY1}.\n"
      + "000800 01  COPY MAID {~COPY2}.\n"
      + "000900 LINKAGE SECTION.                                                   \n";

  private static final String COPY_MAID = "     1 01  {$*LDOMCNTM-XT4}.                                                   115\n"
      + "    10     03 {$*ADDTXT-XT4}               PIC X(50)   VALUE SPACE.             54\n"
      + "    11     03 FILLER                   PIC X(12)   VALUE SPACE.            104\n";

  @Test
  void testIdmsAndMaidWithLevels() {
    UseCaseEngine.runTest(
        TEXT_IDMS_MAID_WRK, ImmutableList.of(
            new CobolText("EPLREL-XEP_WRK", DialectType.MAID.name(), COPY_FILLER),
            new CobolText("COPY2", IdmsDialect.NAME, COPY_FILLER)),
        ImmutableMap.of(), ImmutableList.of(), DialectConfigs.getFullAnalysisConfig());
  }

  @Test
  void testIdmsAndMaid() {
    UseCaseEngine.runTest(
        TEXT_IDMS_MAID, ImmutableList.of(
            new CobolText("COPY1", IdmsDialect.NAME, COPY_FILLER),
            new CobolText("COPY2", DialectType.MAID.name(), COPY_MAID)),
        ImmutableMap.of(), ImmutableList.of(), DialectConfigs.getFullAnalysisConfig());
  }

}

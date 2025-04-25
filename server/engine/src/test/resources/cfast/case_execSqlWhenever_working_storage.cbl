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
      *    Broadcom, Inc. - initial API and implementation
      *
        IDENTIFICATION DIVISION.
        PROGRAM-ID.    CBACT01C.
        DATA DIVISION.
        WORKING-STORAGE SECTION.
              EXEC SQL  WHENEVER SQLERROR GOTO HANDLER  END-EXEC.
        PROCEDURE DIVISION.
        HANDLER.
              DISPLAY "HANDLER".

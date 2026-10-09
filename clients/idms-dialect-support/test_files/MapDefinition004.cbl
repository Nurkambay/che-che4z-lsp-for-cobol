       IDENTIFICATION DIVISION.
       PROGRAM-ID.    PROG1.
       ENVIRONMENT  DIVISION.
       IDMS-CONTROL SECTION.
       PROTOCOL.    MODE IS IDMS-DC-NONAUTO DEBUG
                       IDMS-RECORDS            MANUAL.
       CONFIGURATION    SECTION.
       DATA   DIVISION.
           SCHEMA SECTION.
                DB DB1 WITHIN IDMSDB1.
                MAP SECTION.
       MAP MAP1.
       WORKING-STORAGE SECTION.
       01  WS-AREA.
           03 VAR-FLAG             PIC X(8)    VALUE 'INACTIVE'.
       PROCEDURE DIVISION.
           INQUIRE MAP MAP1
           IF CURSOR AT DFLD VAR-FLAG THEN
                MODIFY MAP MAP1
                FOR DFLD VAR-FLAG EDIT ERROR
           ELSE
                MOVE 99 TO VAR-FLAG.

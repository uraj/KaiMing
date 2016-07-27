0x3BF4	towel_root:
0x3BF4  STMFD           SP!, {R4-R11,LR}
0x3BF8  ADD             R11, SP, #0x1C
0x3BFC  SUB             SP, SP, #0x1C
0x3C00  SUB             SP, SP, #0x1000
0x3C04  ADD             R4, SP, #0x17
0x3C08  STR             R0, [SP,#0x10]
0x3C0C  MOV             R1, #0x1000
0x3C10  MOV             R2, #0
0x3C14  MOV             R0, R4
0x3C18  BL              0xB3C
0x3C1C  LDR             R0, =0x2C1B8
0x3C20  LDR             R1, =0xFFFE16E7
0x3C24  LDR             R2, =0xFFFE16F5
0x3C28  ADD             R3, PC, R0 
0x3C2C  ADD             R0, R1, R3
0x3C30  ADD             R1, R2, R3
0x3C34  BL              0xB90
0x3C38  MOV             R6, R0
0x3C3C  MOV             R0, R4  
0x3C40  MOV             R1, #1  
0x3C44  MOV             R2, #0xFFF
0x3C48  MOV             R3, R6  
0x3C4C  BL              0xB9C
0x3C50  MOV             R0, R6  
0x3C54  BL              0xBA8
0x3C58  MOV             R10, #0xE36E
0x3C5C  MOV             R7, #0x47BF
0x3C60  MOV             R5, #0x6527
0x3C64  MOV             R4, #0xE36D
0x3C68  MOV             R8, #0xB2D9
0x3C6C  MOV             R1, #0x1DE3
0x3C70  MOV             R6, #0xBCCD
0x3C74  MOV             LR, #0x47C0
0x3C78  MOV             R12, #0x1DE4
0x3C7C  MOV             R2, #0xB2DA
0x3C80  MOVT            R10, #0x7AD5
0x3C84  MOVT            R7, #0xD9F9
0x3C88  MOVT            R5, #0x904B
0x3C8C  MOVT            R4, #0x7AD5
0x3C90  MOVT            R8, #0x69D7
0x3C94  MOVT            R1, #0x4118
0x3C98  MOVT            R6, #0x2CF2
0x3C9C  MOVT            LR, #0xD9F9
0x3CA0  MOVT            R12, #0x4118
0x3CA4  MOVT            R2, #0x69D7
0x3CA8  MOV             R0, R10
0x3CAC  B               0x3CB8
0x3CB0  CMP             R0, R2
0x3CB4  MOVEQ           R0, R12
0x3CB8  CMP             R0, R7
0x3CBC  BGT             0x3CCC
0x3CC0  CMP             R0, R5
0x3CC4  BNE             0x3CB8
0x3CC8  B               0x3E30
0x3CCC  CMP             R0, R4
0x3CD0  BGT             0x3D10
0x3CD4  CMP             R0, R8
0x3CD8  BGT             0x3CB0
0x3CDC  CMP             R0, R1
0x3CE0  BGT             0x3D34
0x3CE4  CMP             R0, R6
0x3CE8  BGT             0x3DA4
0x3CEC  CMP             R0, LR
0x3CF0  BEQ             0x3DB8
0x3CF4  MOV             R3, #0xF297
0x3CF8  MOVT            R3, #0xDFAE
0x3CFC  CMP             R0, R3
0x3D00  BNE             0x3DCC
0x3D04  LDRB            R9, [R11,#-0x21]
0x3D08  MOV             R0, LR
0x3D0C  B               0x3CB8
0x3D10  CMP             R0, R10
0x3D14  BNE             0x3CB8
0x3D18  LDRB            R0, [SP,#0x17]
0x3D1C  MOV             R9, #0
0x3D20  CMP             R0, #0
0x3D24  MOV             R0, LR
0x3D28  MOVNE           R0, #0xB2DA
0x3D2C  MOVTNE          R0, #0x69D7
0x3D30  B               0x3CB8
0x3D34  CMP             R0, R12
0x3D38  BNE             0x3CB8
0x3D3C  STR             R9, [SP,#0xC]
0x3D40  ADD             R9, SP, #0x17
0x3D44  MOV             R6, LR
0x3D48  MOV             R0, R9  
0x3D4C  BL              0xBB4
0x3D50  ADD             R0, R0, R9
0x3D54  MOV             R3, #0x1DE4
0x3D58  SUB             R0, R0, #1
0x3D5C  MOV             R1, #0x1DE3
0x3D60  MOV             R2, #0xB2DA
0x3D64  MOVT            R3, #0x4118
0x3D68  LDRB            R0, [R0]
0x3D6C  MOV             LR, R6
0x3D70  MOV             R6, #0xBCCD
0x3D74  MOV             R12, R3
0x3D78  CMP             R0, #0xA
0x3D7C  MOV             R0, #0
0x3D80  MOVEQ           R0, #1
0x3D84  MOVT            R1, #0x4118
0x3D88  STRB            R0, [R11,#-0x21]
0x3D8C  MOV             R0, #0xF297
0x3D90  MOVT            R6, #0x2CF2
0x3D94  LDR             R9, [SP,#0xC]
0x3D98  MOVT            R2, #0x69D7
0x3D9C  MOVT            R0, #0xDFAE
0x3DA0  B               0x3CB8
0x3DA4  MOV             R3, #0xBCCE
0x3DA8  MOVT            R3, #0x2CF2
0x3DAC  CMP             R0, R3
0x3DB0  MOVEQ           R0, R12
0x3DB4  B               0x3CB8
0x3DB8  TST             R9, #1
0x3DBC  MOV             R0, R5
0x3DC0  MOVNE           R0, #0xD611
0x3DC4  MOVTNE          R0, #0x2145
0x3DC8  B               0x3CB8
0x3DCC  MOV             R3, #0xD611
0x3DD0  MOVT            R3, #0x2145
0x3DD4  CMP             R0, R3
0x3DD8  BNE             0x3CB8
0x3DDC  STR             R9, [SP,#0xC]
0x3DE0  ADD             R9, SP, #0x17
0x3DE4  MOV             R0, R9  
0x3DE8  BL              0xBB4
0x3DEC  ADD             R0, R0, R9
0x3DF0  MOV             R2, #0x47C0
0x3DF4  SUB             R0, R0, #1
0x3DF8  MOV             R3, #0x1DE4
0x3DFC  MOVT            R2, #0xD9F9
0x3E00  MOV             R1, #0
0x3E04  STRB            R1, [R0]
0x3E08  MOV             R1, #0x1DE3
0x3E0C  MOVT            R3, #0x4118
0x3E10  MOV             LR, R2
0x3E14  MOV             R2, #0xB2DA
0x3E18  MOV             R12, R3
0x3E1C  LDR             R9, [SP,#0xC]
0x3E20  MOVT            R2, #0x69D7
0x3E24  MOVT            R1, #0x4118
0x3E28  MOV             R0, R10
0x3E2C  B               0x3CB8
0x3E30  LDR             R0, [SP,#0x10]
0x3E34  LDR             R1, [R0]
0x3E38  LDR             R2, [R1,#0x29C]
0x3E3C  ADD             R1, SP, #0x17
0x3E40  BLX             R2
0x3E44  SUB             SP, R11, #0x1C
0x3E48  LDMFD           SP!, {R4-R11,PC}

0x00444800	SomeCPPFunc:
0x444800	push   %ebp
0x444801	push   %edi
0x444802	push   %esi
0x444803	push   %ebx
0x444804	mov    %eip,%ebx
0x444809	add    $0x7b3789b,%ebx
0x44480f	sub    $0x128,%esp
0x444815	mov    0x13c(%esp),%ebp
0x44481c	mov    %gs:0x14,%eax
0x444822	mov    %eax,0x118(%esp)
0x444829	xor    %eax,%eax
0x44482b	push   %ebp
0x44482c	call 0x442f40
0x444831	add    $0x10,%esp
0x444834	test   %al,%al
0x444836	je 0x444860
0x444838	mov    0x10c(%esp),%eax
0x44483f	xor    %gs:0x14,%eax
0x444846	jne 0x444be6
0x44484c	add    $0x11c,%esp
0x444852	pop    %ebx
0x444853	pop    %esi
0x444854	pop    %edi
0x444855	pop    %ebp
0x444856	ret
0x444857	mov    %esi,%esi
0x444859	lea    0x0(%edi,%eiz,1),%edi
0x444860	mov    %al,0xd(%esp)
0x444864	lea    0x2b(%esp),%eax
0x444868	lea    0x2c(%esp),%edi
0x44486c	lea    -0x2dd0d48(%ebx),%esi
0x444872	sub    $0xc,%esp
0x444875	mov    %eax,0x10(%esp)
0x444879	mov    %eax,%ecx
0x44487b	mov    -0x2dd0d48(%ebx),%eax
0x444881	sub    %edi,%ecx
0x444883	sub    %ecx,%esi
0x444885	add    $0xe1,%ecx
0x44488b	mov    %eax,0x37(%esp)
0x44488f	mov    -0x2dd0c6b(%ebx),%eax
0x444895	shr    $0x2,%ecx
0x444898	mov    %eax,0x114(%esp)
0x44489f	rep movsl %ds:(%esi),%es:(%edi)
0x4448a1	push   %ebp
0x4448a2	lea    -0x2dd0a24(%ebx),%edi
0x4448a8	call 0x442e50
0x4448ad	mov    %eax,%esi
0x4448af	pop    %eax
0x4448b0	pop    %edx
0x4448b1	push   %edi
0x4448b2	push   %esi
0x4448b3	call 0x328b3a0
0x4448b8	add    $0x10,%esp
0x4448bb	test   %al,%al
0x4448bd	movzbl 0xd(%esp),%edx
0x4448c2	jne 0x444958
0x4448c8	lea    0x18(%esp),%eax
0x4448cc	mov    %dl,0xe(%esp)
0x4448d0	sub    $0x4,%esp
0x4448d3	push   %edi
0x4448d4	pushl  0xc(%esp)
0x4448d8	push   %eax
0x4448d9	call 0xa30220
0x4448de	sub    $0xc,%esp
0x4448e1	pushl  0x30(%esp)
0x4448e5	push   %esi
0x4448e6	call 0x328ba00
0x4448eb	mov    %al,0x2d(%esp)
0x4448ef	add    $0x20,%esp
0x4448f2	test   %al,%al
0x4448f4	movzbl 0xe(%esp),%edx
0x4448f9	jne 0x44495d
0x4448fb	movb   $0x0,0xe(%esp)
0x444900	movb   $0x1,0xd(%esp)
0x444905	xor    %ecx,%ecx
0x444907	cmpb   $0x0,0xe(%esp)
0x44490c	je 0x444921
0x44490e	mov    0x1c(%esp),%eax
0x444912	lea    -0xc(%eax),%edx
0x444915	cmp    0x3f30(%ebx),%edx
0x44491b	jne 0x444b30
0x444921	cmpb   $0x0,0xd(%esp)
0x444926	je 0x44493b
0x444928	mov    0x18(%esp),%eax
0x44492c	lea    -0xc(%eax),%edx
0x44492f	cmp    0x3f30(%ebx),%edx
0x444935	jne 0x444b70
0x44493b	test   %cl,%cl
0x44493d	jne 0x444838
0x444943	sub    $0xc,%esp
0x444946	push   %ebp
0x444947	call 0x442ec0
0x44494c	add    $0x10,%esp
0x44494f	jmp 0x444838
0x444954	lea    0x0(%esi,%eiz,1),%esi
0x444958	movb   $0x0,0xd(%esp)
0x44495d	lea    -0x2dd0a64(%ebx),%edi
0x444963	mov    %dl,0xe(%esp)
0x444967	sub    $0x8,%esp
0x44496a	push   %edi
0x44496b	push   %esi
0x44496c	call 0x328b3a0
0x444971	add    $0x10,%esp
0x444974	test   %al,%al
0x444976	movzbl 0xe(%esp),%edx
0x44497b	jne 0x4449c0
0x44497d	lea    0x1c(%esp),%eax
0x444981	lea    -0x2dd0da4(%ebx),%ecx
0x444987	mov    %dl,0x8(%esp)
0x44498b	sub    $0x4,%esp
0x44498e	push   %edi
0x44498f	push   %ecx
0x444990	push   %eax
0x444991	call 0xa30220
0x444996	sub    $0xc,%esp
0x444999	pushl  0x34(%esp)
0x44499d	push   %esi
0x44499e	call 0x328ba00
0x4449a3	mov    %al,0x2e(%esp)
0x4449a7	add    $0x20,%esp
0x4449aa	test   %al,%al
0x4449ac	movzbl 0x8(%esp),%edx
0x4449b1	jne 0x4449c5
0x4449b3	xor    %ecx,%ecx
0x4449b5	jmp 0x44490e
0x4449ba	lea    0x0(%esi),%esi
0x4449c0	movb   $0x0,0xe(%esp)
0x4449c5	lea    -0x2dd0aa4(%ebx),%edi
0x4449cb	mov    %dl,0x8(%esp)
0x4449cf	sub    $0x8,%esp
0x4449d2	push   %edi
0x4449d3	push   %esi
0x4449d4	call 0x328b3a0
0x4449d9	add    $0x10,%esp
0x4449dc	test   %al,%al
0x4449de	movzbl 0x8(%esp),%edx
0x4449e3	jne 0x444a11
0x4449e5	lea    0x20(%esp),%eax
0x4449e9	sub    $0x4,%esp
0x4449ec	push   %edi
0x4449ed	pushl  0xc(%esp)
0x4449f1	push   %eax
0x4449f2	call 0xa30220
0x4449f7	sub    $0xc,%esp
0x4449fa	pushl  0x38(%esp)
0x4449fe	push   %esi
0x4449ff	call 0x328ba00
0x444a04	add    $0x20,%esp
0x444a07	test   %al,%al
0x444a09	mov    %eax,%edx
0x444a0b	je 0x444a90
0x444a11	lea    -0x2dd0ae4(%ebx),%edi
0x444a17	mov    %dl,0x4(%esp)
0x444a1b	sub    $0x8,%esp
0x444a1e	push   %edi
0x444a1f	push   %esi
0x444a20	call 0x328b3a0
0x444a25	add    $0x10,%esp
0x444a28	test   %al,%al
0x444a2a	mov    $0x1,%ecx
0x444a2f	movzbl 0x4(%esp),%edx
0x444a34	je 0x444a98
0x444a36	test   %dl,%dl
0x444a38	je 0x444907
0x444a3e	mov    0x20(%esp),%eax
0x444a42	lea    -0xc(%eax),%edx
0x444a45	cmp    0x3f30(%ebx),%edx
0x444a4b	je 0x444907
0x444a51	mov    0x499c(%ebx),%esi
0x444a57	test   %esi,%esi
0x444a59	je 0x444bbb
0x444a5f	mov    $0xffffffff,%esi
0x444a64	lock xadd %esi,-0x4(%eax)
0x444a69	test   %esi,%esi
0x444a6b	jg 0x444907
0x444a71	mov    %cl,0x4(%esp)
0x444a75	sub    $0x8,%esp
0x444a78	lea    0x2c(%esp),%eax
0x444a7c	push   %eax
0x444a7d	push   %edx
0x444a7e	call 0x3b49c0
0x444a83	add    $0x10,%esp
0x444a86	movzbl 0x4(%esp),%ecx
0x444a8b	jmp 0x444907
0x444a90	xor    %ecx,%ecx
0x444a92	jmp 0x444a3e
0x444a94	lea    0x0(%esi,%eiz,1),%esi
0x444a98	lea    0x24(%esp),%eax
0x444a9c	lea    -0x2dd0da4(%ebx),%ecx
0x444aa2	sub    $0x4,%esp
0x444aa5	push   %edi
0x444aa6	push   %ecx
0x444aa7	push   %eax
0x444aa8	call 0xa30220
0x444aad	sub    $0xc,%esp
0x444ab0	pushl  0x3c(%esp)
0x444ab4	push   %esi
0x444ab5	call 0x328ba00
0x444aba	mov    %eax,%ecx
0x444abc	mov    0x44(%esp),%eax
0x444ac0	lea    -0xc(%eax),%edi
0x444ac3	mov    %edi,0x28(%esp)
0x444ac7	add    $0x20,%esp
0x444aca	cmp    0x3f30(%ebx),%edi
0x444ad0	movzbl 0x4(%esp),%edx
0x444ad5	je 0x444a36
0x444adb	mov    0x499c(%ebx),%edi
0x444ae1	test   %edi,%edi
0x444ae3	je 0x444bad
0x444ae9	mov    $0xffffffff,%esi
0x444aee	lock xadd %esi,-0x4(%eax)
0x444af3	test   %esi,%esi
0x444af5	jg 0x444a36
0x444afb	mov    %cl,0xf(%esp)
0x444aff	mov    %dl,0x4(%esp)
0x444b03	sub    $0x8,%esp
0x444b06	lea    0x1f(%esp),%eax
0x444b0a	push   %eax
0x444b0b	pushl  0x14(%esp)
0x444b0f	call 0x3b49c0
0x444b14	add    $0x10,%esp
0x444b17	movzbl 0xf(%esp),%ecx
0x444b1c	movzbl 0x4(%esp),%edx
0x444b21	jmp 0x444a36
0x444b26	lea    0x0(%esi),%esi
0x444b29	lea    0x0(%edi,%eiz,1),%edi
0x444b30	mov    0x499c(%ebx),%edi
0x444b36	test   %edi,%edi
0x444b38	je 0x444bcb
0x444b3e	mov    $0xffffffff,%esi
0x444b43	lock xadd %esi,-0x4(%eax)
0x444b48	test   %esi,%esi
0x444b4a	jg 0x444921
0x444b50	mov    %cl,0xe(%esp)
0x444b54	sub    $0x8,%esp
0x444b57	lea    0x2c(%esp),%eax
0x444b5b	push   %eax
0x444b5c	push   %edx
0x444b5d	call 0x3b49c0
0x444b62	add    $0x10,%esp
0x444b65	movzbl 0xe(%esp),%ecx
0x444b6a	jmp 0x444921
0x444b6f	nop
0x444b70	mov    0x499c(%ebx),%esi
0x444b76	test   %esi,%esi
0x444b78	je 0x444bd9
0x444b7a	mov    $0xffffffff,%esi
0x444b7f	lock xadd %esi,-0x4(%eax)
0x444b84	mov    %esi,%eax
0x444b86	test   %eax,%eax
0x444b88	jg 0x44493b
0x444b8e	mov    %cl,0xd(%esp)
0x444b92	sub    $0x8,%esp
0x444b95	lea    0x2c(%esp),%eax
0x444b99	push   %eax
0x444b9a	push   %edx
0x444b9b	call 0x3b49c0
0x444ba0	add    $0x10,%esp
0x444ba3	movzbl 0xd(%esp),%ecx
0x444ba8	jmp 0x44493b
0x444bad	mov    -0x4(%eax),%esi
0x444bb0	lea    -0x1(%esi),%edi
0x444bb3	mov    %edi,-0x4(%eax)
0x444bb6	jmp 0x444af3
0x444bbb	mov    -0x4(%eax),%edi
0x444bbe	lea    -0x1(%edi),%esi
0x444bc1	mov    %esi,-0x4(%eax)
0x444bc4	mov    %edi,%esi
0x444bc6	jmp 0x444a69
0x444bcb	mov    -0x4(%eax),%esi
0x444bce	lea    -0x1(%esi),%edi
0x444bd1	mov    %edi,-0x4(%eax)
0x444bd4	jmp 0x444b48
0x444bd9	mov    -0x4(%eax),%esi
0x444bdc	lea    -0x1(%esi),%edi
0x444bdf	mov    %edi,-0x4(%eax)
0x444be2	mov    %esi,%eax
0x444be4	jmp 0x444b86
0x444be6	call 0x519a300
0x444beb	xchg   %ax,%ax
0x444bed	xchg   %ax,%ax
0x444bef	nop
0x004459a0	_ZN10predictors31ResourcePrefetchPredictorTables10DeleteDataERKSt6vectorISsSaISsEES5_:
0x4459a0	push   %ebp
0x4459a1	push   %edi
0x4459a2	push   %esi
0x4459a3	push   %ebx
0x4459a4	mov    %eip,%ebx
0x4459a9	add    $0x7b366fb,%ebx
0x4459af	sub    $0x18,%esp
0x4459b2	mov    0x2c(%esp),%esi
0x4459b6	mov    0x30(%esp),%ebp
0x4459ba	mov    0x34(%esp),%edi
0x4459be	push   %esi
0x4459bf	call 0x442f40
0x4459c4	add    $0x10,%esp
0x4459c7	test   %al,%al
0x4459c9	jne 0x445a08
0x4459cb	mov    0x0(%ebp),%eax
0x4459ce	cmp    %eax,0x4(%ebp)
0x4459d1	je 0x4459e2
0x4459d3	sub    $0x4,%esp
0x4459d6	push   %ebp
0x4459d7	push   $0x1
0x4459d9	push   %esi
0x4459da	call 0x445890
0x4459df	add    $0x10,%esp
0x4459e2	mov    (%edi),%eax
0x4459e4	cmp    %eax,0x4(%edi)
0x4459e7	je 0x445a08
0x4459e9	mov    %edi,0x28(%esp)
0x4459ed	mov    %esi,0x20(%esp)
0x4459f1	movl   $0x0,0x24(%esp)
0x4459f9	add    $0xc,%esp
0x4459fc	pop    %ebx
0x4459fd	pop    %esi
0x4459fe	pop    %edi
0x4459ff	pop    %ebp
0x445a00	jmp 0x445890
0x445a05	lea    0x0(%esi),%esi
0x445a08	add    $0xc,%esp
0x445a0b	pop    %ebx
0x445a0c	pop    %esi
0x445a0d	pop    %edi
0x445a0e	pop    %ebp
0x445a0f	ret
0x00445a10	_ZN10predictors31ResourcePrefetchPredictorTables21DeleteSingleDataPointERKSsNS_15PrefetchKeyTypeE:
0x445a10	push   %ebp
0x445a11	push   %edi
0x445a12	push   %esi
0x445a13	push   %ebx
0x445a14	mov    %eip,%ebx
0x445a19	add    $0x7b3668b,%ebx
0x445a1f	sub    $0x28,%esp
0x445a22	mov    0x3c(%esp),%esi
0x445a26	push   %esi
0x445a27	call 0x442f40
0x445a2c	add    $0x10,%esp
0x445a2f	test   %al,%al
0x445a31	je 0x445a40
0x445a33	add    $0x1c,%esp
0x445a36	pop    %ebx
0x445a37	pop    %esi
0x445a38	pop    %edi
0x445a39	pop    %ebp
0x445a3a	ret
0x445a3b	nop
0x445a3c	lea    0x0(%esi,%eiz,1),%esi
0x445a40	movl   $0x0,0x4(%esp)
0x445a48	movl   $0x0,0x8(%esp)
0x445a50	sub    $0xc,%esp
0x445a53	movl   $0x0,0x18(%esp)
0x445a5b	push   $0x4
0x445a5d	call 0x519f7e0
0x445a62	lea    0x4(%eax),%edx
0x445a65	mov    %eax,0x14(%esp)
0x445a69	mov    %eax,0x18(%esp)
0x445a6d	mov    %edx,0x1c(%esp)
0x445a71	pop    %edi
0x445a72	pop    %ebp
0x445a73	pushl  0x3c(%esp)
0x445a77	push   %eax
0x445a78	call 0x3b84f0
0x445a7d	mov    0x1c(%esp),%eax
0x445a81	mov    %eax,0x18(%esp)
0x445a85	add    $0xc,%esp
0x445a88	lea    0x8(%esp),%eax
0x445a8c	push   %eax
0x445a8d	pushl  0x40(%esp)
0x445a91	push   %esi
0x445a92	call 0x445890
0x445a97	mov    0x18(%esp),%edi
0x445a9b	mov    0x14(%esp),%esi
0x445a9f	add    $0x10,%esp
0x445aa2	cmp    %esi,%edi
0x445aa4	jne 0x445ab7
0x445aa6	jmp 0x445afc
0x445aa8	nop
0x445aa9	lea    0x0(%esi,%eiz,1),%esi
0x445ab0	add    $0x4,%esi
0x445ab3	cmp    %esi,%edi
0x445ab5	je 0x445af8
0x445ab7	mov    (%esi),%eax
0x445ab9	lea    -0xc(%eax),%edx
0x445abc	cmp    0x3f30(%ebx),%edx
0x445ac2	je 0x445ab0
0x445ac4	mov    0x499c(%ebx),%ecx
0x445aca	test   %ecx,%ecx
0x445acc	je 0x445b20
0x445ace	mov    $0xffffffff,%ecx
0x445ad3	lock xadd %ecx,-0x4(%eax)
0x445ad8	mov    %ecx,%eax
0x445ada	test   %eax,%eax
0x445adc	jg 0x445ab0
0x445ade	sub    $0x8,%esp
0x445ae1	lea    0xb(%esp),%eax
0x445ae5	push   %eax
0x445ae6	push   %edx
0x445ae7	call 0x3b49c0
0x445aec	add    $0x10,%esp
0x445aef	jmp 0x445ab0
0x445af1	lea    0x0(%esi,%eiz,1),%esi
0x445af8	mov    0x4(%esp),%edi
0x445afc	test   %edi,%edi
0x445afe	je 0x445a33
0x445b04	sub    $0xc,%esp
0x445b07	push   %edi
0x445b08	call 0x51a0300
0x445b0d	add    $0x10,%esp
0x445b10	add    $0x1c,%esp
0x445b13	pop    %ebx
0x445b14	pop    %esi
0x445b15	pop    %edi
0x445b16	pop    %ebp
0x445b17	ret
0x445b18	nop
0x445b19	lea    0x0(%esi,%eiz,1),%esi
0x445b20	mov    -0x4(%eax),%ecx
0x445b23	lea    -0x1(%ecx),%ebp
0x445b26	mov    %ebp,-0x4(%eax)
0x445b29	mov    %ecx,%eax
0x445b2b	jmp 0x445ada
0x445b2d	xchg   %ax,%ax
0x445b2f	nop

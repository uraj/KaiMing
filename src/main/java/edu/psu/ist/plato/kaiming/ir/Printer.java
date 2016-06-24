package edu.psu.ist.plato.kaiming.ir;

import java.io.OutputStream;
import java.io.PrintWriter;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class Printer extends PrintWriter {

    public Printer(OutputStream out) {
        super(out, true);
    }
    
    public static Printer out = new Printer(System.out);
    public static Printer err = new Printer(System.err);

    public void printConst(Const c) {
        long value = c.value();
        if (value < 0) {
            print(String.format("-0x%x", -value));
        } else {
            print(String.format("0x%x", value));
        }
    }
    
    public void printReg(Reg r) {
        print('%');
        print(r.machRegister().name());
    }
    
    public void printVar(Var v) {
        print("@(");
        print(v.name());
        print(":");
        print(v.sizeInBits());
        print(")");
    }
    
    public void printFlg(Flg f) {
        print('%');
        print(f.flag.name());
    }
    
    public void printLval(Lval lv) {
        if (lv instanceof Reg) {
            printReg((Reg)lv);
        } else if (lv instanceof Var) {
            printVar((Var)lv);
        } else if (lv instanceof Flg) {
            printFlg((Flg)lv);
        } else {
            Assert.unreachable();
        }
    }
    
    private void printBExprOperator(BExpr.Op op) {
        switch (op) {
            case ADD:    print('+'); break;
            case AND:    print('&'); break;
            case CONCAT: print(':'); break;
            case DIV:    print('/'); break;
            case MUL:    print('*'); break;
            case OR:     print('|'); break;
            case SAR:    print(">>>"); break;
            case SHL:    print("<<"); break;
            case SHR:    print(">>"); break;
            case ROR:    print("><"); break;
            case SUB:    print('-'); break;
            case XOR:    print('^'); break;
            case SEXT:   print("sext"); break;
            case UEXT:   print("uext"); break;
        }
    }
    
    private void printBExpr(BExpr e) {
        {
            boolean isLeftPrimitive = e.leftSubExpr() instanceof BExpr;
            if (isLeftPrimitive)
                print('(');
            printExpr(e.leftSubExpr());
            if (isLeftPrimitive)
                print(')');
        }
        print(' ');
        printBExprOperator(e.operator());
        print(' ');
        {
            boolean isRightPrimitive = e.rightSubExpr() instanceof BExpr;
            if (isRightPrimitive)
                print('(');
            printExpr(e.rightSubExpr());
            if (isRightPrimitive)
                print(')');
        }
    }
    
    private void printUExprOperator(UExpr.Op op) {
        switch (op) {
            case BSWAP:    print("<>");   break;
            case HIGH:     print("high"); break;
            case LOW:      print("low");  break;
            case NOT:      print("~");    break;
            case CARRY:    print("C");    break;
            case NEGATIVE: print("N");    break;
            case OVERFLOW: print("V");    break;
            case ZERO:     print("Z");    break;
        }
    }
    
    private void printUExpr(UExpr e) {
        print('(');
        printUExprOperator(e.operator());
        print(' ');
        printExpr(e.subExpr());
        print(')');
    }
    
    private void printTarget(Target l) {
        print(l.targetLabel().name());
    }
    
    public void printExpr(Expr e) {
        if (e instanceof Const) {
            printConst((Const) e);
        } else if (e instanceof Lval) {
            printLval((Lval) e);
        } else if (e instanceof BExpr) {
            printBExpr((BExpr) e);
        } else if (e instanceof UExpr) {
            printUExpr((UExpr) e);
        } else {
            Assert.unreachable();
        }
    }
    
    private void printAssignStmt(AssignStmt s) {
        printLval(s.definedLval());
        if (s.isPartialAssignment()) {
            Tuple<Integer, Integer> range = s.rangeOfAssignment();
            print('<');
            print(range.first);
            print(',');
            print(range.second);
            print('>');
        }
        print(" = ");
        printExpr(s.usedRval());
        print(";");
    }
    
    private void printCallStmt(CallStmt s) {
        print("call ");
        printExpr(s.target());
        print(";");
    }
    
    private void printCmpStmt(CmpStmt s) {
        print("cmp ");
        printExpr(s.comparedFirst());
        print(", ");
        printExpr(s.comparedSecond());
        print(";");
    }
    
    private void printJmpStmt(JmpStmt s) {
        print("jmp");
        if (s.isConditional()) {
            print('[');
            s.dependentFlags().forEach(f -> print(f.toString() + ", "));
            print(']');
        }
        print(" ");
        if (s.hasResolvedTarget())
            printTarget(s.resolvedTarget());
        else
            printExpr(s.target());
        print(";");
    }
    
    private void printLdStmt(LdStmt s) {
        printLval(s.definedLval());
        print(" <- [ ");
        printExpr(s.loadFrom());
        print(" ];");
    }

    private void printStStmt(StStmt s) {
        printExpr(s.storedExpr());
        print(" -> [ ");
        printExpr(s.storeTo());
        print(" ];");
    }
    
    public void printSelectStmt(SelStmt s) {
        printLval(s.definedLval());
        print(" = ");
        printExpr(s.condition());
        print(" ? ");
        printExpr(s.truevalue());
        print(" : ");
        printExpr(s.falsevalue());
    }
    
    public void printStmt(Stmt s) {
        switch (s.kind()) {
            case ASSIGN:
                printAssignStmt((AssignStmt) s);
                break;
            case CALL:
                printCallStmt((CallStmt) s);
                break;
            case CMP:
                printCmpStmt((CmpStmt) s);
                break;
            case JMP:
                printJmpStmt((JmpStmt) s);
                break;
            case LD:
                printLdStmt((LdStmt) s);
                break;
            case RET:
                print("return;");
                break;
            case SETF:
                print("setf;");
                break;
            case ST:
                printStStmt((StStmt) s);
                break;
            case SELECT:
                printSelectStmt((SelStmt) s);
        }
    }
    
    public void printBasicBlock(BasicBlock<Stmt> bb) {
        println(bb.label() + ":");
        bb.forEach(s -> { print('\t'); printStmt(s); println(); });
    }
    
    public void printIndentedBasicBlock(BasicBlock<Stmt> bb) {
        print('\t');
        println(bb.label() + ":");
        bb.forEach(s -> { print("\t\t"); printStmt(s); println(); });
    }
    
    public void printContextWithUDInfo(Context ctx) {
        print(ctx.name());
        println(" {");
        ctx.cfg().forEach(bb -> {
            print(bb.label());
            println(":");
            bb.forEach(s -> {
                print('\t');
                printStmt(s);
                print("\t# ");
                println(s.index());
                s.usedLvals().forEach(lv -> {
                    print("\t\t# ");
                    printLval(lv);
                    print(" -- ");
                    s.searchDefFor(lv).forEach(def -> {
                        print(def.index());
                        print(", ");
                    });
                    println();
                });
            });
        });
        println('}');
    }
    
    public void printContext(Context ctx) {
        print(ctx.name());
        println(" {");
        ctx.cfg().forEach(bb -> printBasicBlock(bb));
        println('}');
    }
}

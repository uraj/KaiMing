package edu.psu.ist.plato.kaiming.x86.ir;

import java.io.OutputStream;
import java.io.PrintWriter;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.util.Assert;

public class Printer extends PrintWriter {

    public Printer(OutputStream out) {
        super(out, true);
    }
    
    public static Printer out = new Printer(System.out);
    public static Printer err = new Printer(System.err);

    public void printConst(Const c) {
        long value = c.getValue();
        if (value < 0) {
            print(String.format("-0x%x", -value));
        } else {
            print(String.format("0x%x", value));
        }
    }
    
    public void printReg(Reg r) {
        print('%');
        print(r.getRegister().id.name().toLowerCase());
    }
    
    public void printVar(Var v) {
        print("@(");
        print(v.getName());
        print(":");
        print(v.sizeInBits());
        print(")");
    }
    
    public void printFlg(Flg f) {
        print('%');
        print(f.flag.name().toUpperCase());
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
            case SUB:    print('-'); break;
            case UADD:   print("!+"); break;
            case UMUL:   print("!*"); break;
            case USUB:   print("!-"); break;
            case XOR:    print('^'); break;
        }
    }
    
    private void printBExpr(BExpr e) {
        {
            boolean isLeftPrimitive = e.getLeftSubExpr() instanceof BExpr;
            if (isLeftPrimitive)
                print('(');
            printExpr(e.getLeftSubExpr());
            if (isLeftPrimitive)
                print(')');
        }
        print(' ');
        printBExprOperator(e.getOperator());
        print(' ');
        {
            boolean isRightPrimitive = e.getRightSubExpr() instanceof BExpr;
            if (isRightPrimitive)
                print('(');
            printExpr(e.getRightSubExpr());
            if (isRightPrimitive)
                print(')');
        }
    }
    
    private void printUExprOperator(UExpr.Op op) {
        switch (op) {
            case BSWAP: print("<>"); break;
            case HIGH:  print("{"); break;
            case LOW:   print("}"); break;
            case NOT:   print("~"); break;
        }
    }
    
    private void printUExpr(UExpr e) {
        print('(');
        printUExprOperator(e.getOperator());
        print(' ');
        printExpr(e.getSubExpr());
        print(')');
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
        printLval(s.getDefinedLval());
        print(" = ");
        printExpr(s.getExpr());
        print(";");
    }
    
    private void printCallStmt(CallStmt s) {
        print("call ");
        printExpr(s.getTarget());
        print(";");
    }
    
    private void printCmpStmt(CmpStmt s) {
        print("cmp ");
        printExpr(s.getComparedFirst());
        print(", ");
        printExpr(s.getComparedSecond());
        print(";");
    }
    
    private void printJmpStmt(JmpStmt s) {
        print("jmp ");
        printExpr(s.getTarget());
        print(";");
    }
    
    private void printLdStmt(LdStmt s) {
        printLval(s.getDefinedLval());
        print(" <- [ ");
        printExpr(s.getAddr());
        print(" ];");
    }

    private void printStStmt(StStmt s) {
        printExpr(s.getContent());
        print(" -> [ ");
        printExpr(s.getAddr());
        print(" ];");
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
        }
    }
    
    public void printBasicBlock(BasicBlock<Stmt> bb) {
        println(bb.getLabel() + ":");
        bb.forEach(s -> { print('\t'); printStmt(s); println(); });
    }
    
    public void printContext(Context ctx) {
        ctx.getCFG().forEach(bb -> printBasicBlock(bb));
    }
}

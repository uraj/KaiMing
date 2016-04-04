package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.elf.Elf;
import edu.psu.ist.plato.kaiming.problem.PathInsensitiveProblem;
import edu.psu.ist.plato.kaiming.problem.UnsolvableException;
import edu.psu.ist.plato.kaiming.util.Tuple;

/**
 * A Unit is an object that holds the information of the disassembled
 * binary. Unit should be responsible for resolving the target of  
 * inter-procedural calls. 
 */
public class Unit {
    
    private Elf mElf;
    
    public Unit(Elf elf) {
        mElf = elf;
    }
    
    public Elf elf() {
        return mElf;
    }
    
    public static void UDAnalysis(Context ctx) throws UnsolvableException {
        class ReachingDefinition extends
            PathInsensitiveProblem<Stmt, Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>>> {

            public ReachingDefinition(Context ctx) {
                super(ctx, ctx.getCFG(), PathInsensitiveProblem.Direction.FORWARD);
            }

            @Override
            protected Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>>
            getInitialEntryState(BasicBlock<Stmt> bb) {
                return new HashMap<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>>();
            }

            @Override
            protected Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>>
            transfer(BasicBlock<Stmt> bb,
                    Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>> in)
                            throws UnsolvableException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            protected Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>> confluence(
                    Set<Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>>> dataset) {
                // TODO Auto-generated method stub
                return null;
            }
        }
        
        ReachingDefinition rd = new ReachingDefinition(ctx);
        rd.solve();
        Map<BasicBlock<Stmt>, Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>>>
        solution = rd.getSolution();
    }
}

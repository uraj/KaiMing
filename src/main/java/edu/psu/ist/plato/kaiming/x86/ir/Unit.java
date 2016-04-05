package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.elf.Elf;
import edu.psu.ist.plato.kaiming.problem.PathInsensitiveProblem;
import edu.psu.ist.plato.kaiming.problem.UnsolvableException;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.SetUtil;
import edu.psu.ist.plato.kaiming.x86.Register;
import edu.psu.ist.plato.kaiming.x86.Register.Id;

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
    
    public static void UDAnalysis(Context ctx) {
        class ReachingDefinition extends
            PathInsensitiveProblem<Stmt, Map<Lval, Set<DefStmt>>> {
            
            public ReachingDefinition(Context ctx) {
                super(ctx, ctx.cfg(), PathInsensitiveProblem.Direction.FORWARD);
            }
            
            @Override
            protected Map<Lval, Set<DefStmt>>
            getInitialEntryState(BasicBlock<Stmt> bb) {
                Map<Lval, Set<DefStmt>> ret = new HashMap<Lval, Set<DefStmt>>();
                // If a block is the entry, all registers used at the starting point
                // are defined externally 
                if (mCfg.entries() == bb) {
                    Set<DefStmt> set = new HashSet<DefStmt>();
                    set.add(DefStmt.External);
                    for (Id id : Register.Id.values()) {
                        ret.put(Reg.getReg(Register.getRegister(id)), set);
                    }
                }
                return ret;
            }

            @Override
            protected Map<Lval, Set<DefStmt>>
            transfer(BasicBlock<Stmt> bb, Map<Lval, Set<DefStmt>> in)
                    throws UnsolvableException {
                Map<Lval, Set<DefStmt>> out = new HashMap<Lval, Set<DefStmt>>(in);
                for (Stmt s : bb) {
                    for (Lval lv : s.getUsedLvals()) {
                        s.setDef(lv, new HashSet<DefStmt>(out.get(lv)));
                    }
                    
                    if (s instanceof DefStmt) {
                        DefStmt defstmt = (DefStmt)s;
                        Set<DefStmt> defset = new HashSet<DefStmt>();
                        defset.add(defstmt);
                        out.put(defstmt.getDefinedLval(), defset);
                    }
                }
                return out;
            }

            @Override
            protected Map<Lval, Set<DefStmt>>
            confluence(Set<Map<Lval, Set<DefStmt>>> dataset) {
                Map<Lval, Set<DefStmt>> out = new HashMap<Lval, Set<DefStmt>>();
                dataset.forEach(map -> map.forEach(
                        (k, v) -> out.merge(k, v, SetUtil::union)));
                return out;
            }
        }
        
        ReachingDefinition rd = new ReachingDefinition(ctx);
        try {
            rd.solve();
        } catch (UnsolvableException e) {
            Assert.unreachable();
        }

        // Set def-use chains given use-def chains
        for (Stmt s : ctx.entries()) {
            for (Lval lv : s.getUsedLvals()) {
                for (DefStmt defs : s.getDef(lv)) {
                    if (!defs.isExternal()) {
                        defs.addToDefUseChain(s);
                    }
                }
            }
        }
    }
}

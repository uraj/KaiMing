package edu.psu.ist.plato.kaiming.ir;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.MachRegister;
import edu.psu.ist.plato.kaiming.elf.Elf;
import edu.psu.ist.plato.kaiming.problem.PathInsensitiveProblem;
import edu.psu.ist.plato.kaiming.problem.UnsolvableException;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.SetUtil;
//import edu.psu.ist.plato.kaiming.x86.Register;
//import edu.psu.ist.plato.kaiming.x86.Register.Id;

/**
 * A Unit is an object that holds the information of the disassembled
 * binary. Unit should be responsible for resolving the target of  
 * inter-procedural calls. 
 */
public class AssemblyUnit {
    
    private Elf mElf;
    
    public AssemblyUnit(Elf elf) {
        mElf = elf;
    }
    
    public Elf elf() {
        return mElf;
    }
    
    public static void UDAnalysis(Context ctx) {
        class ReachingDefinition extends
            PathInsensitiveProblem<Stmt, Map<Lval, Set<DefStmt>>> {
            
            private Machine mMach;
            
            public ReachingDefinition(Context ctx) {
                super(ctx, ctx.cfg(), Direction.FORWARD);
                mMach = ctx.mach();
            }
            
            @Override
            protected Map<Lval, Set<DefStmt>>
            getInitialEntryState(BasicBlock<Stmt> bb) {
                Map<Lval, Set<DefStmt>> ret = new HashMap<Lval, Set<DefStmt>>();
                // If a block is the entry, all registers used at the starting point
                // are defined externally 
                if (mCfg.entryBlock() == bb) {
                    Set<DefStmt> set = new HashSet<DefStmt>();
                    set.add(DefStmt.EXTERNAL);
                    for (MachRegister mreg : mMach.registers()) {
                        ret.put(Reg.get(mreg), set);
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
                    for (Lval lv : s.usedLvals()) {
                        Lval key;
                        if (lv instanceof Reg && !out.containsKey(lv)) {
                            key = ((Reg)lv).containingReg();
                        } else {
                            key = lv;
                        }
                        if (!out.containsKey(key)) {
                            out.put(key, new HashSet<DefStmt>());
                        }
                        s.updateDefFor(lv, out.get(key));
                    }
                    
                    if (s instanceof DefStmt) {
                        DefStmt defstmt = (DefStmt)s;
                        Set<DefStmt> defset = new HashSet<DefStmt>();
                        defset.add(defstmt);
                        Lval definedLval = defstmt.definedLval();
                        out.put(definedLval, defset);
                        // If the defined lvalue is a register, then registers overlapping this
                        // one also get defined.
                        if (definedLval instanceof Reg) {
                            MachRegister definedReg = ((Reg)definedLval).machRegister();
                            for (MachRegister mr : definedReg.subsumedRegisters()) {
                                Reg irr = Reg.get(mr);
                                out.put(irr, defset);
                            }
                            out.put(Reg.get(definedReg.containingRegister()), defset);
                        }
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
            for (Lval lv : s.usedLvals()) {
                for (DefStmt defs : s.searchDefFor(lv)) {
                    if (!defs.isExternal()) {
                        defs.addToDefUseChain(s);
                    }
                }
            }
        }
    }
}

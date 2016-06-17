package edu.psu.ist.plato.kaiming.arm64;

import java.util.List;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.Procedure;

public class Function extends Procedure<Instruction> {
    
    public Function(Label label, List<Instruction> entries) {
        
    }

    @Override
    public CFG<Instruction> cfg() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Label deriveSubLabel(BasicBlock<Instruction> bb) {
        // TODO Auto-generated method stub
        return null;
    }

}

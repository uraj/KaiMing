package edu.psu.ist.plato.kaiming.arm64;

import java.util.List;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.util.Assert;

public class Function extends Procedure<Instruction> {
    
    private CFG<Instruction> mCFG;
    private int mSubLabelCount;
    private static final String sSubLabelSuffix = "_sub";
    
    public Function(Label label, List<Instruction> insts) {
        mLabel = label;
        mCFG = buildCFG(insts);
        mSubLabelCount = 0;
    }

    @Override
    public CFG<Instruction> cfg() {
        return mCFG;
    }

    @Override
    public String name() {
        return mLabel.name();
    }

    @Override
    protected Label deriveSubLabel(BasicBlock<Instruction> bb) {
        Assert.verify(mLabel != null);
        Assert.verify(mLabel.name() != null);
        String name = mLabel.name() + sSubLabelSuffix
                + String.valueOf(mSubLabelCount++);
        Label ret = new Label(name, 0);
        bb.firstEntry().fillLabelInformation(ret);
        return ret;
    }

}

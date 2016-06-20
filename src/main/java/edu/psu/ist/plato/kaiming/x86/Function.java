package edu.psu.ist.plato.kaiming.x86;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
    public Label label() {
        return mLabel;
    }
    
    @Override
    public CFG<Instruction> cfg() {
        return mCFG;
    }
    
    public void setEntries(List<Instruction> entries) {
        mCFG = buildCFG(entries);
    }
    
    @Override
    public String name() {
        return mLabel.name();
    }

    @Override
    public Label deriveSubLabel(BasicBlock<Instruction> bb) {
        Assert.verify(mLabel != null);
        Assert.verify(mLabel.name() != null);
        String name = mLabel.name() + sSubLabelSuffix
                + String.valueOf(mSubLabelCount++);
        Label ret = new Label(name, 0);
        bb.firstEntry().fillLabelInformation(ret);
        return ret;
    }
    
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printFunction(this);
        p.close();
        return baos.toString();
    }
}

package edu.psu.ist.plato.kaiming.ir;

public class Var extends Lval {
    private String mName;
    private Context mContext;
    private int mSize;
    
    public Var(Context context, String name) {
        mName = name;
        mContext = context;
        mSize = context.mach().wordSizeInBits();
    }
    
    public Var(Context context, String name, int sizeInBits) {
        mName = name;
        mContext = context;
        mSize = sizeInBits;
    }
    
    public String name() {
        return mName;
    }
    
    public Context context() {
        return mContext;
    }

    @Override
    public final boolean equals(Object lv) {
        if (lv instanceof Var) {
            Var v = (Var)lv;
            return v.mContext == mContext && v.mName.equals(mName);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return mContext.hashCode() * 31 + mName.hashCode();
    }

    @Override
    public final int sizeInBits() {
        return mSize;
    }
}

package edu.psu.ist.plato.kaiming.x86.ir;

public class Var extends Lval {
    private String mName;
    private Context mContext;
    private int mSize;
    
    public Var(Context context, String name) {
        mName = name;
        mContext = context;
        mSize = 32;
    }
    
    public Var(Context context, String name, int sizeInBits) {
        mName = name;
        mContext = context;
        mSize = sizeInBits;
    }
    
    public String getName() {
        return mName;
    }
    
    public Context getContext() {
        return mContext;
    }

    @Override
    public boolean equals(Object lv) {
        if (lv instanceof Var) {
            return ((Var)lv).mName.equals(mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public int sizeInBits() {
        return mSize;
    }
}

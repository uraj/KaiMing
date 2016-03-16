package edu.psu.ist.plato.kaiming.x86.ir;

public class Var extends Lval {
    private String mName;
    private Context mContext;
    
    public Var(Context context, String name) {
        mName = name;
        mContext = context;
    }
    
    public String getName() {
        return mName;
    }
    
    public Context getContext() {
        return mContext;
    }
}

package edu.psu.ist.plato.kaiming.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<E> implements Iterator<E> {
    private int mCursor;
    private E[] mArray;

    public ArrayIterator(E[] operands) {
        mCursor = 0;
        mArray = operands;
    }

    @Override
    public boolean hasNext() {
        return mCursor < mArray.length;
    }

    @Override
    public E next() {
        return mArray[mCursor++];
    }

    @Override
    public void remove() {
        throw new NoSuchElementException(
                "You can not remove operand from an instruction.");
    }
}

package edu.psu.ist.plato.kaiming.util;

public interface ReverseIterator<T> {
    public boolean hasPrevious();
    public T previous();
    void remove();
}

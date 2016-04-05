package edu.psu.ist.plato.kaiming;

import java.util.Comparator;

public abstract class Entry implements Comparable<Long> {
    public abstract long index();

    @Override
    public final int compareTo(Long l) {
        return (int) (index() - l);
    }
    
    public static Comparator<Entry> comparator = new Comparator<Entry> () {
        @Override
        public final int compare(Entry e1, Entry e2) {
            return Long.signum(e1.index() - e2.index());
        }
    };
    
    public static int searchIndex(final Entry[] entries, long index) {
        int left = 0, right = entries.length - 1;
        while (left <= right) {
            int pivot = (right + left) / 2;
            int ret = entries[pivot].compareTo(index);
            if (ret == 0)
                return pivot;
            else if (ret < 0) {
                left = pivot + 1;
            } else {
                right = pivot - 1;
            }
        }
        return -1;
    }
    
    public static Entry search(Iterable<Entry> entries, long index) {
        for (Entry e : entries) {
            if (e.index() == index)
                return e;
        }
        return null;
    }
}

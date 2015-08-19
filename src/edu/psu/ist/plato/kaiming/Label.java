package edu.psu.ist.plato.kaiming;

public abstract class Label {
	
	public abstract String toString();
	
	public abstract String getName();
	
	public boolean equals(Label l) {
	    return l.getName() == getName();
	}
}

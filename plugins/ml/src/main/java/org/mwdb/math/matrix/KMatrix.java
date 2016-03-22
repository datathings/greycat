package org.mwdb.math.matrix;

public interface KMatrix {

    byte matrixType();


    int rows();

    int columns();

    double get(int rowIndex, int columnIndex);

    double set(int rowIndex, int columnIndex, double value);

    double add(int rowIndex, int columnIndex, double value);

    void setAll(double value);

    double getAtIndex(int index);

    double setAtIndex(int index, double value);

    double addAtIndex(int index, double value);

    KMatrix clone();

    double[] data();

    void setData(double[] data);



}

package com.medion.project_icescream403;

/**
 * Created by Medion on 2015/8/13.
 */
public class ScaleWeight {
    private boolean isPositive;
    private Double value;

    public ScaleWeight() {}


    public Double getWeight() {
        if (isPositive)
            return value;
        else
            return -1 * value;
    }

    public void setPositive(boolean flag) {
        isPositive = flag;
    }

    public void setWeightValue(Double value) {
        this.value = value;
    }

    public String toString() {
        return "Evaluated Net Weight: " + String.valueOf(getWeight());
    }
}

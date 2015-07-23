package com.medion.project_icescream403;

/**
 * Created by Medion on 2015/7/23.
 */
public class Mix {

    private String id;
    private String name;
    private double weight;

    public Mix(String id, String name, double weight) {
        this.id = id;
        this.name = name;
        this.weight = weight;
    }

    public String toString() {
        return id + " " + name + " " + weight;
    }
}

package com.medion.project_icescream403;

/**
 * Created by Medion on 2015/7/23.
 */
public class Recipe {

    private String ingredientID;
    private String ingredientName;
    private String productID;
    private String productName;
    private double weight;
    private String weightUnit;

    public Recipe(String ingredientID, String ingredientName, String productID, String productName, double weight, String weightUnit) {
        this.ingredientID = ingredientID;
        this.ingredientName = ingredientName;
        this.productID = productID;
        this.productName = productName;
        this.weight = weight;
        this.weightUnit = weightUnit;
    }

    public String toString() {
        return ingredientID + " " + ingredientName + " " + productID + " " + productName + " " + weight + " " + weightUnit;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getProductID() {
        return productID;
    }

    public String getProductName() {
        return productName;
    }

    public double getWeight() {
        return weight;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

}

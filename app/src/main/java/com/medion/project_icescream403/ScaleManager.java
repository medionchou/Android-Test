package com.medion.project_icescream403;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Medion-PC on 2016/2/16.
 */

public class ScaleManager implements Runnable {

    private ScaleInterface[] scales;
    private int scaleCount; //how many scales are connected
    private int scaleIndex; //index of scale
    private int cachedScaleIndex; //
    private int recipeIndex; // recipe entry in a recipe list
    private int pdaState; // pda status
    private boolean stop = false; // indicate terminatio
    private MainActivity activity;
    private ProgressDialog dialog;
    private ProgressDialog pdaDialog;
    private Client client;
    private List<String> precision;
    private List<List<Recipe>> recipeGroup;
    private MediaPlayer alarmAudio;
    private Button confirmButton;
    private Button nextButton;

    public ScaleManager(ScaleInterface[] scales, MainActivity activity) {
        this.scales = scales;
        scaleCount = scales.length;
        scaleIndex = 0;
        cachedScaleIndex = 0;
        recipeIndex = 0;
        this.activity = activity;
        dialog = activity.getDialog();
        pdaDialog = activity.getPdaDialog();
        client = activity.getClient();
        alarmAudio = MediaPlayer.create(activity, R.raw.alarm);
        confirmButton = (Button) activity.findViewById(R.id.confirm);
        nextButton = (Button) activity.findViewById(R.id.nextBatch);

    }

    @Override
    public void run() {
        final TextView recipeIDView = (TextView) activity.findViewById(R.id.recipeID_text_view);
        final TextView productIDView = (TextView) activity.findViewById(R.id.productID_text_view);
        final TextView productWeightView = (TextView) activity.findViewById(R.id.product_weight_text_view);
        final TextView scaleWeightView = (TextView) activity.findViewById(R.id.scale_weight_text_view);
        final TextView biasView = (TextView) activity.findViewById(R.id.precision_text_view);

        while (!stop) {
            try {
                if (stop) break;

                if (!scales[scaleIndex].isDataPrepared()) continue;
                if (dialog.isShowing() || activity.getGlobalState() != States.PDA_CONNECTED) {
                    Thread.sleep(1000);
                    continue;
                }

                Thread.sleep(1000);
                if (client == null) break;

                String serialNum = client.getSerialNumber();
                final String ingredientName;
                final String productName;
                final String productWeightText;
                final String productID;
                final boolean enabled;
                final double scaleWeight;
                final String bias;
                int range = -1;

                if (cachedScaleIndex != scaleIndex) {
                    cachedScaleIndex = scaleIndex;
                    scaleWeight = 0;
                } else {
                    scaleWeight = scales[scaleIndex].getScaleWeight().getWeight();
                }

                if (recipeGroup != null) {
                    if (recipeGroup.size() > 0) {

                        List<Recipe> recipeList = recipeGroup.get(0);
                        Recipe recipe = recipeList.get(recipeIndex);
                        double productWeight = recipe.getWeight();

                        ingredientName = recipe.getIngredientName();
                        productName = recipe.getProductName();
                        productWeightText = String.valueOf(productWeight) + " " + recipe.getWeightUnit();
                        productID = recipe.getProductID();
                        //serialNum = client.getSerialNumber();
                        bias = String.valueOf(Double.valueOf(precision.get(getRange(productWeight))) / 1000.0)  + " KG";

                        if (serialNum.equals("")) {
                                /*
                                    While recipe is empty, and then receiving a new recipe input from server. After that, invoke progressDialog to ask
                                    PDA scan barcode.
                                 */
                            pdaState = States.PDA_SCANNING;
                            client.setSerialNumber("Unchecked");
                        } else if (productID.equals(serialNum)) {
                                /*
                                    PDA query match the specified serial Number
                                 */
                            pdaState = States.PDA_SCANNING_CORRECT;
                            client.setSerialNumber("Unchecked");
                            client.setCmd("QUERY_REPLY\tOK<END>");
                        } else if (!productID.equals(serialNum) && !serialNum.equals("Unchecked")) {
                                /*
                                    Send wrong command if worker scan incorrect barcode.
                                 */
                            pdaState = States.PDA_SCANNING;
                            client.setSerialNumber("Unchecked");
                            client.setCmd("QUERY_REPLY\tWRONG<END>");
                        }

                        range = getRange(productWeight);

                        if (range >= 0) {
                            if (Math.abs(productWeight - scaleWeight) < (Double.valueOf(precision.get(range)) / 1000.0)) {
                                enabled = true;
                            } else {
                                enabled = false;
                            }
                        } else {
                            if (Math.abs(productWeight - scaleWeight) < 0.1)
                                enabled = true;
                            else
                                enabled = false;
                        }

                    } else {
                        if (!serialNum.equals("")) {
                                /*
                                    Send empty command if no recipe contain here.
                                */
                            client.setCmd("QUERY_REPLY\tEMPTY<END>");
                            client.setSerialNumber("");
                        }
                        pdaState = States.PDA_NO_INPUT_DATA;
                        ingredientName = activity.getString(R.string.no_data);
                        productName = activity.getString(R.string.no_data);
                        productWeightText = activity.getString(R.string.no_data);
                        productID = activity.getString(R.string.no_data);
                        bias = "0.1 KG";
                        enabled = false;
                    }

                } else {
                    if (!serialNum.equals("")) {
                            /*
                                Send empty command if no recipe contain here.
                             */
                        client.setCmd("QUERY_REPLY\tEMPTY<END>");
                        client.setSerialNumber("");
                    }

                    pdaState = States.PDA_NO_INPUT_DATA;
                    ingredientName = activity.getString(R.string.no_data);
                    productName = activity.getString(R.string.no_data);
                    productWeightText = activity.getString(R.string.no_data);
                    productID = activity.getString(R.string.no_data);
                    bias = "0.1 KG";
                    enabled = false;
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (pdaState == States.PDA_SCANNING) {
                            pdaDialog.setTitle(activity.setDialogText(activity.getString(R.string.pda_progress_status), 1));
                            pdaDialog.setMessage(activity.setDialogText(productID + " " + productName, 3));
                            pdaDialog.show();
                            pdaDialog.setCancelable(false);
                            pdaState = States.PDA_IDLING;
                            Log.v("MyLog", "Waiting PDA Scanning");
                        } else if (pdaState == States.PDA_SCANNING_CORRECT) {
                            if (pdaDialog.isShowing())
                                pdaDialog.dismiss();
                        }

                        if (!productWeightText.equals("尚無配料資料") && recipeGroup.size() > 0) {
                            List<Recipe> recipeList = recipeGroup.get(0);
                            Recipe recipe = recipeList.get(recipeIndex);

                            if ((scaleWeight / recipe.getWeight()) >= 0.9) {
                                if (!alarmAudio.isPlaying())
                                    alarmAudio.start();
                            }
                        }

                        recipeIDView.setText(ingredientName);
                        productIDView.setText(productName);
                        productWeightView.setText(productWeightText);
                        scaleWeightView.setText(String.valueOf(scaleWeight) + " KG");
                        biasView.setText(bias);
                        if (enabled)
                            scaleWeightView.setTextColor(Color.GREEN);
                        else
                            scaleWeightView.setTextColor(Color.RED);

                        confirmButton.setEnabled(true); // enabled is set to true
                    }
                });


            } catch (Exception e) {
                Log.e("MyLog", "ScaleManager " + e.toString());
                com.medion.project_icescream403.Log.getRequest("<b><font size=\"5\" color=\"red\">Caught Exception in ScaleManager:</font></b>" + e.toString());
            }
        }
    }

    public void setPdaState(int state) {
        pdaState = state;
    }

    public void setScaleIndex(int val) {
        cachedScaleIndex = scaleIndex;
        scaleIndex = val;
    }

    public int getScaleIndex() {
        return scaleIndex;
    }

    public void setRecipeIndex(int val) {
        recipeIndex = val;
    }

    public int getRecipeIndex() {
        return recipeIndex;
    }

    public int getScaleCount() {
        return scaleCount;
    }

    public void terminate() {
        stop = true;
    }

    public void setPrecision(List<String> precision) {
        this.precision = precision;
    }

    public void setRecipeGroup(List<List<Recipe>> recipeGroup) {
        this.recipeGroup = recipeGroup;
    }

    public int getRange(double weight) {
        if (weight >= 0 && weight < 5) {
            return 0;
        } else if (weight >= 5 && weight < 10) {
            return 1;
        } else if (weight >= 10 && weight < 20) {
            return 2;
        } else if (weight >= 20 && weight <= 30) {
            return 3;
        }
        return -1;
    }
}
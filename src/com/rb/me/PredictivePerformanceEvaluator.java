package com.rb.me;

import java.math.RoundingMode;

public class PredictivePerformanceEvaluator
{
    //correctly classified positives
    private double truePositives;
    //correctly classified negatives
    private double trueNegatives;
    //incorrectly classified negatives
    private double falsePositives; // classifier classified as positive, but in reality negative
    //incorrectly classified positives
    private double falseNegatives; // classifier classified as negative, but in reality positive
    private String[] positiveNegativeClassLabels;


    //constructor accepts confusion matrix
    public PredictivePerformanceEvaluator(double truePositives, double falseNegatives, double falsePositives, double trueNegatives, String[] positiveNegativeClassLabels)
    {
        // true positives
        this.truePositives = truePositives;

        // true negatives at index 3
        this.trueNegatives = trueNegatives;

        // false positives at index 2
        this.falsePositives = falsePositives;

        // false negatives at at index 1
        this.falseNegatives = falseNegatives;

        this.positiveNegativeClassLabels = positiveNegativeClassLabels;

		/*System.out.println();
		System.out.println("true pos: " + truePositives);
		System.out.println("false pos: " + falsePositives);
		System.out.println("false neg: " + falseNegatives);
		System.out.println("true neg: " + trueNegatives);*/
    } // Evaluator

    //helper method for precision
    public double precision()
    {
        //precision is tp / (tp + fp)
        double precision = truePositives / (truePositives + falsePositives);

        return Utils.format("#.##", RoundingMode.HALF_UP,
                 precision);
    } // precision

    //recall
    public double recall()
    {
        //recall is tp / (tp + fp)
        double recall = truePositives / (truePositives + falseNegatives);

        return recall;
        //return Utils.format("#.####", RoundingMode.HALF_UP,
        //         recall);
    } // recall

    //F1 - score
    public double fMeasure()
    {
        //fmeasure is 2 * precision * recall / (precision + recall)
        double fMeasure = 2 * precision() * recall() / (precision() + recall());

        return Utils.format("#.####", RoundingMode.HALF_UP,
                 fMeasure);
    } // fMeasure

    //accuracy
    public double accuracy()
    {
        //accuary is (tp + tn) / (tp + tn + fp + fn)
        double accuracy = (truePositives + trueNegatives)
                / (truePositives + trueNegatives + falsePositives + falseNegatives);

        return accuracy;
        //return Utils.format("#.####", RoundingMode.HALF_UP,
        //         accuracy);
    } // accuracy


    //TP FN
    //FP TN
    public String confusionMatrix()
    {
        int defaultPaddingLength
                = Utils.defaultPaddingLength(truePositives + "", falsePositives + "", falseNegatives + "", trueNegatives + "");
        int defaultPaddingForClassLabels = Utils.defaultPaddingLength(positiveNegativeClassLabels) + 2; //for ""
        StringBuilder sb = new StringBuilder(Utils.padLeftSpaces("  ", defaultPaddingForClassLabels));
        sb.append("  predicted").append("\n");
        sb.append(Utils.padLeftSpaces("  ", defaultPaddingForClassLabels));
        sb.append(Utils.padLeftSpaces("\"" + positiveNegativeClassLabels[0] + "\"" + "  ", defaultPaddingLength));
        sb.append(Utils.padLeftSpaces("\"" + positiveNegativeClassLabels[1] + "\"" + "  ", defaultPaddingLength)).append("\n");

        sb.append(Utils.padLeftSpaces("\"" + positiveNegativeClassLabels[0] + "\"" + "  ", defaultPaddingForClassLabels));
        sb.append(Utils.padLeftSpaces(truePositives + "  ", defaultPaddingLength));
        sb.append(Utils.padLeftSpaces(falseNegatives + "  ", defaultPaddingLength)).append("\n");

        sb.append(Utils.padLeftSpaces("\"" + positiveNegativeClassLabels[1] + "\"" + "  ", defaultPaddingForClassLabels));
        sb.append(Utils.padLeftSpaces(falsePositives + "  ", defaultPaddingLength));
        sb.append(Utils.padLeftSpaces(trueNegatives + "  ", defaultPaddingLength)).append("\n");

        return sb.toString();
    } // confusionMatrix

} // class PredictivePerformanceEvaluator

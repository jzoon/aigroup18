package ai2018.group18;

import genius.core.Bid;
import genius.core.issue.*;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class UtilityFunctionEstimate {
    private List<Bid> rankingList;
    private List<Integer> issueNumbers;
    private List<Issue> issuesInThisDomain;
    private Map<Integer, Map<String, Double>> valueWeights;
    private Map<Integer, Double> issueWeights;

    /**
     * Constructor that estimates the value and issue weights given the ranked list of bids
     * @param utilitySpaceEstimate
     * @param rankingList
     */
    public UtilityFunctionEstimate(AdditiveUtilitySpace utilitySpaceEstimate, List<Bid> rankingList) {
        // get ranked list of bids
        this.rankingList = rankingList;

        // get list of issue numbers and list of issues
        issueNumbers = new ArrayList<>();
        issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();
        for (Issue issue : issuesInThisDomain) {
            issueNumbers.add(issue.getNumber());
        }

        // estimate value and issue weights
        valueWeights = estimateValueWeights();
        issueWeights = estimateIssueWeights();
    }

    /**
     * Estimate value weights with linearly spaced vector from 0 to 1
     * @return normalized value weights matrix
     */
    private Map<Integer, Map<String, Double>> estimateValueWeights() {
        // initialize linearly spaced vector and an empty matrix:
        // Map<issueNumber, Map<valueString, 0.0>>
        List<Double> linearUtility = linspace(0.0, 1.0, rankingList.size());
        Map<Integer, Map<String, Double>> valueWeights = initializeMatrix();

        // compute matrix with weighted frequency analysis:
        // Map<issueNumber, Map<valueString, weightedFrequency>>
        computeWeightedFrequency(valueWeights, linearUtility);

        // normalize issue columns by dividing by the max of each column
        for (int issueNumber : issueNumbers) {
            Map<String, Double> issueValues = valueWeights.get(issueNumber);
            double max = Collections.max(issueValues.values());

            for (Map.Entry<String, Double> entry : issueValues.entrySet()) {
                issueValues.put(entry.getKey(), entry.getValue() / max);
            }
            valueWeights.put(issueNumber, issueValues);
        }

        return valueWeights;
    }

    /**
     * Estimate issue weights with linearly spaced vector from -1 to 1
     * @return normalized issue weights vector
     */
    private Map<Integer, Double> estimateIssueWeights() {
        // initialize linearly spaced vector and an empty matrix:
        // Map<issueNumber, Map<valueString, 0.0>>
        List<Double> linearUtility = linspace(-1.0, 1.0, rankingList.size());
        Map<Integer, Map<String, Double>> matrix = initializeMatrix();

        // compute matrix with weighted frequency analysis:
        // Map<issueNumber, Map<valueString, weightedFrequency>>
        computeWeightedFrequency(matrix, linearUtility);

        // get issue weights vector by taking the max of each issue column:
        // Map<issueNumber, weightedFrequency>
        Map<Integer, Double> issueWeights = new HashMap<>();
        double sumIssueWeights = 0.0;
        for (int issueNumber : issueNumbers) {
            Map<String, Double> issueValues = matrix.get(issueNumber);
            double max = Collections.max(issueValues.values());
            issueWeights.put(issueNumber, max);
            sumIssueWeights += max;
        }

        // normalize issue weights vector by dividing by the sum of the vector
        for (Map.Entry<Integer, Double> entry : issueWeights.entrySet()) {
            issueWeights.put(entry.getKey(), entry.getValue() / sumIssueWeights);
        }

        return issueWeights;
    }


    /**
     * Initialize empty matrix
     * @return Map<issueNumber, Map<valueString, zero>>
     */
    private Map<Integer, Map<String, Double>> initializeMatrix() {
        Map<Integer, Map<String, Double>> matrix = new HashMap<>();

        // create column for each issue
        for (Issue issue : issuesInThisDomain) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;

            // fill column with zeros
            Map<String, Double> issueValues = new HashMap<>();
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                issueValues.put(valueDiscrete.getValue(), 0.0);
            }
            matrix.put(issue.getNumber(), issueValues);
        }

        return matrix;
    }

    /**
     * Compute matrix with weighted frequency analysis: Map<issueNumber, Map<valueString, weightedFrequency>>
     * @param matrix
     * @param linearUtility
     */
    private void computeWeightedFrequency(Map<Integer, Map<String, Double>> matrix, List<Double> linearUtility) {
        // frequency analysis of every bid i from the ranked list
        for (int i = 0; i < rankingList.size(); i++) {
            Map<Integer, Value> bidValues = rankingList.get(i).getValues();

            // for every issue add linearly spaced utility u_i to the matrix
            for (Issue issue : issuesInThisDomain) {
                int issueNumber = issue.getNumber();
                ValueDiscrete valueDiscrete = (ValueDiscrete) bidValues.get(issueNumber);

                // add u_i to the corresponding issue-value in the matrix
                Map<String, Double> values = matrix.get(issueNumber);
                double currentValue = values.get(valueDiscrete.getValue());
                double newValue = currentValue + linearUtility.get(i);
                values.replace(valueDiscrete.getValue(), newValue);
                matrix.replace(issueNumber, values);
            }
        }
    }

    /**
     * Generate linearly spaced vector
     * @param start
     * @param end
     * @param n
     * @return
     */
    private List<Double> linspace(double start, double end, int n) {
        List<Double> vector = new ArrayList<>();
        double step = (end - start) / n;

        double current = start;
        for (int i = 0; i < n; i++) {
            vector.add(current + i * step);
        }
        assert vector.size() == n;
        assert vector.get(0) == start;
        assert vector.get(n - 1) == end;

        return vector;
    }

    /**
     * Estimate utility of the bid
     * @param bid
     * @return
     */
    public Double getUtilityEstimate(Bid bid) {
        double utility = 0.0;

        Map<Integer, Value> bidValues = bid.getValues();
        for (Issue issue : issuesInThisDomain) {
            int issueNumber = issue.getNumber();
            ValueDiscrete valueDiscrete = (ValueDiscrete) bidValues.get(issueNumber);

            // add utility contribution for this issue
            double issueWeight = issueWeights.get(issueNumber);
            double valueWeight = valueWeights.get(issueNumber).get(valueDiscrete.getValue());
            utility += issueWeight * valueWeight;
        }

        return utility;
    }
}

package ai2018.group18;

import genius.core.Bid;
import genius.core.issue.*;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class UtilityFunctionEstimate {
    private AdditiveUtilitySpace utilitySpaceEstimate;
    private List<Bid> rankingList;
    private List<Integer> issueNumbers;
    private Map<Integer, Map<String, Double>> valueWeights;
    private Map<Integer, Double> issueWeights;

    public UtilityFunctionEstimate(AdditiveUtilitySpace utilitySpaceEstimate, List<Bid> rankingList) {
        this.utilitySpaceEstimate = utilitySpaceEstimate;
        this.rankingList = rankingList;

        // get list of issue numbers
        issueNumbers = new ArrayList<>();
        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();
        for (Issue issue : issuesInThisDomain) {
            issueNumbers.add(issue.getNumber());
        }

        valueWeights = estimateValueWeights();
        issueWeights = estimateIssueWeights();
    }

    /**
     * Estimate value weights
     */
    private Map<Integer, Map<String, Double>> estimateValueWeights() {
        Map<Integer, Map<String, Double>> valueWeights = initializeMatrix(); // Map<issueNumber, Map<valueNumber, Value>>
        List<Double> linearUtility = linspace(0.0, 1.0, rankingList.size());

        // weighted frequency
        weightedFrequency(valueWeights, linearUtility);

        // normalize by dividing by the max of each issue
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
     * Estimate issue weights
     */
    private Map<Integer, Double> estimateIssueWeights() {
        Map<Integer, Map<String, Double>> matrix = initializeMatrix(); // Map<issueNumber, Map<valueNumber, Value>>
        List<Double> linearUtility = linspace(-1.0, 1.0, rankingList.size());

        // weighted frequency
        weightedFrequency(matrix, linearUtility);

        // get issue weights by taking the max of each issue
        Map<Integer, Double> issueWeights = new HashMap<>();
        double sumIssueWeights = 0.0;
        for (int issueNumber : issueNumbers) {
            Map<String, Double> issueValues = matrix.get(issueNumber);
            double max = Collections.max(issueValues.values());
            issueWeights.put(issueNumber, max);
            sumIssueWeights += max;
        }

        // normalize by dividing by the max of issue weights
        for (Map.Entry<Integer, Double> entry : issueWeights.entrySet()) {
            issueWeights.put(entry.getKey(), entry.getValue() / sumIssueWeights);
        }

        return issueWeights;
    }


    /**
     * Initialize matrix
     * @return Map<issueNumber, Map<valueNumber, zero>>
     */
    private Map<Integer, Map<String, Double>> initializeMatrix() {
        Map<Integer, Map<String, Double>> matrix = new HashMap<>();

        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();
        for (Issue issue : issuesInThisDomain) {
            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            Map<String, Double> issueValues = new HashMap<>();
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                issueValues.put(valueDiscrete.getValue(), 0.0);
            }
            matrix.put(issueNumber, issueValues);
        }

        return matrix;
    }

    /**
     * Get weighted frequency from ranking list
     * @param matrix
     * @param linearUtility
     */
    private void weightedFrequency(Map<Integer, Map<String, Double>> matrix, List<Double> linearUtility) {
        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();

        for (int i = 0; i < rankingList.size(); i++) {
            Map<Integer, Value> bidValues = rankingList.get(i).getValues();
            for (Issue issue : issuesInThisDomain) {
                int issueNumber = issue.getNumber();
                ValueDiscrete valueDiscrete = (ValueDiscrete) bidValues.get(issueNumber);

                // add u_i
                Map<String, Double> values = matrix.get(issueNumber);
                double currentValue = values.get(valueDiscrete.getValue());
                values.replace(valueDiscrete.getValue(), currentValue + linearUtility.get(i));
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

        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();
        Map<Integer, Value> bidValues = bid.getValues();
        for (Issue issue : issuesInThisDomain) {
            int issueNumber = issue.getNumber();
            // get value of this issue for the bid
            ValueDiscrete valueDiscrete = (ValueDiscrete) bidValues.get(issueNumber);

            // add utility contribution for this issue
            utility += issueWeights.get(issueNumber) * valueWeights.get(issueNumber).get(valueDiscrete.getValue());
        }

        return utility;
    }
}

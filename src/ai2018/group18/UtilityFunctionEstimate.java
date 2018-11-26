package ai2018.group18;

import genius.core.Bid;
import genius.core.issue.*;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilityFunctionEstimate {
    private AdditiveUtilitySpace utilitySpaceEstimate;
    private List<Bid> rankingList;
    private Map<Integer, Map<Integer, Double>> matrix; // Map<issueNumber, Map<valueNumber, Value>>
    private List<Integer> issueNumbers;

    public UtilityFunctionEstimate(AdditiveUtilitySpace utilitySpaceEstimate, List<Bid> rankingList) {
        this.utilitySpaceEstimate = utilitySpaceEstimate;
        this.rankingList = rankingList;
        this.matrix = initializeMatrix();

        // get list of issue numbers
        issueNumbers = new ArrayList<>();
        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();
        for (Issue issue : issuesInThisDomain) {
            issueNumbers.add(issue.getNumber());
        }
    }

    private Map<Integer, Map<Integer, Double>> initializeMatrix() {
        Map<Integer, Map<Integer, Double>> matrix = new HashMap<>();

        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();
        for (Issue issue : issuesInThisDomain) {
            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) utilitySpaceEstimate.getEvaluator(issueNumber);
            Map<Integer, Double> issueValues = new HashMap<>();
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                int value = evaluatorDiscrete.getValue(valueDiscrete);
                issueValues.put(value, 0.0);
            }
            matrix.put(issueNumber, issueValues);
        }

        return matrix;
    }

    public void estimateValueWeights(List<Bid> bidOrder) {
        List<Issue> issuesInThisDomain = utilitySpaceEstimate.getDomain().getIssues();

        for (Bid bid : bidOrder) {
            Map<Integer, Value> lastBidValues = bid.getValues();
            for (Issue issue : issuesInThisDomain) {
                int issueNumber = issue.getNumber();
                EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) utilitySpaceEstimate.getEvaluator(issueNumber);
                // get value of this issue for the last bid
                ValueDiscrete valueDiscrete = (ValueDiscrete) lastBidValues.get(issueNumber);
                int lastBidValue = evaluatorDiscrete.getValue(valueDiscrete);
                // increment by one for this value
                Map<Integer, Double> values = matrix.get(issueNumber);
                double currentValue = values.get(lastBidValue);
                values.replace(lastBidValue, currentValue + 1);
                matrix.replace(issueNumber, values);
            }
        }

    public void estimateUtilityWeights(List<Bid> bidOrder) {

    }
}

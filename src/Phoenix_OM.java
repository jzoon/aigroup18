import genius.core.Bid;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.Map;

public class Phoenix_OM extends OpponentModel {
    int amountOfIssues;

    @Override
    public void init(NegotiationSession negotiationSession,
                     Map<String, Double> parameters) {
        this.negotiationSession = negotiationSession;

        // get outcome and utility spaces and list of issues in this domain
        opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
        amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();

        initializeOmega();
    }

    @Override
    public void updateModel(Bid opponentBid, double time) {
        // if there is not more than one bid, there is no need to update the model
        if (negotiationSession.getOpponentBidHistory().size() < 2) {
            return;
        }

        // get the values of the first and last bid
        Map<Integer, Value> firstBidValues = negotiationSession.getOpponentBidHistory().getFirstBidDetails().getBid().getValues();
        Map<Integer, Value> lastBidValues = negotiationSession.getOpponentBidHistory().getLastBid().getValues();

        // add 1 to values that match first bid
        try{
            for (Map.Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {
                EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
                IssueDiscrete issue = ((IssueDiscrete) e.getKey());

                ValueDiscrete lastBidValue = (ValueDiscrete) lastBidValues.get(issue.getNumber());
                ValueDiscrete firstBidValue = (ValueDiscrete) firstBidValues.get(issue.getNumber());

                if (firstBidValue == lastBidValue) {
                    // add 1 to value for this issue
                    int eval = value.getEvaluationNotNormalized(firstBidValue);
                    int newEval = eval + 1;
                    value.setEvaluation(firstBidValue, newEval);

                    // update weight for this issue
                    e.getValue().setWeight(newEval);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();;
        }

        // normalize weights
        opponentUtilitySpace.normalizeWeights();
    }


    public void initializeOmega() {
        for (Map.Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {

            // Clear a lock on the weight of an objective or issue.
            opponentUtilitySpace.unlock(e.getKey());

            // set weights
            e.getValue().setWeight(1.0 / amountOfIssues);

            // set all values to one
            for (ValueDiscrete valueDiscrete : ((IssueDiscrete) e.getKey()).getValues()) {
                ((EvaluatorDiscrete) e.getValue()).setEvaluation(valueDiscrete, 1);
            }
        }
    }

    @Override
    public String getName() {
        return "Phoenix_OM";
    }
}

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Phoenix_OM extends OpponentModel {
    Map<Integer, Map<Integer, Double>> omega; // issues weights that are approximated using a frequency-based method


    @Override
    public void init(NegotiationSession negotiationSession,
                     Map<String, Double> parameters) {
        this.negotiationSession = negotiationSession;

        // get outcome and utility spaces and list of issues in this domain
        opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
        List<Issue> issuesInThisDomain = opponentUtilitySpace.getDomain().getIssues();

        initializeOmega();

    }

    @Override
    public void updateModel(Bid opponentBid, double time) {
        Map<Integer, Value> lastBidValues = negotiationSession.getOpponentBidHistory().getLastBid().getValues();

        if (!lastBidValues.isEmpty()) {
            for ()





            for (Map.Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {
                EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
                IssueDiscrete issue = ((IssueDiscrete) e.getKey());
                /*
                 * add constant learnValueAddition to the current preference of
                 * the value to make it more important
                 */
                ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid().getValue(issue.getNumber());
                Integer eval = value.getEvaluationNotNormalized(issuevalue);
                value.setEvaluation(issuevalue, (learnValueAddition + eval));
            }
        }


                // get value of this issue for the last bid


                // increment by one for this value
            opponentUtilitySpace.setWeight(issue, newWeight);

        }
    }

    public void initializeOmega() {
        for (Map.Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {

            // Clear a lock on the weight of an objective or issue.
            opponentUtilitySpace.unlock(e.getKey());
            //e.getValue().setWeight(commonWeight);

            // set all value weights to one
            for (ValueDiscrete vd : ((IssueDiscrete) e.getKey()).getValues()) {
                ((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
            }
        }
    }

    @Override
    public String getName() {
        return "Phoenix_OM";
    }
}

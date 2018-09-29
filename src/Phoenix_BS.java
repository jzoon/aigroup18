import java.util.*;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.misc.Range;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

public class Phoenix_BS extends OfferingStrategy{
    SortedOutcomeSpace outcomespace;
    AbstractUtilitySpace utilitySpace;
    AdditiveUtilitySpace additiveUtilitySpace;

    Map<Integer, Map<Integer, Double>> omega; // issues weights that are approximated using a frequency-based method
    List<Double> gamma; // weights for the three reference bids

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy,
                     Map<String, Double> parameters) {
        this.negotiationSession = negotiationSession;

        // initialize gamma
        if (parameters != null && parameters.get("gamma_first") != null && parameters.get("gamma_best") != null &&
                parameters.get("gamma_last") != null) {
            gamma = new ArrayList<>();
            gamma.add(parameters.get("gamma_first"));
            gamma.add(parameters.get("gamma_best"));
            gamma.add(parameters.get("gamma_last"));
        } else {
            gamma = new ArrayList<>();
            gamma.add(1.0);
            gamma.add(0.8);
            gamma.add(0.3);
        }

        // get outcome and utility spaces and list of issues in this domain
        outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
        utilitySpace = negotiationSession.getUtilitySpace();
        additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
        List<Issue> issuesInThisDomain = additiveUtilitySpace.getDomain().getIssues();

        // initialize omega with ones for each issue and value possible in this domain
        omega = new HashMap<>();
        for (Issue issue : issuesInThisDomain) {
            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

            Map<Integer, Double> issueValues = new HashMap<>();
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                int value = evaluatorDiscrete.getValue(valueDiscrete);
                issueValues.put(value, 1.0);
            }

            omega.put(issueNumber, issueValues);
        }
    }

    @Override
    public BidDetails determineOpeningBid() {
        return outcomespace.getMaxBidPossible();
    }

    @Override
    public BidDetails determineNextBid() {
        // determine minimal utility of the next bid
        double lowerBound = 0.7;
        double upperBound = 1;
        Range range = new Range(lowerBound, upperBound);

        // get available bids greater than minimal utility and get reference bids
        List<BidDetails> availableBids = getAvailableBids(range);
        List<BidDetails> referenceBids = getReferenceBids();

        // update omega using last bid of the opponent
        updateOmega(omega);

        // compute rating for all available bids
        List<Double> ratings = new ArrayList<>();
        for (int i = 0; i < availableBids.size(); i++) {
            ratings.add(i, computeRating(availableBids.get(i), referenceBids, omega, gamma));
        }

        // get index of the bid with highest rating (closest to zero)
        double highestRating = -1.0 * Double.MAX_VALUE;
        int indexHighestRating = 0;
        for (int j = 0; j < ratings.size(); j++) {
            double rating = ratings.get(j);
            if (rating > highestRating) {
                highestRating = rating;
                indexHighestRating = j;
            }
        }

        return availableBids.get(indexHighestRating);
    }

    /**
     * compute rating of a bid against reference bids
     * @param bidDetails compute for this bid
     * @param referenceBids first, best and last bids of the opponent
     * @param omega issues weights
     * @param gamma weights for the three reference bids
     * @return rating (closer to zero is more similar)
     */
    public double computeRating(BidDetails bidDetails, List<BidDetails> referenceBids, Map<Integer,
            Map<Integer, Double>> omega, List<Double> gamma) {
        Bid bid = bidDetails.getBid();
        List<Issue> issues = bid.getIssues();
        Map<Integer, Value> values = bid.getValues();

        double rating = 0;
        // compare bid to every reference bid
        for (int i = 0; i < referenceBids.size(); i++) {
            Bid referenceBid = referenceBids.get(i).getBid();
            Map<Integer, Value> referenceBidValues = referenceBid.getValues();

            // for every issue in this domain
            double[] omegaArray = new double[issues.size()];
            double[] bidValueArray = new double[issues.size()];
            double[] referenceBidValueArray = new double[issues.size()];
            for (int j = 0; j < issues.size(); j++) {
                int issueNumber = issues.get(j).getNumber();

                // convert Value of the issue to ValueDiscrete
                EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);
                ValueDiscrete referenceValue = (ValueDiscrete) referenceBidValues.get(issueNumber);
                ValueDiscrete value = (ValueDiscrete) values.get(issueNumber);

                // get omega, bid value and reference bid value
                omegaArray[j] = omega.get(issueNumber).get(evaluatorDiscrete.getValue(value));
                bidValueArray[j] = evaluatorDiscrete.getValue(value);
                referenceBidValueArray[j] = evaluatorDiscrete.getValue(referenceValue);
            }

            // calculate euclidean distance
            double distance = euclideanDistance(omegaArray, bidValueArray, referenceBidValueArray);

            // sum weighted distance
            rating += gamma.get(i) * distance;
        }

        return -1 * rating;
    }

    /**
     * update issues weights that are approximated using a frequency-based method
     * @param omega
     */
    public void updateOmega(Map<Integer, Map<Integer, Double>> omega) {
        List<Issue> issuesInThisDomain = additiveUtilitySpace.getDomain().getIssues();
        Map<Integer, Value> lastBidValues = negotiationSession.getOpponentBidHistory().getLastBid().getValues();

        if (!lastBidValues.isEmpty()) {
            for (Issue issue : issuesInThisDomain) {
                int issueNumber = issue.getNumber();
                EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

                // get value of this issue for the last bid
                ValueDiscrete valueDiscrete = (ValueDiscrete) lastBidValues.get(issueNumber);
                int lastBidValue = evaluatorDiscrete.getValue(valueDiscrete);

                // increment by one for this value
                Map<Integer, Double> values = omega.get(issueNumber);
                double currentValue = values.get(lastBidValue);
                values.replace(lastBidValue, currentValue + 1);
                omega.replace(issueNumber, values);
            }
        }
    }

    /**
     *
     * @param range in which the bids must be found.
     * @return list of bids which a utility in the given range.
     */
    public List<BidDetails> getAvailableBids(Range range) {
        return outcomespace.getBidsinRange(range);
    }

    /**
     *
     * @return list of reference bids (first bid, best bid, last bid)
     */
    public List<BidDetails> getReferenceBids() {
        List<BidDetails> referenceBids = new ArrayList<>();

        referenceBids.add(negotiationSession.getOpponentBidHistory().getFirstBidDetails());
        referenceBids.add(negotiationSession.getOpponentBidHistory().getBestBidDetails());
        referenceBids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails());

        return referenceBids;
    }

    /**
     *
     * @param omega issues weights
     * @param a issues values
     * @param b reference issues values
     * @return euclidean distance of omega * (a - b)
     */
    public double euclideanDistance(double[] omega, double[] a, double[] b) {
        double diff_square_sum = 0.0;
        for (int i = 0; i < omega.length; i++) {
            diff_square_sum += Math.pow(omega[i] * (a[i] - b[i]), 2);
        }

        return Math.sqrt(diff_square_sum);
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("gamma_first", 1.0,
                "Importance of the first bid of the opponent"));
        set.add(new BOAparameter("gamma_best", 0.8,
                "Importance of the best bid of the opponent"));
        set.add(new BOAparameter("gamma_last", 0.3,
                "Importance of the last bid of the opponent"));
        return set;
    }

    @Override
    public String getName() {
        return "Group18_BS";
    }
}

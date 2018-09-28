import java.util.*;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.OutcomeSpace;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.issue.*;
import genius.core.misc.Range;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

public class Phoenix_BS extends OfferingStrategy{
    SortedOutcomeSpace outcomespace;
    AbstractUtilitySpace utilitySpace;
    AdditiveUtilitySpace additiveUtilitySpace;

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy,
                     Map<String, Double> parameters) {
        outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
        utilitySpace = negotiationSession.getUtilitySpace();
        additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        // initialize omega with ones
        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
        Map<Integer, Double> omega = new HashMap<>();
        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            omega.put(issueNumber, 1.0);
        }
    }

    @Override
    public BidDetails determineOpeningBid() {
        return outcomespace.getMaxBidPossible();
    }

    @Override
    public BidDetails determineNextBid() {
        // determine minimal utility of the next bid
        double lowerBound = 0;
        double upperBound = 1;
        Range range = new Range(lowerBound, upperBound);

        // get available bids greater than minimal utility and get reference bids
        List<BidDetails> availableBids = getAvailableBids(range);
        List<BidDetails> referenceBids = getReferenceBids();

        // compute rating for all available bids
        List<Double> ratings = new ArrayList<>();
        for (int i = 0; i < availableBids.size(); i++) {
            ratings.set(i, computeRating(availableBids.get(i), referenceBids));
        }

        // get index of the bid with highest rating
        double highestRating = 0;
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

    public double computeRating(BidDetails bidDetails, List<BidDetails> referenceBids, Map<Integer, Double> omega,
                                List<Double> gamma) {
        Bid bid = bidDetails.getBid();
        Domain domain = bid.getDomain();
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
                EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

                ValueDiscrete referenceValue = (ValueDiscrete) referenceBidValues.get(issueNumber);
                ValueDiscrete value = (ValueDiscrete) values.get(issueNumber);

                omegaArray[j] = omega.get(issueNumber);
                bidValueArray[j] = evaluatorDiscrete.getValue(value);
                referenceBidValueArray[j] = evaluatorDiscrete.getValue(referenceValue);
            }

            // calculate euclidean distance
            double distance = euclideanDistance(omegaArray, bidValueArray, referenceBidValueArray);

            rating += gamma.get(i) * distance;
        }

        return -1 * rating;
    }

    /**
     *
     * @param range in which the bids must be found.
     * @return list of bids which a utility in the given range.
     */
    public List<BidDetails> getAvailableBids(Range range) {
        return negotiationSession.getOutcomeSpace().getBidsinRange(range);
    }

    /**
     *
     * @return list of reference bids
     */
    public List<BidDetails> getReferenceBids() {
        List<BidDetails> referenceBids = new ArrayList<>();

        referenceBids.add(negotiationSession.getOpponentBidHistory().getFirstBidDetails());
        referenceBids.add(negotiationSession.getOpponentBidHistory().getBestBidDetails());
        referenceBids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails());

        return referenceBids;
    }

    public double euclideanDistance(double[] omega, double[] a, double[] b) {
        double diff_square_sum = 0.0;
        for (int i = 0; i < omega.length; i++) {
            diff_square_sum += Math.pow(omega[i] * a[i] - b[i], 2);
        }

        return Math.sqrt(diff_square_sum);
    }

    @Override
    public String getName() {
        return "Group18_BS";
    }
}

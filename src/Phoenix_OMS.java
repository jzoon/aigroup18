import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.misc.Range;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

public class Phoenix_OMS extends OMStrategy {
	
    List<Double> gamma; // weights for the three reference bids
    double bias; // lower bias gives higher ratings a higher probability to be chosen (between 0 and 1)
    AbstractUtilitySpace utilitySpace;
    AdditiveUtilitySpace additiveUtilitySpace;
	
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		
		// initialize gamma and bias
        if (parameters != null && parameters.get("gamma_first") != null && parameters.get("gamma_best") != null &&
                parameters.get("gamma_last") != null && parameters.get("bias") != null) {
            gamma = new ArrayList<>();
            gamma.add(parameters.get("gamma_first"));
            gamma.add(parameters.get("gamma_best"));
            gamma.add(parameters.get("gamma_last"));
            bias = parameters.get("bias");
        } else {
            gamma = new ArrayList<>();
            gamma.add(1.0);
            gamma.add(0.8);
            gamma.add(0.3);
            bias = 0.25;
        }
		
		utilitySpace = negotiationSession.getUtilitySpace();
		additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
	}
	
	public BidDetails getBid(List<BidDetails> allBids) {
        // get available bids greater than minimal utility and get reference bids
        List<BidDetails> referenceBids = getReferenceBids();

        // compute rating for all available bids
        List<Double> ratings = new ArrayList<>();
        for (int i = 0; i < allBids.size(); i++) {
            ratings.add(i, computeRating(allBids.get(i), referenceBids, gamma));
        }

        // choose bid randomly, where bids with higher rating have higher probability to be chosen
        return drawBidFollowRating(allBids, ratings, bias);
	}
	
	@Override
	public boolean canUpdateOM() {
		return true;
	}
	
	public double computeRating(BidDetails bidDetails, List<BidDetails> referenceBids, List<Double> gamma) {
        Bid bid = bidDetails.getBid();
        List<Issue> issues = bid.getIssues();
        Map<Integer, Value> values = bid.getValues();

        double rating = 0;
        // compare bid to every reference bid
        for (int i = 0; i < referenceBids.size(); i++) {
            Bid referenceBid = referenceBids.get(i).getBid();
            Map<Integer, Value> referenceBidValues = referenceBid.getValues();

            // get issue weights
            double[] omegaArray = model.getIssueWeights();

            // for every issue in this domain
            double[] bidValueArray = new double[issues.size()];
            double[] referenceBidValueArray = new double[issues.size()];
            for (int j = 0; j < issues.size(); j++) {
                int issueNumber = issues.get(j).getNumber();

                // convert Value of the issue to ValueDiscrete
                EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);
                ValueDiscrete referenceValue = (ValueDiscrete) referenceBidValues.get(issueNumber);
                ValueDiscrete value = (ValueDiscrete) values.get(issueNumber);

                // get bid value and reference bid value
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
	
	public BidDetails drawBidFollowRating(List<BidDetails> availableBids, List<Double> ratings, double bias) {
        TreeMap<Double, List<BidDetails>> sortedBids = new TreeMap<>();
        double lowestRating = 0;
        double highestRating = -1.0 * Double.MAX_VALUE;

        for (int i = 0; i < availableBids.size(); i++) {
            // get bid details and corresponding rating
            BidDetails bidDetails = availableBids.get(i);
            double rating = ratings.get(i);

            // update lowest or highest rating
            if (rating > highestRating) {
                highestRating = rating;
            }
            if (rating < lowestRating) {
                lowestRating = rating;
            }

            // bids with same rating gets put into the same list
            if (sortedBids.containsKey(rating)) {
                List<BidDetails> bidsWithSameRating = sortedBids.get(rating);
                bidsWithSameRating.add(bidDetails);
                sortedBids.replace(rating, bidsWithSameRating);
            } else {
                List<BidDetails> bidsWithSameRating = new ArrayList<>();
                bidsWithSameRating.add(bidDetails);
                sortedBids.put(rating, bidsWithSameRating);
            }
        }

        // sample a double between lowest rating and highest rating, with more bias towards highest rating
        double sampleRating = lowestRating + (highestRating - lowestRating) * Math.pow(Math.random(), bias);
        Map.Entry<Double, List<BidDetails>> ceiling = sortedBids.ceilingEntry(sampleRating);
        Map.Entry<Double, List<BidDetails>> floor = sortedBids.floorEntry(sampleRating);

        // check which key is closer to sample rating
        List<BidDetails> closestBids;
        if ((ceiling.getKey() - sampleRating) < (sampleRating - floor.getKey())) {
            // ceiling key is closer
            closestBids = ceiling.getValue();
        } else {
            // floor key is closer
            closestBids = floor.getValue();
        }

        // choose random bid from closest bids
        int size = closestBids.size();
        int index = (int) (Math.random() * size);

        return closestBids.get(index);
    }
	
    /**
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
        set.add(new BOAparameter("bias", 0.25,
                "Lower bias: higher ratings have higher probability (between 0 and 1)"));
        return set;
	}

	@Override
	public String getName() {
		return "Phoenix_OMS";
	}
}

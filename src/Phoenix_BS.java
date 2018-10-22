import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.misc.Range;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Phoenix_BS extends OfferingStrategy {
	
    SortedOutcomeSpace outcomespace;

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy,
                     Map<String, Double> parameters) {
        this.negotiationSession = negotiationSession;
        this.opponentModel = opponentModel;
        this.omStrategy = omStrategy;

        // get outcome and utility spaces and list of issues in this domain
        outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
    }

    @Override
    public BidDetails determineOpeningBid() {
        return outcomespace.getMaxBidPossible();
    }

    /**
     * Select the next bid for the opponent using opponent model and opponent model strategy
     * @return next bid
     */
    @Override
    public BidDetails determineNextBid() {
        // update opponent model
        opponentModel.updateModel(outcomespace.getMaxBidPossible().getBid());

        // determine minimal utility of the next bid
        double lowerBound = findLowerBound();
        double upperBound = 1;
        Range range = new Range(lowerBound, upperBound);

        // get available bids greater than minimal utility and get reference bids
        List<BidDetails> availableBids = getAvailableBids(range);

        return omStrategy.getBid(availableBids);
    }
    
    /**
     * Computes the lower bound value, depending on the time left, the best offer of the opponent and the best possible offer
     * @return lower bound value (double)
     */
    public double findLowerBound() {
    	double maximumOffer = outcomespace.getMaxBidPossible().getMyUndiscountedUtil();
    	double lowestOffer = maximumOffer/1.4;
	   	double minimumOffer = negotiationSession.getOpponentBidHistory().getBestBidDetails().getMyUndiscountedUtil();
		double difference = maximumOffer - minimumOffer;
		
		if (difference >= 0) {
			double percentageTimeLeft = (negotiationSession.getTimeline().getTotalTime() -
					negotiationSession.getTimeline().getCurrentTime())/negotiationSession.getTimeline().getTotalTime();
			
			double acceptableOffer = maximumOffer - (difference*Math.pow((1-percentageTimeLeft), 2));
		
			if (acceptableOffer < maximumOffer && acceptableOffer > lowestOffer) {
				return acceptableOffer;
			} else if (acceptableOffer < lowestOffer) {
				return lowestOffer;
			}
		}
    	
    	return maximumOffer;
    }

    /**
     *
     * @param range in which the bids must be found.
     * @return list of bids which a utility in the given range.
     */
    public List<BidDetails> getAvailableBids(Range range) {
    	return outcomespace.getBidsinRange(range);
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        return set;
    }

    @Override
    public String getName() {
        return "Phoenix_BS_New";
    }
}

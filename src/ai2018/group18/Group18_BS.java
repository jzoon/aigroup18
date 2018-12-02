package ai2018.group18;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.misc.Range;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Group18_BS extends OfferingStrategy {

    private UserModel userModel;
    private SortedOutcomeSpace outcomeSpace;
    private UtilityFunctionEstimate utilityFunctionEstimate;

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy,
                     Map<String, Double> parameters) {
        this.negotiationSession = negotiationSession;
        this.opponentModel = opponentModel;
        this.omStrategy = omStrategy;

        userModel = negotiationSession.getUserModel();
        if (userModel != null) { // "enable uncertainty" is checked

            // create utility space with estimated preferences
            List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
            AdditiveUtilitySpace utilitySpaceEstimate = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
            utilityFunctionEstimate = new UtilityFunctionEstimate(utilitySpaceEstimate, bidOrder);
            utilitySpaceEstimate = utilityFunctionEstimate.getUtilitySpace();

            // create outcomeSpace from utility space estimate and set it for negotiation session
            outcomeSpace = new SortedOutcomeSpace(utilitySpaceEstimate);
            this.negotiationSession.setOutcomeSpace(outcomeSpace);

        } else { // "enable uncertainty" is unchecked

            outcomeSpace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
        }
    }

    @Override
    public BidDetails determineOpeningBid() {
        return outcomeSpace.getMaxBidPossible();
    }

    /**
     * Updates the opponentModel and uses the OMStrategy to determine the next bid
     */
    @Override
    public BidDetails determineNextBid() {
        // update opponent model
        opponentModel.updateModel(outcomeSpace.getMaxBidPossible().getBid());

        // determine minimal utility of the next bid
        boolean discounted = false;
        if (negotiationSession.getDiscountFactor() < 0.7) {
        	discounted = true;
        }
        double lowerBound = findLowerBound(discounted);
        double upperBound = 1;
        Range range = new Range(lowerBound, upperBound);

        // get available bids greater than minimal utility
        List<BidDetails> availableBids = getAvailableBids(range);

        // return bid that is drawn from the available bids
        return omStrategy.getBid(availableBids);
    }
    
    /**
     * Computes the lower bound value, depending on the time left, the best offer of the opponent and the best possible offer
     * @return lower bound value (double)
     */
    public double findLowerBound(boolean discounted) {
        // calculate lowest utility that we will ever propose
    	double lowerBound = outcomeSpace.getMaxBidPossible().getMyUndiscountedUtil();
    	double lowestOffer = lowerBound / 1.4;

    	// find best offer of the opponent and compare to best offer possible
	   	double minimumOffer = negotiationSession.getOpponentBidHistory().getBestBidDetails().getMyUndiscountedUtil();
	   	if (userModel != null) { // "enable uncertainty" is checked
            Bid bestBid = negotiationSession.getOpponentBidHistory().getBestBidDetails().getBid();
	   	    minimumOffer = utilityFunctionEstimate.getUtilityEstimate(bestBid);
        }
		double difference = lowerBound - minimumOffer;
		
		// if best offer of the opponent is less than our best offer possible
		if (difference >= 0) {
            // get percentage time left
            double totalTime = negotiationSession.getTimeline().getTotalTime();
            double currentTime = negotiationSession.getTimeline().getCurrentTime();
            double percentageTimeLeft = (totalTime - currentTime) / totalTime;

			// calculate utility of acceptable offer depending on the time left
			double acceptableOffer;
			if (discounted) {
				double discountFactor = 0.45 - negotiationSession.getDiscountFactor();
				
				if (discountFactor > 0) {
					acceptableOffer = minimumOffer - discountFactor;
				} else {
					acceptableOffer = minimumOffer;
				}
			} else {
				acceptableOffer = lowerBound - (difference * Math.pow((1 - percentageTimeLeft), 2));
			}

			// decide if we use utility of time dependant offer or our lowest possible offer
			if (acceptableOffer < lowerBound && acceptableOffer > lowestOffer) {
				return acceptableOffer;
			} else if (acceptableOffer < lowestOffer) {
				return lowestOffer;
			}
		}

		// else utility is best possible offer
    	return lowerBound;
    }

    /**
     *
     * @param range in which the bids must be found.
     * @return list of bids which a utility in the given range.
     */
    public List<BidDetails> getAvailableBids(Range range) {
    	return outcomeSpace.getBidsinRange(range);
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        return set;
    }

    @Override
    public String getName() {
        return "Group18_BS";
    }
}

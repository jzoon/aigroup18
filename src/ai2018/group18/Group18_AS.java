package ai2018.group18;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AdditiveUtilitySpace;

/**
 * This acceptance class will accept bids if they are higher than the threshold,
 * defined by a function of time and the difference between our own best offer 
 * and the opponents best offer.
 */
public class Group18_AS extends AcceptanceStrategy {

    private UserModel userModel;
    private UtilityFunctionEstimate utilityFunctionEstimate;
    private double a; // alpha
    private double b; // beta

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;

        userModel = negotiationSession.getUserModel();
        if (userModel != null) { // "enable uncertainty" is checked

            // create utility space with estimated preferences
            List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
            AdditiveUtilitySpace utilitySpaceEstimate = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
            utilityFunctionEstimate = new UtilityFunctionEstimate(utilitySpaceEstimate, bidOrder);
        }

        // initialize alpha and beta
        if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = 1.4;
			b = 0.02;
		}
    }
    
	@Override
	public String printParameters() {
		String str = "[a: " + a + "]";
		return str;
	}

	/**
	 * Determines the acceptability based on the acceptance function, given (un)discounted or (un)certainty domain
	 */
    @Override
    public Actions determineAcceptability() {
    	if (negotiationSession.getDiscountFactor() < 0.7) {
    	    if (userModel != null) {
    	        return determineAcceptabilityAction(true, true);
            } else {
                return determineAcceptabilityAction(true, false);
            }
    	} else {
    	    if (userModel != null) {
    	        return determineAcceptabilityAction(false, true);
            } else {
                return determineAcceptabilityAction(false, false);
            }
    	}
    }
    
    /**
     * Determines the acceptability based on the acceptance function.
     * Acceptance function is based on the time left and the difference between our best offer and the opponents best offer.
     * for (un)discounted or (un)certainty domains
     */
    public Actions determineAcceptabilityAction(boolean discount, boolean uncertainty) {
        double percentageTimeLeft = getPercentageTimeLeft();

        // get utility of my first bid, my next bid, opponent's best bid, opponent's last bid
        double myFirstBidUtility = getMyFirstBidUtility(uncertainty);
        double myNextBidUtility = getMyNextBidUtility(uncertainty);
        double opponentsBestBidUtility = getOpponentsBestBidUtility(uncertainty);
        double opponentsLastBidUtility = getOpponentsLastBidUtility(uncertainty);

        // find minimum offer that we are ever willing to accept
        double minimumOffer = findMinimumOffer(myFirstBidUtility, opponentsBestBidUtility);

        // calculate difference between our first bid and the best bid of the opponent
        double difference = myFirstBidUtility - opponentsBestBidUtility;

        // find minimum offer that we are willing to accept at the current time
        double acceptableOffer;
        if (discount) {
            acceptableOffer = findAcceptableOfferDiscounted(minimumOffer);
        } else {
            acceptableOffer = findAcceptableOffer(minimumOffer, percentageTimeLeft, myFirstBidUtility, difference);
        }

        // Accept an offer if it is better than my next bid OR if it is better than the acceptableOffer variable
        if (opponentsLastBidUtility >= myNextBidUtility && opponentsLastBidUtility >= opponentsBestBidUtility) {
            return Actions.Accept;
        } else if (opponentsLastBidUtility >= acceptableOffer) {
            return Actions.Accept;
        } else {
            return Actions.Reject;
        }
    }
    
    /**
     * Determines minimum offer based on our optimal bid and the opponent's best bid.
     * @param myFirstBid
     * @param opponentsBestBid
     * @return minimimOffer
     */
    public double findMinimumOffer(double myFirstBid, double opponentsBestBid) {
        if (myFirstBid / a > opponentsBestBid) {
            return myFirstBid / a;
        } else {
            return opponentsBestBid;
        }
    }
    
    /**
     * An acceptable offer: the closer we come to the end of a negotiation, the lower it gets. In the last 2% of rounds it is
     * equal to the minimumoffer variable.
     * @param minimumOffer
     * @param percentageTimeLeft
     * @param myFirstBid
     * @param difference
     * @return acceptableOffer utility
     */
    public double findAcceptableOffer(double minimumOffer, double percentageTimeLeft, double myFirstBid, double difference) {
        if (percentageTimeLeft > b) {
        	return myFirstBid - (difference * Math.pow((1 - percentageTimeLeft), 2));
        } else {
            return minimumOffer;
        }
    }
    
    /**
     * Finds an acceptable offer for a discounted domain, using a different function.
     * @param minimumOffer
     * @return acceptableOffer utility
     */
    public double findAcceptableOfferDiscounted(double minimumOffer) {
    	double discountFactor = 0.45 - negotiationSession.getDiscountFactor();
		
		if (discountFactor > 0) {
			return minimumOffer - discountFactor;
		} else {
			return minimumOffer;
		}
    }

    /**
     *
     * @return percentage time left
     */
    public double getPercentageTimeLeft() {
        double totalTime = negotiationSession.getTimeline().getTotalTime();
        double currentTime = negotiationSession.getTimeline().getCurrentTime();
        return (totalTime - currentTime) / totalTime;
    }

    /**
     *
     * @param uncertainty 
     * @return get utility of my first bid
     */
    public double getMyFirstBidUtility(boolean uncertainty) {
        if (negotiationSession.getTimeline().getCurrentTime() > 1) {
            BidDetails myFirstBidDetails = negotiationSession.getOwnBidHistory().getFirstBidDetails();
            if (uncertainty) {
                return utilityFunctionEstimate.getUtilityEstimate(myFirstBidDetails.getBid());
            } else {
                return myFirstBidDetails.getMyUndiscountedUtil();
            }
        } else {
            return 1;
        }
    }

    /**
     *
     * @param uncertainty
     * @return get utility of my next bid
     */
    public double getMyNextBidUtility(boolean uncertainty) {
        BidDetails myNextBidDetails = offeringStrategy.getNextBid();
        if (uncertainty) {
            return utilityFunctionEstimate.getUtilityEstimate(myNextBidDetails.getBid());
        } else {
            return myNextBidDetails.getMyUndiscountedUtil();
        }
    }

    /**
     *
     * @param uncertainty
     * @return get utility of the best bid of the opponent
     */
    public double getOpponentsBestBidUtility(boolean uncertainty) {
        BidDetails opponentsBestBidDetails = negotiationSession.getOpponentBidHistory().getBestBidDetails();
        if (uncertainty) {
            return utilityFunctionEstimate.getUtilityEstimate(opponentsBestBidDetails.getBid());
        } else {
            return opponentsBestBidDetails.getMyUndiscountedUtil();
        }
    }

    /**
     *
     * @param uncertainty
     * @return get utility of the last bid of the opponent
     */
    public double getOpponentsLastBidUtility(boolean uncertainty) {
        BidDetails opponentsLastBidDetails = negotiationSession.getOpponentBidHistory().getLastBidDetails();
        if (uncertainty) {
            return utilityFunctionEstimate.getUtilityEstimate(opponentsLastBidDetails.getBid());
        } else {
            return opponentsLastBidDetails.getMyUndiscountedUtil();
        }
    }
    
	@Override
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 1.4,
				"Acceptable bid becomes starting offer divided by a if that is higher than the timedependent bid"));
		
		set.add(new BOAparameter("b", 0.02,
				"The last b percentage of rounds, the agent will accept offers equal to its minimum offer variable"));

		return set;
	}

    @Override
    public String getName() {
        return "Group18_AS";
    }
}
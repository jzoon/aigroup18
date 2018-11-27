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
    private double a;
    private double b;

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
        
        if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = 1.2;
			b = 0.02;
		}
    }
    
	@Override
	public String printParameters() {
		String str = "[a: " + a + "]";
		return str;
	}

	/**
	 * Determines the acceptability based on the acceptance function.
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
     * Determines the acceptability based on the undiscounted acceptance function.
     * Acceptance function is based on the time left and the difference between our best offer and the opponents best offer.
     */
    public Actions determineAcceptabilityAction(boolean discount, boolean uncertainty) {
        // get percentage time left
        double totalTime = negotiationSession.getTimeline().getTotalTime();
        double currentTime = negotiationSession.getTimeline().getCurrentTime();
        double percentageTimeLeft = (totalTime - currentTime) / totalTime;

        // get utility of my first bid
        double myFirstBidUtility = 1;
        if (negotiationSession.getTimeline().getCurrentTime() > 1) {
            BidDetails myFirstBidDetails = negotiationSession.getOwnBidHistory().getFirstBidDetails();
            if (uncertainty) {
                myFirstBidUtility = utilityFunctionEstimate.getUtilityEstimate(myFirstBidDetails.getBid());
            } else {
                myFirstBidUtility = myFirstBidDetails.getMyUndiscountedUtil();
            }
        }

        // get utility of my next bid
        BidDetails myNextBidDetails = offeringStrategy.getNextBid();
        double myNextBidUtility = myNextBidDetails.getMyUndiscountedUtil();
        if (uncertainty) {
            myNextBidUtility = utilityFunctionEstimate.getUtilityEstimate(myNextBidDetails.getBid());
        }

        // get utility of best bid of the opponent
        BidDetails opponentsBestBidDetails = negotiationSession.getOpponentBidHistory().getBestBidDetails();
        double opponentsBestBidUtility = opponentsBestBidDetails.getMyUndiscountedUtil();
        if (uncertainty) {
            opponentsBestBidUtility = utilityFunctionEstimate.getUtilityEstimate(opponentsBestBidDetails.getBid());
        }

        // get utility of last bid of the opponent
        BidDetails opponentsLastBidDetails = negotiationSession.getOpponentBidHistory().getLastBidDetails();
        double opponentsLastBidUtility = opponentsLastBidDetails.getMyUndiscountedUtil();
        if (uncertainty) {
            opponentsLastBidUtility = utilityFunctionEstimate.getUtilityEstimate(opponentsLastBidDetails.getBid());
        }

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
        double minimumOffer;
        if (myFirstBid / a > opponentsBestBid) {
            minimumOffer = myFirstBid / a;
        } else {
            minimumOffer = opponentsBestBid;
        }
        
        return minimumOffer;
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
    	double acceptableOffer = minimumOffer;
        if (percentageTimeLeft > b) {
        	acceptableOffer = myFirstBid - (difference * Math.pow((1 - percentageTimeLeft), 2));
        }
        
        return acceptableOffer;
    }
    
    public double findAcceptableOfferDiscounted(double minimumOffer) {
    	double acceptableOffer;
    	double discountFactor = 0.45 - negotiationSession.getDiscountFactor();
		
		if (discountFactor > 0) {
			acceptableOffer = minimumOffer - discountFactor;
		} else {
			acceptableOffer = minimumOffer;
		}
        
        return acceptableOffer;
    }
    
	@Override
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 1.2,
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

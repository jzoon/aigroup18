package ai2018.group18;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

/**
 * This acceptance class will accept bids if they are higher than the threshold,
 * defined by a function of time and the difference between our own best offer 
 * and the opponents best offer.
 */
public class Group18_AS extends AcceptanceStrategy {

	private double a;
	private double b;
	
    /**
     * Empty constructor for the BOA framework.
     */
    public Group18_AS() {
    }

    public Group18_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.b = beta;
    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        
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
    		return determineAcceptabilityAction(true);
    	} else {
    		return determineAcceptabilityAction(false);
    	}
    }
    
    /**
	 * Determines the acceptability based on the undiscounted acceptance function.
	 * Acceptance function is based on the time left and the difference between our best offer and the opponents best offer.
	 */
    public Actions determineAcceptabilityAction(boolean discount) {
    	double myFirstBid = 1;
    	if (negotiationSession.getTimeline().getCurrentTime() > 1) {
    		myFirstBid = negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil();
    	} 
        double opponentsBestBid = negotiationSession.getOpponentBidHistory().getBestBidDetails()
                .getMyUndiscountedUtil();
        double myNextBid = offeringStrategy.getNextBid().getMyUndiscountedUtil();
        double lastOpponentBid = negotiationSession.getOpponentBidHistory().getLastBidDetails()
                .getMyUndiscountedUtil();
        
        double percentageTimeLeft = (negotiationSession.getTimeline().getTotalTime() -
                negotiationSession.getTimeline().getCurrentTime())/negotiationSession.getTimeline().getTotalTime();
        
        double minimumOffer = findMinimumOffer(myFirstBid, opponentsBestBid);
        double difference = myFirstBid - opponentsBestBid;
        
        double acceptableOffer;
        if (discount) {
        	acceptableOffer = findAcceptableOfferDiscounted(minimumOffer, percentageTimeLeft, myFirstBid, difference);
        } else {
        	acceptableOffer = findAcceptableOffer(minimumOffer, percentageTimeLeft, myFirstBid, difference);
        }

        // Accept an offer if it is better than my next bid OR if it is better than the acceptableOffer variable
        if (lastOpponentBid >= myNextBid && lastOpponentBid >= opponentsBestBid) {
            return Actions.Accept;
        } else if (lastOpponentBid >= acceptableOffer) {
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
        double minimumOffer = 1;
        if (myFirstBid/a > opponentsBestBid) {
            minimumOffer = myFirstBid/a;
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
        	acceptableOffer = myFirstBid - (difference*Math.pow((1-percentageTimeLeft), 2));
        }
        
        return acceptableOffer;
    }
    
    public double findAcceptableOfferDiscounted(double minimumOffer, double percentageTimeLeft, double myFirstBid, double difference) {
    	double acceptableOffer;
    	double discountFactor = 0.45-negotiationSession.getDiscountFactor();
		
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

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;

public class Group18_AS extends AcceptanceStrategy {
    public Group18_AS() {}

    public Actions determineAcceptability() {
        return Actions.Accept;
    }

    public String getName() {
        return "Group 18 Acceptance Strategy";
    }
}

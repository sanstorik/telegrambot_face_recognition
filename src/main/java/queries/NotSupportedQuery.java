package queries;

import org.telegram.telegrambots.api.objects.Message;

public class NotSupportedQuery extends Query {

    public NotSupportedQuery() {
        super(null, 1);
    }

    @Override public String getPhotoCaption() {
        return super.getPhotoCaption();
    }

    @Override public String getStartingHelp() {
        return "Not supported.";
    }

    @Override public boolean isValidInputForCurrentAction(Message message) {
        return true;
    }

    @Override public boolean isPhotoAnswer() {
        return false;
    }

    @Override public String executeQuery(Message message) {
        return "Not supported yet. Try other features.";
    }

    @Override public String action(Message message) {
        increaseActionIndex();
        return "Not supported yet. Try other features.";
    }
}

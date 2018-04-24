package queries;

import http.NeuralRequests;
import org.telegram.telegrambots.api.objects.Message;

import java.util.function.BiFunction;

public class LoginQuery extends Query {
    private String username;
    private String password;
    private BiFunction<Long, String, Boolean> storeToken;

    public LoginQuery(BiFunction<Long, String, Boolean> storeToken) {
        super(null, 2);
        this.storeToken = storeToken;
    }


    @Override public String getStartingHelp() {
        return "This is a login command. You will be able to use other commands when logged in." +
                " Input your username.";
    }


    @Override public boolean isValidInputForCurrentAction(Message message) {
        boolean allowed = false;

        switch (actionIndex) {
            case 0:
                allowed = message.hasText();
                break;
            case 1:
                allowed = message.hasText();
                break;
        }

        return allowed;
    }


    @Override public boolean isPhotoAnswer() {
        return false;
    }


    @Override public String executeQuery(Message message) {
        NeuralRequests.Response response = NeuralRequests.login(username, password);

        if (response.response != null && !response.response.isEmpty()) {
            storeToken.apply(message.getChatId(), response.response);
        }

        return response.message;
    }


    @Override public String action(Message message) {
        String help = "Error. Try this again.";

        switch (actionIndex) {
            case 0:
                username = message.getText();
                help = "Input your password.";
                break;
            case 1:
                password = message.getText();
                help = null;
                break;
        }

        increaseActionIndex();
        return help;
    }
}

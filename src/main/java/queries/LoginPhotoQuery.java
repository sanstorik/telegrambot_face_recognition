package queries;

import http.NeuralRequests;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class LoginPhotoQuery extends Query {
    private BiFunction<Long, String, Boolean> storeToken;
    private File image;
    private RegisterQuery.SendImageFunction sendImageFunction;
    private DownloadImageFromUrlFunction downloadFromUrl;
    private boolean isFaceCorrect;


    public LoginPhotoQuery(DownloadImageFunction downloadImage,
                           BiFunction<Long, String, Boolean> storeToken,
                           RegisterQuery.SendImageFunction sendImageFunction,
                           DownloadImageFromUrlFunction downloadFromUrl) {
        super(downloadImage, 2);
        this.storeToken = storeToken;
        this.sendImageFunction = sendImageFunction;
        this.downloadFromUrl = downloadFromUrl;
    }


    @Override public String getStartingHelp() {
        return "This is a login with photo command. You will be able to use other commands when logged in." +
                " Input your photo.";
    }


    @Override public boolean isValidInputForCurrentAction(Message message) {
        boolean allowed = false;

        switch (actionIndex) {
            case 0:
                allowed = message.hasPhoto();
                break;
            case 1:
                allowed = message.hasText() && (message.getText().equals("Yes")
                        || message.getText().equals("No"));
                break;
        }

        return allowed;
    }


    @Override public boolean isPhotoAnswer() {
        return false;
    }


    @Override public String executeQuery(Message message) {
        String responseMsg;

        if (isFaceCorrect) {
            NeuralRequests.Response response = NeuralRequests.loginPhoto(image);

            if (response.response != null && !response.response.isEmpty()) {
                storeToken.apply(message.getChatId(), response.response);
            }

            responseMsg = response.message;
        } else {
            responseMsg = "It is not your face, I'm sorry to hear that. Aborting query.";
        }

        return responseMsg;
    }


    @Override public String action(Message message) {
        String help = "Error. Try this again.";

        switch (actionIndex) {
            case 0:
                image = downloadImage.download(message.getPhoto(), message.getChatId());
                NeuralRequests.Response response = NeuralRequests.nonTokenCropFace(image);

                if (response.response == null || response.response.isEmpty()) {
                    help = "Aborting. No faces can be found or some network error.";
                } else {
                    File cropped = downloadFromUrl.download(response.response, message.getChatId());
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow keyboardRow = new KeyboardRow();
                    keyboardRow.add("Yes");
                    keyboardRow.add("No");
                    rows.add(keyboardRow);

                    sendImageFunction.send(cropped, message.getChatId(), "Found face", rows);
                    help = "Found face type = " +  response.message +
                            ". Is it your face? Answer Yes or No or use given keyboard.";
                }
                break;
            case 1:
                isFaceCorrect = message.getText().startsWith("Yes");
                help = null;
                break;
        }

        increaseActionIndex();
        return help;
    }
}

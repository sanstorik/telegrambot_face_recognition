package queries;

import http.NeuralRequests;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RegisterQuery extends Query {
    @FunctionalInterface
    public interface SendImageFunction {
        boolean send(File image, long chatId, String caption, List<KeyboardRow> rows);
    }

    private static final int MAX_IMAGES_COUNT = 6;

    private String username;
    private String password;
    private SendImageFunction sendImageFunction;
    private DownloadImageFromUrlFunction downloadFromUrl;
    private List<File> faces = new ArrayList<>();
    private List<String> faceTypes = new ArrayList<>();


    //variables for cycle control
    private String foundFaceFileUrl;
    private String foundFaceType;

    public RegisterQuery(DownloadImageFunction downloadImage,
                         SendImageFunction sendImageFunction,
                         DownloadImageFromUrlFunction downloadFromUrl) {
        super(downloadImage, 4);

        this.sendImageFunction = sendImageFunction;
        this.downloadFromUrl = downloadFromUrl;
    }


    @Override public String getStartingHelp() {
        return "This is registering new user command. To fully register you must provide 3-6 images." +
                " Images where you look right, left and straight are mandatory." +
                " First you should input your username.";
    }


    @Override public boolean isPhotoAnswer() {
        return false;
    }


    @Override public String action(Message message) {
        String nextMessage = "Error. Try this again.";

        switch (actionIndex) {
            case 0:
                username = message.getText();
                nextMessage = "Input your password";
                increaseActionIndex();
                break;
            case 1:
                password = message.getText();
                nextMessage = "Input your image file";
                increaseActionIndex();
                break;
            case 2:
                File image = downloadImage.download(message.getPhoto(), message.getChatId());
                NeuralRequests.Response response = NeuralRequests.nonTokenCropFace(image);


                if (response.response == null || response.response.isEmpty()) {
                    nextMessage = "No faces can be found or some network error. Try another image.";
                } else {
                    foundFaceType = response.message;
                    foundFaceFileUrl = image.getAbsolutePath();

                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow keyboardRow = new KeyboardRow();

                    if (faces.size() < MAX_IMAGES_COUNT) {
                        keyboardRow.add("Yes");
                        keyboardRow.add("No");
                    }

                    if (allImageTypesAreProvided()) {
                        keyboardRow.add("Register");
                    }

                    keyboardRow.add("Exit");
                    rows.add(keyboardRow);

                    File cropped = downloadFromUrl.download(response.response, message.getChatId());
                    sendImageFunction.send(cropped, message.getChatId(), "Found face.", rows);
                    nextMessage = "Found face type = " + foundFaceType + ". " +
                            "Is it your face? Answer Yes or No or use given keyboard. ";


                    if (allImageTypesAreProvided()) {
                        nextMessage += "You can select Register to register right away.";
                    } else if (faces.size() >= MAX_IMAGES_COUNT) {
                        nextMessage = "Not enough data in files you've put. Please reset and try again.";
                    }

                    increaseActionIndex();
                }
                break;
            case 3:
                if (message.getText().startsWith("Yes")) {
                    nextMessage = "We've put your face. Face type = " + foundFaceType +
                            ". Current image count = " + (faces.size() + 1) + ". Max size = " + MAX_IMAGES_COUNT;
                    nextMessage += ". Input next image.";

                    faces.add(new File(foundFaceFileUrl));
                    faceTypes.add(foundFaceType);


                    //stop if we've reached max images count
                    if (faces.size() >= MAX_IMAGES_COUNT) {
                        if (allImageTypesAreProvided()) {
                            increaseActionIndex();
                        } else {
                            nextMessage = "Aborting. Not all face types are provided.";
                        }
                    } else {
                        decreaseActionIndex();
                    }
                }

                // user declined photo to be registered
                else if (message.getText().startsWith("No")) {
                    nextMessage = "We haven't put your face. Face type = " + foundFaceType;
                    nextMessage += ". Input next image.";
                    decreaseActionIndex();
                }

                //user wants to register already
                else if (message.getText().startsWith("Register")) {
                    //leave cycle and go to executing query
                    nextMessage = null;
                    increaseActionIndex();
                } else if (message.getText().startsWith("Exit")) {
                    nextMessage = "Aborting. User stopped query.";
                }
                break;
        }


        return nextMessage;
    }


    @Override public boolean isValidInputForCurrentAction(Message message) {
        boolean isValid = false;

        switch (actionIndex) {
            case 0:
                isValid = message.hasText();
                break;
            case 1:
                isValid = message.hasText();
                break;
            case 2:
                isValid = message.hasPhoto();
                break;
            case 3:
                final boolean yesNoAnswer = faces.size() < MAX_IMAGES_COUNT
                        && (message.getText().equals("Yes") || message.getText().equals("No"));
                final boolean registerAnswer = allImageTypesAreProvided() && message.getText().equals("Register");
                final boolean exitAnswer = message.getText().equals("Exit");

                isValid = message.hasText() && (yesNoAnswer || registerAnswer || exitAnswer);
                break;
        }

        return isValid;
    }


    @Override public String executeQuery(Message message) {
        //register only when faces of all types are acquired
        return NeuralRequests.register(username, password, faces.toArray(new File[faces.size()]));
    }


    // all face types are given {center; left; right}
    private boolean allImageTypesAreProvided() {
        boolean left = false;
        boolean center = false;
        boolean right = false;

        for (String faceType : faceTypes) {
            if (faceType.equals("left")) {
                left = true;
            } else if (faceType.equals("center")) {
                center = true;
            } else if (faceType.equals("right")) {
                right = true;
            }
        }

        return left && center && right;
    }
}

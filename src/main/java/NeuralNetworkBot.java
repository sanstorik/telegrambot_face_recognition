import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import queries.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;


public class NeuralNetworkBot extends TelegramLongPollingBot {
    private Map<Long, Query> queries = new HashMap<>();
    private Map<Long, String> tokens = new HashMap<>();

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();

        if (message == null) {
            return;
        }

        if (message.hasText() && message.getText().startsWith("/stop")) {
            queries.remove(message.getChatId());
            return;
        }

        Query currentQuery = queries.getOrDefault(message.getChatId(), null);

        if (currentQuery == null && message.hasText()) {
            if (message.getText().startsWith(Commands.REGISTER.getCommand())) {
                currentQuery = new RegisterQuery(this::getImageFromResponse,
                        this::sendImageWithKeyboard, this::downloadImageFromUrl);
            } else if (message.getText().startsWith(Commands.LOGIN_PHOTO.getCommand())) {
                currentQuery = new LoginPhotoQuery(this::getImageFromResponse, this::storeToken,
                        this::sendImageWithKeyboard, this::downloadImageFromUrl);
            } else if (message.getText().startsWith(Commands.FACES_COORDINATES.getCommand())) {
                currentQuery = new NotSupportedQuery();
            } else if (message.getText().startsWith(Commands.EYES_COORDINATES.getCommand())) {
                currentQuery = new EyesCoordinatesQuery(this::getImageFromResponse,
                        tokens.getOrDefault(message.getChatId(), ""));
            } else if (message.getText().startsWith(Commands.HIGHLIGHT_FACES.getCommand())) {
                currentQuery = new HighlightFacesQuery(this::getImageFromResponse,
                        tokens.getOrDefault(message.getChatId(), ""));
            } else if (message.getText().startsWith(Commands.IDENTIFY_GROUP.getCommand())) {
                currentQuery = new IdentifyGroupQuery(this::getImageFromResponse,
                        tokens.getOrDefault(message.getChatId(), ""));
            } else if (message.getText().startsWith(Commands.CROP_FACE.getCommand())) {
                currentQuery = new CropfaceQuery(this::getImageFromResponse,
                        tokens.getOrDefault(message.getChatId(), ""));
            } else if (message.getText().startsWith(Commands.LOGIN.getCommand())) {
                currentQuery = new LoginQuery((this::storeToken));
            } else if (message.getText().startsWith(Commands.UPDATE_USER_PHOTO.getCommand())) {
                currentQuery = new NotSupportedQuery();
            } else {
                sendMessage("No such command found", message.getChatId());
                return;
            }

            queries.put(message.getChatId(), currentQuery);
            //starting setup message
            sendMessage(currentQuery.getStartingHelp(), message.getChatId());
            return;
        }


        if (currentQuery == null) {
            sendMessage("Please start with a command.", message.getChatId());
            return;
        }

        if (currentQuery.isValidInputForCurrentAction(message)) {
            if (currentQuery.isQueryCompleted()) {
                queries.remove(message.getChatId());
                throw new IllegalStateException("query at wrong state");
            } else {
                //setting up data for query
                String helperMessage = currentQuery.action(message);

                if (currentQuery.isQueryCompleted()) {
                    //execution of response to server
                    String answer = currentQuery.executeQuery(message);
                    sendResponse(message, currentQuery, answer);

                    queries.remove(message.getChatId());
                } else {
                    if (helperMessage.startsWith("Aborting")) {
                        queries.remove(message.getChatId());
                    }

                    sendMessage(helperMessage, message.getChatId());
                }
            }
        } else {
            sendMessage("Wrong input for current action", message.getChatId());
        }
    }


    private void sendResponse(Message message, Query query, String answer) {
        if (query.isPhotoAnswer()) {
            //we actually got not an url but error response
            if (!answer.startsWith("http")) {
                sendMessage(answer, message.getChatId());
            } else if (!sendImage(answer, message.getChatId(), query.getPhotoCaption())) {
                //send image couldn't be completed
                sendMessage("Image couldn't be uploaded.", message.getChatId());
            }
        } else {
            sendMessage(answer, message.getChatId());
        }
    }


    private boolean storeToken(Long chatId, String token) {
        tokens.put(chatId, token);
        return true;
    }


    //download image from user telegram request

    //should always check for null result
    private File getImageFromResponse(List<PhotoSize> photoSizes, long chatId) {
        PhotoSize biggest = photoSizes.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null);

        return biggest != null ? downloadImageFromTelegram(getFilePath(biggest), chatId) : null;
    }


    private String getFilePath(PhotoSize photo) {
        Objects.requireNonNull(photo);

        if (photo.hasFilePath()) {
            return photo.getFilePath();
        } else {
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photo.getFileId());

            try {
                org.telegram.telegrambots.api.objects.File file = execute(getFileMethod);

                return file.getFilePath();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    private File downloadImageFromTelegram(String filePath, long chatId) {
        String fullUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

        return downloadImageFromUrl(fullUrl, chatId);
    }


    private File downloadImageFromUrl(String url, long chatId) {
        File file = new File("images/" + UUID.randomUUID().toString().replace('-', '_') + ".jpg");
        try {
            new File("images").mkdirs();

            if (file.exists() || file.createNewFile()) {
                FileUtils.copyURLToFile(
                        new URL(url),
                        file);
            } else {
                file = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }


    private boolean sendImageWithKeyboard(File file, Long chatId, String caption, List<KeyboardRow> rows) {
        if (file == null) {
            return false;
        }

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId)
                .setNewPhoto(file)
                .setCaption(caption);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);
        sendPhoto.setReplyMarkup(keyboardMarkup);

        boolean uploaded = false;
        try {
            sendPhoto(sendPhoto);
            uploaded = true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        return uploaded;
    }


    //send image to chat

    private boolean sendImage(String url, Long chatId, String caption) {
        File downloadedFile = downloadImageFromUrl(url, chatId);

        return sendImage(downloadedFile, chatId, caption);
    }


    private boolean sendImage(File file, Long chatId, String caption) {
        if (file == null) {
            return false;
        }

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId)
                .setNewPhoto(file)
                .setCaption(caption);

        boolean uploaded = false;
        try {
            sendPhoto(sendPhoto);
            uploaded = true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        return uploaded;
    }


    private boolean sendMessage(String message, long chatId) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(chatId)
                .setText(message);

        boolean sent = false;
        try {
            sendMessage(sendMessage);
            sent = true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        return sent;
    }


    @Override public String getBotUsername() {
        return System.getProperty("BOT_USERNAME");
    }


    @Override public String getBotToken() {
        return System.getProperty("BOT_TOKEN");
    }
}

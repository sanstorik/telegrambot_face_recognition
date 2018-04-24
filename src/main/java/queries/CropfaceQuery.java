package queries;

import http.NeuralRequests;
import org.telegram.telegrambots.api.objects.Message;

import java.io.File;

public class CropfaceQuery extends Query {
    private File image;
    private String token;


    public CropfaceQuery(DownloadImageFunction downloadImage, String token) {
        super(downloadImage, 1);
        this.token = token;
    }


    @Override public String getPhotoCaption() {
        return "Cropped face";
    }


    @Override public String getStartingHelp() {
        return "Input your image.";
    }


    @Override public boolean isValidInputForCurrentAction(Message message) {
        return message.hasPhoto();
    }


    @Override public boolean isPhotoAnswer() {
        return true;
    }


    @Override public String executeQuery(Message message) {
        NeuralRequests.Response response = NeuralRequests.cropFace(image, token);

        if (response.response != null && !response.response.isEmpty()) {
            return response.response;
        }

        return response.message;
    }


    @Override public String action(Message message) {
        image = downloadImage.download(message.getPhoto(), message.getChatId());
        increaseActionIndex();

        return null;
    }
}

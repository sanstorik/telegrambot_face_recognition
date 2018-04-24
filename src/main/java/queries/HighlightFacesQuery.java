package queries;

import http.NeuralRequests;
import org.telegram.telegrambots.api.objects.Message;

import java.io.File;

public class HighlightFacesQuery extends Query {
    private File image;
    private String token;


    public HighlightFacesQuery(DownloadImageFunction downloadImage, String token) {
        super(downloadImage, 1);
        this.token = token;
    }


    @Override public String getPhotoCaption() {
        return "Highlighted faces";
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
        NeuralRequests.Response response = NeuralRequests.highlightFaces(image, token);

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

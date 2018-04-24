package queries;

import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;

import java.io.File;
import java.util.List;

public abstract class Query {
    public interface DownloadImageFunction {
        File download(List<PhotoSize> image, long chatId);
    }

    public interface DownloadImageFromUrlFunction {
        File download(String url, long chatId);
    }

    protected int actionIndex;
    private int maxActionIndex;
    protected DownloadImageFunction downloadImage;

    public Query(DownloadImageFunction downloadImage, int maxActionIndex) {
        this.downloadImage = downloadImage;
        this.maxActionIndex = maxActionIndex;
    }


    protected final void increaseActionIndex() {
        actionIndex++;
    }


    protected final void decreaseActionIndex() {
        actionIndex--;
    }


    public final boolean isQueryCompleted() {
        return actionIndex >= maxActionIndex;
    }


    public String getPhotoCaption() {
        return "empty";
    }


    public abstract String getStartingHelp();

    public abstract boolean isValidInputForCurrentAction(Message message);

    public abstract boolean isPhotoAnswer();

    public abstract String executeQuery(Message message);

    //returns next action message to user
    public abstract String action(Message message);
}

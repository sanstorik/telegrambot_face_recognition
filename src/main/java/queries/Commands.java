package queries;

public enum Commands {
    REGISTER("/register"),
    LOGIN("/login"),
    LOGIN_PHOTO("/login_photo"),
    HIGHLIGHT_FACES("/highlight_faces"),
    FACES_COORDINATES("/faces_coordinates"),
    EYES_COORDINATES("/eyes_coordinates"),
    IDENTIFY_GROUP("/identify_group"),
    UPDATE_USER_PHOTO("/update_user_photo"),
    CROP_FACE("/crop_face"),
    NON_TOKEN_CROP_FACE("/non_token_crop_face");

    private String command;


    Commands(String command) {
        this.command = command;
    }


    public String getCommand() {
        return command;
    }
}

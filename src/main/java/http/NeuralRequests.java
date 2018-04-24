package http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;
import queries.Commands;

import java.io.File;
import java.util.function.Consumer;


public class NeuralRequests {
    public static class Response {
        public String response;
        public String message;

        public Response(String response, String message) {
            this.response = response;
            this.message = message;
        }

        public Response() {
            response = "";
            message = "";
        }
    }

    private static final String API_URL = System.getProperty("API_URL");


    public static Response login(String username, String password) {
        Response response = new Response();

        try {
            HttpResponse<String> jsonResponse =
                    Unirest.get(API_URL + Commands.LOGIN.getCommand())
                            .queryString("username", username)
                            .queryString("password", password)
                            .asString();

            parseResponse(jsonResponse,
                    (jsonObject -> {
                        response.response = jsonObject.getAsJsonObject("data").get("Authorization").getAsString();
                        response.message = "Logged successfully.";
                    }),
                    (error -> response.message = error),
                    (invalid -> response.message = invalid)
            );
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return response;
    }


    public static Response loginPhoto(File image) {
        Response response = new Response();

        try {
            HttpResponse<String> jsonResponse =
                    Unirest.post(API_URL + Commands.LOGIN_PHOTO.getCommand())
                            .field("image", image)
                            .asString();

            parseResponse(jsonResponse,
                    (jsonObject -> {
                        JsonObject data = jsonObject.getAsJsonObject("data");
                        if (data.get("matched").getAsBoolean()) {
                            response.response = data.get("Authorization").getAsString();
                            response.message = "Max prob = " + data.get("max_probability").getAsDouble()
                                    + ". Logined as " + data.get("username").getAsString();
                        } else {
                            response.message = "Not authorized. Max prob = " + data.get("max_probability").getAsDouble();
                        }
                    }),
                    (error -> response.message = error),
                    (invalid -> response.message = invalid)
            );
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return response;
    }


    public static String register(String username, String password, File... image) {
        Response response = new Response();

        try {
            MultipartBody request = Unirest.post(API_URL + Commands.REGISTER.getCommand())
                    .field("username", username)
                    .field("password", password);

            for (int i = 0; i < image.length; i++) {
                if (image[i] != null) {
                    request.field("image" + (i + 1), image[i]);
                }
            }

            parseResponse(request.asString(),
                    (jsonObject -> response.response = jsonObject.get("message").getAsString()),
                    (error -> response.response = error),
                    (invalid -> response.response = invalid)
            );

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return response.response;
    }


    public static Response eyesCoodinates(File image, String token) {
        Response response = new Response();
        Unirest.setDefaultHeader("Authorization", "Bearer " + token);

        try {
            HttpResponse<String> jsonResponse =
                    Unirest.post(API_URL + Commands.EYES_COORDINATES.getCommand())
                            .field("image", image)
                            .asString();
            Unirest.clearDefaultHeaders();

            parseResponse(jsonResponse,
                    (jsonObject -> {
                        StringBuilder outputMsg = new StringBuilder();

                        JsonObject data = jsonObject.getAsJsonObject("data");
                        int index = 1;

                        while (data.getAsJsonArray("eyes_" + index) != null) {
                            JsonArray jsonElements = data.getAsJsonArray("eyes_" + index);
                            outputMsg.append("Eyes for person ").append(index).append(" = ");

                            //4 eyes points in form (x1,y1,x2, y2)
                            if (jsonElements.size() == 4) {
                                outputMsg.append("left = (")
                                        .append(jsonElements.get(0))
                                        .append(", ")
                                        .append(jsonElements.get(1))
                                        .append("), ");
                                outputMsg.append("right = (")
                                        .append(jsonElements.get(2))
                                        .append(", ")
                                        .append(jsonElements.get(3))
                                        .append(");  ");
                            }

                            index++;
                        }

                        response.response = outputMsg.toString();
                    }),
                    (error -> response.message = error),
                    (invalid -> response.message = invalid)
            );
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return response;
    }


    public static Response highlightFaces(File image, String token) {
        return simpleImageRequest(Commands.HIGHLIGHT_FACES, image, token);
    }


    public static Response cropFace(File image, String token) {
        return simpleImageRequest(Commands.CROP_FACE, image, token);
    }


    public static Response nonTokenCropFace(File image) {
        return simpleImageRequest(Commands.NON_TOKEN_CROP_FACE, image, null, "face_type");
    }


    public static Response identifyGroup(File image, String token) {
        return simpleImageRequest(Commands.IDENTIFY_GROUP, image, token);
    }


    private static Response simpleImageRequest(Commands command, File image, String token, String messageKey) {
        Response response = new Response();
        if (token != null) {
            Unirest.setDefaultHeader("Authorization", "Bearer " + token);
        }

        try {
            HttpResponse<String> jsonResponse =
                    Unirest.post(API_URL + command.getCommand())
                            .field("image", image)
                            .asString();

            if (token != null) {
                Unirest.clearDefaultHeaders();
            }

            parseResponse(jsonResponse,
                    (jsonObject -> {
                        JsonObject data = jsonObject.getAsJsonObject("data");
                        response.response = data.get("image_url").getAsString();

                        if (messageKey != null) {
                            response.message = data.get(messageKey).getAsString();
                        }
                    }),
                    (error -> response.message = error),
                    (invalid -> response.message = invalid)
            );
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return response;
    }


    private static Response simpleImageRequest(Commands command, File image, String token) {
        return simpleImageRequest(command, image, token, null);
    }


    private static void parseResponse(HttpResponse<String> jsonResponse,
                                      Consumer<JsonObject> successQuery,
                                      Consumer<String> errorQuery,
                                      Consumer<String> invalidQuery) {
        if (success(jsonResponse.getStatus())) {
            JsonElement jsonElement = new JsonParser().parse(jsonResponse.getBody());
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (queryCompleted(jsonObject.get("status").getAsString())) {
                successQuery.accept(jsonObject);
            } else {
                errorQuery.accept(jsonObject.get("message").getAsString());
            }
        } else {
            invalidQuery.accept("Couldn't connect to server. Try again later.");
        }
    }


    private static boolean success(int status) {
        return status == 200;
    }


    private static boolean queryCompleted(String status) {
        return !status.equals("error");
    }
}

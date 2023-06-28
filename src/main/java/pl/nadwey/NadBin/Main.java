package pl.nadwey.NadBin;

import com.google.gson.Gson;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main {
    // TODO: make config file or something
    static final int MIN_BIN_NAME_LENGTH = 15;
    static final String ALLOWED_BIN_REGEX = "^[a-zA-Z0-9-_]+$";

    static DBManager dbManager;

    public static void main(String[] args) throws Exception {
        dbManager = new DBManager("./files.db");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            dbManager.close();
        }));

        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route("/*").handler(StaticHandler.create());

        router
                .route(HttpMethod.GET, "/new-bin")
                .handler(ctx -> {
                    String binID;
                    do {
                        binID = UUID.randomUUID().toString();
                    } while (dbManager.binExists(binID));

                    ctx.redirect("/" + binID);
                });

        router
                .route(HttpMethod.GET, "/test")
                .handler(ctx -> {
                    System.out.println("test");

                    // Do something with them...
                });

        router
                .route(HttpMethod.GET, "/:binID")
                .handler(ctx -> {
                    String binID = ctx.pathParam("binID");

                    if (ctx.request().getHeader("Accept") != null && ctx.request().getHeader("Accept").equalsIgnoreCase("application/json")) {
                        if (!dbManager.binExists(binID)) {
                            status(ctx, "Bin doesn't exist.", 404);
                            return;
                        }

                        ArrayList<HashMap<String, Object>> fileList = new ArrayList<>();

                        Bin bin = dbManager.getBin(binID);
                        for (final DBFile file : bin.files) {
                            HashMap<String, Object> fileMap = new HashMap<>();

                            String mimeType = null;
                            try {
                                mimeType = Files.probeContentType(Paths.get(file.name));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            fileMap.put("name", file.name);
                            fileMap.put("type", mimeType == null ? "application/octet-stream" : mimeType);
                            fileMap.put("size", file.size);

                            fileList.add(fileMap);
                        }

                        Map<String, Object> resultMap = new HashMap<>();

                        resultMap.put("files", fileList);
                        resultMap.put("creationDate", bin.creationDate.toString());

                        Gson gson = new Gson();
                        ctx.response().end(gson.toJson(resultMap));
                        return;
                    }

                    ctx.response().sendFile("web/bin.html");
                });


        router
                .route(HttpMethod.POST, "/:binID/:filename")
                .handler(ctx -> {
                    String binID = ctx.pathParam("binID");
                    String filename = ctx.pathParam("filename");

                    if (!binID.matches(ALLOWED_BIN_REGEX)) {
                        status(ctx, "Invalid bin name", 400);
                        return;
                    }
                    if (binID.length() < MIN_BIN_NAME_LENGTH) {
                        status(ctx, "Bin name is too short", 400);
                        return;
                    }
                    if (dbManager.binExists(binID, filename)) {
                        status(ctx, "File already exists.", 409);
                        return;
                    }

                    ctx.next();
                });
        router.route().handler(BodyHandler.create());
        router
                .route(HttpMethod.POST, "/:binID/:filename")
                .handler(ctx -> {
                    System.out.println("test2");

                    String binID = ctx.pathParam("binID");
                    String filename = ctx.pathParam("filename");

                    System.out.println(ctx.fileUploads().size());
                    if (ctx.fileUploads().size() != 1) {
                        for (FileUpload fileUpload : ctx.fileUploads()) {
                            FileSystem fileSystem = ctx.vertx().fileSystem();
                            if (!fileUpload.cancel()) {
                                String uploadedFileName = fileUpload.uploadedFileName();
                                fileSystem.delete(uploadedFileName);
                            }
                        }
                        status(ctx, "Wrong amount of files.", 400);
                        return;
                    }

                    FileUpload uploadedFile = ctx.fileUploads().get(0);
                    dbManager.addFileToBin(binID, filename, uploadedFile.uploadedFileName(), uploadedFile.size());

                    status(ctx, "Successfully uploaded the file.", 200);
                });

        router
                .route(HttpMethod.GET, "/:binID/:filename")
                .handler(ctx -> {
                    String binID = ctx.pathParam("binID");
                    String filename = ctx.pathParam("filename");

                    if (!dbManager.binExists(binID)) {
                        status(ctx, "Bin doesn't exist.", 404);
                        return;
                    }
                    DBFile file = dbManager.getFile(binID, filename);

                    if (file.localPath == null) {
                        status(ctx, "File doesn't exist.", 404);
                        return;
                    }

                    try {
                        String mimeType = Files.probeContentType(Paths.get(filename));
                        ctx.response().putHeader("Content-Type", mimeType == null ? "application/octet-stream" : mimeType);
                        ctx.response().putHeader("Content-Length", Long.toString(file.size));
                        ctx.response().sendFile(file.localPath);
                    } catch (FileNotFoundException e) {
                        status(ctx, "Error reading file.", 500);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        router
                .route(HttpMethod.DELETE, "/:binID/:filename")
                .handler(ctx -> {

                    String binID = ctx.pathParam("binID");
                    String filename = ctx.pathParam("filename");

                    if (!dbManager.binExists(binID, filename)) {
                        status(ctx, "Specified bin or file doesn't exist.", 404);
                        return;
                    }

                    dbManager.remove(binID, filename);

                    status(ctx, "Successfully removed file", 200);
                });

        router
                .route(HttpMethod.DELETE, "/:binID")
                .handler(ctx -> {

                    String binID = ctx.pathParam("binID");

                    if (!dbManager.binExists(binID)) {
                        status(ctx, "Specified bin doesn't exist.", 404);
                        return;
                    }

                    dbManager.removeBin(binID);

                    status(ctx, "Successfully removed bin", 200);
                });



        server.requestHandler(router).listen(7000);
        System.out.println("NadBin running on port 7000");
    }

    private static void status(RoutingContext ctx, String message, int code) {
        Gson gson = new Gson();
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("message", message);

        ctx.response().setStatusCode(code);
        ctx.response().putHeader("content-type", "application/json");
        ctx.response().end(gson.toJson(resultMap));
    }
}

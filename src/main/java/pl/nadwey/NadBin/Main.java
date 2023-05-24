package pl.nadwey.NadBin;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    static BinManager binManager;
    static ArrayList<String> reservedBins = new ArrayList<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        binManager = new BinManager("./files.db");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            binManager.close();
        }));

        serveResource(app, "web/index.html", "/");
        serveResource(app, "web/style.css", "/style.css");
        serveResource(app, "web/Quicksand.ttf", "/Quicksand.ttf");
        serveResource(app, "web/trash.svg", "/trash.svg");

        app.get("/new-bin", ctx -> {
            String binID;
            do {
                binID = UUID.randomUUID().toString();
            } while(binManager.binExists(binID));

            ctx.redirect("/" + binID);
        });
        reservedBins.add("new-bin");

        app.get("/{bin}/{file}", ctx -> {
            String binParam = ctx.pathParam("bin");
            String fileParam = ctx.pathParam("file");

            if (!binManager.binExists(binParam)) {
                fail(ctx, "Bin doesn't exist.", 404);
                return;
            }
            DBFile file = binManager.getFile(binParam, fileParam);

            if (file.localPath == null) {
                fail(ctx, "File doesn't exist.", 404);
                return;
            }

            try {
                String mimeType = Files.probeContentType(Paths.get(fileParam));
                ctx.header("Content-Disposition", "attachment; filename=\"" + fileParam + "\"");
                ctx.header("Content-Type", mimeType == null ? "application/octet-stream" : mimeType);
                ctx.result(new FileInputStream(file.localPath));
            } catch (FileNotFoundException e) {
                fail(ctx, "Error reading file.");
            }
        });

        app.post("/{bin}/{file}", ctx -> {
            String binParam = ctx.pathParam("bin");
            String fileParam = ctx.pathParam("file");

            if (!binParam.matches("^[a-zA-Z0-9-_]+$")) {
                fail(ctx, "Invalid bin name", 400);
                return;
            }

            UploadedFile uploadedFile = ctx.uploadedFile("file");
            if (uploadedFile == null) {
                fail(ctx, "No uploaded file", 400);
                return;
            }
            if (reservedBins.contains(binParam)) {
                fail(ctx, "Reserved bin name", 403);
                return;
            }
            if (binManager.binExists(binParam, fileParam)) {
                fail(ctx, "File with the same name already exists", 409);
                return;
            }

            String result = binManager.uploadFile(uploadedFile);

            binManager.addFileToBin(binParam, fileParam, result, uploadedFile.size());

            Gson gson = new Gson();
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("message", "Success");

            ctx.result(gson.toJson(resultMap));
        });

        app.delete("/{bin}/{file}", ctx -> {
            String binParam = ctx.pathParam("bin");
            String fileParam = ctx.pathParam("file");

            if (!binManager.binExists(binParam, fileParam)) {
                fail(ctx, "Specified bin or file doesn't exist.", 404);
                return;
            }

            binManager.remove(binParam, fileParam);
        });

        app.delete("/{bin}", ctx -> {
            String binParam = ctx.pathParam("bin");

            if (!binManager.binExists(binParam)) {
                fail(ctx, "Specified bin doesn't exist.", 404);
                return;
            }

            binManager.removeBin(binParam);
        });

        app.get("/{bin}", ctx -> {
            String binParam = ctx.pathParam("bin");

            if (!binParam.matches("^[a-zA-Z0-9-_]+$")) {
                fail(ctx, "Invalid bin name", 400);
                return;
            }

            if (ctx.header("Accept") != null && Objects.equals(ctx.header("Accept"), "application/json")) {
                if (!binManager.binExists(binParam)) {
                    fail(ctx, "Bin doesn't exist.", 404);
                    return;
                }

                ArrayList<HashMap<String, Object>> fileList = new ArrayList<>();

                Bin bin = binManager.getBin(binParam);
                for (final DBFile file : bin.files) {
                    HashMap<String, Object> fileMap = new HashMap<>();

                    String mimeType = Files.probeContentType(Paths.get(file.name));

                    fileMap.put("name", file.name);
                    fileMap.put("type", mimeType == null ? "application/octet-stream" : mimeType);
                    fileMap.put("size", file.size);

                    fileList.add(fileMap);
                }

                Map<String, Object> resultMap = new HashMap<>();

                resultMap.put("files", fileList);
                resultMap.put("creationDate", bin.creationDate.toString());

                Gson gson = new Gson();
                ctx.result(gson.toJson(resultMap));
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("bin", binParam);

            ctx.render("web/bin.mustache", model);
        });
    }

    private static void fail(Context ctx, String message, int code) {
        Gson gson = new Gson();
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("message", message);

        ctx.status(code);
        ctx.result(gson.toJson(resultMap));
    }

    private static void fail(Context ctx, String message) {
        fail(ctx, message, 500);
    }

    private static void serveResource(Javalin app, String path, String url) {
        Path filename = Paths.get(url);
        if (filename.getNameCount() > 0) {
            reservedBins.add(filename.getName(0).toString());
        }

        app.get(url, ctx -> {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream in = classloader.getResourceAsStream(path);
            ctx.contentType(Files.probeContentType(Paths.get(path)));
            if (in != null) ctx.result(in);
        });
    }
}
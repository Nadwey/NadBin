package pl.nadwey.NadBin;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main {
    static BTreeMap fileDB;
    static ArrayList<String> reservedBins = new ArrayList<>();
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        new File("./files.db").delete();
        DB dbMaker = DBMaker.fileDB("files.db").make();
        fileDB = dbMaker.treeMap("btree", Serializer.STRING, Serializer.JAVA).createOrOpen();

        serveResource(app, "web/index.html", "/");
        serveResource(app, "web/style.css", "/style.css");
        serveResource(app, "web/Quicksand.ttf", "/Quicksand.ttf");

        app.get("/{bin}/{file}", ctx -> {
            String binParam = ctx.pathParam("bin");
            String fileParam = ctx.pathParam("file");


            if (!fileDB.containsKey(binParam)) {
                fail(ctx, "Bin doesn't exist.");
                return;
            }
            Bin bin = (Bin)fileDB.get(binParam);

            String localPath = null;
            for (int i = 0; i < bin.files.size(); i++) {
                DBFile dbFile = bin.files.get(i);
                if (dbFile.name.equals(fileParam)) {
                    localPath = dbFile.localPath;
                }
            }

            if (localPath == null) {
                fail(ctx, "File doesn't exist.");
                return;
            }

            try {
                String mimeType = Files.probeContentType(Paths.get(fileParam));

                // ctx.header("Content-Disposition", "attachment; filename=\"" + fileParam + "\"");
                ctx.header("Content-Type", mimeType == null ? "application/octet-stream" : mimeType);

                ctx.result(new FileInputStream(localPath));
            } catch (FileNotFoundException e) {
                fail(ctx, "Error reading file.");
            }
        });

        app.post("/{bin}/{file}", ctx -> {
            String binParam = ctx.pathParam("bin");
            String fileParam = ctx.pathParam("file");

            UploadedFile uploadedFile = ctx.uploadedFile("file");
            if (uploadedFile == null) {
                fail(ctx, "No uploaded file");
                return;
            }
            if (reservedBins.contains(binParam)) {
                fail(ctx, "Reserved bin name");
                return;
            }
            if (binExists(binParam) && existsInBin(binParam, fileParam)) {
                fail(ctx, "File with the same name already exists");
                return;
            }

            String result = uploadFile(uploadedFile);

            addFileToBin(binParam, fileParam, result, uploadedFile.size());

            Gson gson = new Gson();
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("message", "Success");

            ctx.result(gson.toJson(resultMap));
        });

        app.get("/{bin}", ctx -> {
            String binParam = ctx.pathParam("bin");

            if (ctx.header("Accept") != null && ctx.header("Accept").equals("application/json")) {
                if (!fileDB.containsKey(binParam)) {
                    fail(ctx, "Bin doesn't exist.");
                    return;
                }

                Bin bin = (Bin)fileDB.get(binParam);

                ArrayList<HashMap<String, Object>> fileList = new ArrayList<>();

                for (int i = 0; i < bin.files.size(); i++) {
                    HashMap<String, Object> fileMap = new HashMap<>();

                    DBFile file = bin.files.get(i);
                    String mimeType = Files.probeContentType(Paths.get(file.name));

                    fileMap.put("name", file.name);
                    fileMap.put("type", mimeType == null ? "application/octet-stream" : mimeType);
                    fileMap.put("size", file.size);

                    fileList.add(fileMap);
                }

                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("files", fileList);
                Gson gson = new Gson();
                ctx.result(gson.toJson(resultMap));
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("bin", binParam);

            ctx.render("web/bin.mustache", model);
        });

        app.post("/upload", ctx -> {
            UploadedFile uploadedFile = ctx.uploadedFile("file");
            if (uploadedFile == null) {
                fail(ctx, "No uploaded file");
                return;
            }

            String bin;
            do {
                bin = UUID.randomUUID().toString();
            } while(fileDB.containsKey(bin));

            String result = uploadFile(uploadedFile);

            addFileToBin(bin, uploadedFile.filename(), result, uploadedFile.size());

            Gson gson = new Gson();
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("bin", bin);
            resultMap.put("message", "Success");

            ctx.result(gson.toJson(resultMap));
        });
    }

    /**
     * Uploads file
     *
     * @param uploadedFile
     * @return Local path of the uploaded file
     */
    private static String uploadFile(UploadedFile uploadedFile) throws IOException {
        File targetFile;

        // generate file until new id
        do {
            targetFile = Paths.get("files", UUID.randomUUID().toString()).toFile();
        } while(targetFile.exists());

        targetFile.getParentFile().mkdirs();

        // get the file from user
        InputStream uploadedFileContent = uploadedFile.content();
        OutputStream outStream = new FileOutputStream(targetFile);
        byte[] buffer = new byte[512 * 1024];
        int bytesRead;
        while ((bytesRead = uploadedFileContent.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        outStream.close();
        uploadedFileContent.close();

        return targetFile.toString();
    }

    private static void addFileToBin(String binID, String name, String localPath, long size) {
        Bin bin = null;
        if (binExists(binID))
            bin = (Bin)fileDB.get(binID);
        if (bin == null) bin = new Bin();

        if (bin.files.contains(name)) return;
        bin.files.add(new DBFile(name, localPath, size));
        fileDB.put(binID, bin);
    }

    private static boolean binExists(String binID) {
        return fileDB.containsKey(binID) && (fileDB.get(binID) != null);
    }

    private static boolean existsInBin(String binID, String name) {
        Bin bin = null;
        if (binExists(binID))
            bin = (Bin)fileDB.get(binID);
        else return false;

        for (final DBFile dbFile : bin.files) {
            if (dbFile.name.equals(name)) return true;
        }

        return false;
    }

    private static void fail(Context ctx, String message) {
        Gson gson = new Gson();
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("message", message);

        ctx.status(500);
        ctx.result(gson.toJson(resultMap));
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
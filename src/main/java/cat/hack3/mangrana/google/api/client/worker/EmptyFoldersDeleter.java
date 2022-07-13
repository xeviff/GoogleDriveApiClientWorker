package cat.hack3.mangrana.google.api.client.worker;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmptyFoldersDeleter {
    /** Application name. */
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /** Directory to store authorization tokens for this application. */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    Drive service;


    public static void main(String... args) throws GeneralSecurityException, IOException {
        EmptyFoldersDeleter main = new EmptyFoldersDeleter();
        String seriesEspFolderId = "1iwaTK5IHDWfK-Znkj_qMfc05l77fHhmO";
        main.iterateAndSeekEmptyFolders(seriesEspFolderId, "Series_esp");
    }

    public EmptyFoldersDeleter() throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = EmptyFoldersDeleter.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    @SuppressWarnings("unused, unchecked")
    private void listTD () throws IOException {
        List list = service.teamdrives().list().execute().getTeamDrives();
        list.stream().forEach(td -> td.toString());
    }

    void iterateAndSeekEmptyFolders (String parentId, String name) {
        log("lookup to folder: "+name);
        getChildrenById(parentId, true).forEach(gFile -> iterateAndSeekEmptyFolders(gFile.getId(), gFile.getName()));
        checkForEmptiness(parentId, name);
    }

    private void checkForEmptiness(String parentId, String name) {
        logc("now checking emptiness on folder: "+name);
        if (!getChildrenById(parentId, false).isEmpty())
            log(" ---- this folder is not empty");
        else {
            log(" !*** ELIGIBLE TO DELETE :D ---- DELETING ;) ");
            delete(parentId);
        }
    }

    private List<File> getChildrenById(String id, boolean onlyFolders)  {
        List<File> fullFileList = new ArrayList<>();

        String query =
                "trashed=false and "+
        (onlyFolders ? "mimeType = 'application/vnd.google-apps.folder' and " : "")+
                 "'"+id+"' in parents";

        String pageToken = null;
        do {
            try {
                FileList fileList = service.files()
                        .list()
                        .setQ(query)
                        .setIncludeItemsFromAllDrives(true)
                        .setSupportsTeamDrives(true)
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .setOrderBy("name")
                        .execute();

                fullFileList.addAll(fileList.getFiles());
                pageToken = fileList.getNextPageToken();
            } catch (IOException e) {
                log("ERROR during api call");
                e.printStackTrace();
            }
        } while (pageToken != null);

        return fullFileList;
    }

    private void delete (String id) {
        try {
            File newContent = new File();
            newContent.setTrashed(true);
            service.files()
                    .update(id, newContent)
                    .setSupportsTeamDrives(true)
                    .execute();
        } catch (IOException e) {
            log("ERROR during api call");
            e.printStackTrace();
        }
    }

    
    private void log(String msg) {
        System.out.println(msg);
    }

    private void logc(String msg) {
        System.out.print(msg);
    }

}
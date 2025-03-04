package dev.robingenz.capacitorjs.plugins.filepicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResult;
import androidx.annotation.Nullable;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

@CapacitorPlugin(name = "FilePicker")
public class FilePickerPlugin extends Plugin {

    public static final String ERROR_PICK_FILE_FAILED = "pickFiles failed.";
    public static final String ERROR_PICK_FILE_CANCELED = "pickFiles canceled.";
    private FilePicker implementation;

    public void load() {
        implementation = new FilePicker(this.getBridge());
    }

    @PluginMethod
    public void pickFiles(PluginCall call) {
        JSArray types = call.getArray("types", null);
        boolean multiple = call.getBoolean("multiple", false);
        String[] parsedTypes = parseTypesOption(types);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        if (multiple == false) {
            if (parsedTypes == null || parsedTypes.length < 1) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, "*/*");
            } else {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, parsedTypes);
            }
        }

        intent = Intent.createChooser(intent, "Choose a file");

        startActivityForResult(call, intent, "pickFilesResult");
    }

    @Nullable
    private String[] parseTypesOption(@Nullable JSArray types) {
        if (types == null) {
            return null;
        }
        try {
            List<String> typesList = types.toList();
            return typesList.toArray(new String[0]);
        } catch (JSONException exception) {
            Logger.error("parseTypesOption failed.", exception);
            return null;
        }
    }

    @ActivityCallback
    private void pickFilesResult(PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }
        boolean readData = call.getBoolean("readData", true);
        int resultCode = result.getResultCode();
        switch (resultCode) {
            case Activity.RESULT_OK:
                JSObject callResult = createPickFilesResult(result.getData(), readData);
                call.resolve(callResult);
                break;
            case Activity.RESULT_CANCELED:
                call.reject(ERROR_PICK_FILE_CANCELED);
                break;
            default:
                call.reject(ERROR_PICK_FILE_FAILED);
        }
    }

    private JSObject createPickFilesResult(@Nullable Intent data, boolean readData) {
        JSObject callResult = new JSObject();
        List<JSObject> filesResultList = new ArrayList<>();
        if (data == null) {
            callResult.put("files", JSArray.from(filesResultList));
            return callResult;
        }
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() == null) {
            Uri uri = data.getData();
            uris.add(uri);
        } else {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                uris.add(uri);
            }
        }
        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            JSObject fileResult = new JSObject();
            fileResult.put("path", implementation.getPathFromUri(uri));
            fileResult.put("name", implementation.getNameFromUri(uri));
            if (readData) {
                fileResult.put("data", implementation.getDataFromUri(uri));
            }
            fileResult.put("mimeType", implementation.getMimeTypeFromUri(uri));
            fileResult.put("size", implementation.getSizeFromUri(uri));
            filesResultList.add(fileResult);
        }
        callResult.put("files", JSArray.from(filesResultList.toArray()));
        return callResult;
    }
}

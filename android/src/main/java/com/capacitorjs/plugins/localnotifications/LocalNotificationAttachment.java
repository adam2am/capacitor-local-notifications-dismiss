package com.capacitorjs.plugins.localnotifications;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LocalNotificationAttachment {

    private String id;
    private String url;
    private JSONObject options;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONObject getOptions() {
        return options;
    }

    public void setOptions(JSONObject options) {
        this.options = options;
    }

    public static List<LocalNotificationAttachment> getAttachments(JSObject notification) {
        List<LocalNotificationAttachment> attachmentsList = new ArrayList<>();
        JSONArray attachments = null;
        try {
            attachments = notification.getJSONArray("attachments");
        } catch (Exception e) {
            // Attachments are optional, return empty list if not present
            Logger.debug(Logger.tags("LN"), "No attachments array found in notification");
            return attachmentsList;
        }
        
        if (attachments == null) {
            return attachmentsList;
        }

        for (int i = 0; i < attachments.length(); i++) {
            try {
                JSONObject jsonObject = attachments.getJSONObject(i);
                JSObject jsObject = JSObject.fromJSONObject(jsonObject);
                
                LocalNotificationAttachment newAttachment = new LocalNotificationAttachment();
                newAttachment.setId(jsObject.getString("id"));
                newAttachment.setUrl(jsObject.getString("url"));
                
                try {
                    newAttachment.setOptions(jsObject.getJSONObject("options"));
                } catch (JSONException e) {
                    // Options are optional for attachments
                    Logger.debug(Logger.tags("LN"), "No options found for attachment " + i);
                }
                
                attachmentsList.add(newAttachment);
            } catch (JSONException e) {
                // Skip malformed attachment and continue
                Logger.warn(Logger.tags("LN"), "Could not parse attachment at index " + i + ", skipping: " + e.getMessage());
            }
        }

        return attachmentsList;
    }
}

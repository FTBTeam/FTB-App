package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.instance.InstanceCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.UUID;

public class InstanceCategoryHandler implements IMessageHandler<InstanceCategoryHandler.Data> {
    @Override
    public void handle(Data data) {
        var categoryUuid = data.categoryId == null ? null : UUID.fromString(data.categoryId);
        
        if (data.action == Action.GET) {
            var categories = Instances.categories();
            WebSocketHandler.sendMessage(new Reply(data, true, "...", categories));
        } else if (data.action == Action.DELETE && categoryUuid != null) {
            Instances.removeCategory(categoryUuid);
            WebSocketHandler.sendMessage(new Reply(data, true, "Category deleted successfully"));
        } else if (data.action == Action.CREATE) {
            if (data.categoryName == null || data.categoryName.isEmpty()) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Category name cannot be empty"));
                return;
            }
            
            var result = Instances.addCategory(data.categoryName);
            if (result == null) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Failed to create category"));
                return;
            }
            
            WebSocketHandler.sendMessage(new Reply(data, true, "Category created successfully", result));
        } else if (data.action == Action.RENAME) {
            if (categoryUuid == null || data.categoryName == null || data.categoryName.isEmpty()) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Category ID and name cannot be empty"));
                return;
            }
            
            var result = Instances.updateCategory(categoryUuid, data.categoryName);
            if (result == null) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Failed to rename category"));
                return;
            }
            
            WebSocketHandler.sendMessage(new Reply(data, true, "Category renamed successfully", result));
        } else {
            WebSocketHandler.sendMessage(new Reply(data, false, "Invalid action specified"));
        }
    }

    public static class Data extends BaseData {
        public Action action;
        
        @Nullable
        public String categoryId;
        
        @Nullable
        public String categoryName;
    }
    
    private static class Reply extends BaseData {
        public boolean success;
        public String message;
        
        @Nullable
        public InstanceCategory category;
        
        @Nullable
        public LinkedList<InstanceCategory> categories;
        
        public Reply(Data data, boolean success, String message) {
            this.type = "instanceCategoryReply";
            this.requestId = data.requestId;
            this.success = success;
            this.message = message;
            this.category = null; // Default to null, set if applicable
        }
        
        public Reply(Data data, boolean success, String message, @NotNull InstanceCategory category) {
            this(data, success, message);
            this.category = category;
        }
        
        public Reply(Data data, boolean success, String message, @NotNull LinkedList<InstanceCategory> categories) {
            this(data, success, message);
            this.categories = categories;
        }
    }
    
    public enum Action {
        GET,
        CREATE,
        DELETE,
        RENAME
    }
}

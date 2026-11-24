package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.instance.InstanceCategory;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.UUID;

public class InstanceCategoryHandler implements IMessageHandler<InstanceCategoryHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        var categoryUuid = data.categoryId == null ? null : UUID.fromString(data.categoryId);
        
        var instances = context.services().instances();
        
        if (data.action == Action.GET) {
            var categories = instances.categories();
            context.reply(new Reply(data, true, "...", categories));
        } else if (data.action == Action.DELETE && categoryUuid != null) {
            instances.removeCategory(categoryUuid);
            context.reply(new Reply(data, true, "Category deleted successfully"));
        } else if (data.action == Action.CREATE) {
            if (data.categoryName == null || data.categoryName.isEmpty()) {
                context.reply(new Reply(data, false, "Category name cannot be empty"));
                return;
            }
            
            var result = instances.addCategory(data.categoryName);
            if (result == null) {
                context.reply(new Reply(data, false, "Failed to create category"));
                return;
            }
            
            context.reply(new Reply(data, true, "Category created successfully", result));
        } else if (data.action == Action.RENAME) {
            if (categoryUuid == null || data.categoryName == null || data.categoryName.isEmpty()) {
                context.reply(new Reply(data, false, "Category ID and name cannot be empty"));
                return;
            }
            
            var result = instances.updateCategory(categoryUuid, data.categoryName);
            if (result == null) {
                context.reply(new Reply(data, false, "Failed to rename category"));
                return;
            }
            
            context.reply(new Reply(data, true, "Category renamed successfully", result));
        } else {
            context.reply(new Reply(data, false, "Invalid action specified"));
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
        
        public Reply(Data data, boolean success, String message, InstanceCategory category) {
            this(data, success, message);
            this.category = category;
        }
        
        public Reply(Data data, boolean success, String message, LinkedList<InstanceCategory> categories) {
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

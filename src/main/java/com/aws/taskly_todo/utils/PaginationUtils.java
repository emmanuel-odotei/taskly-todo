package com.aws.taskly_todo.utils;

import java.util.HashMap;
import java.util.Map;

public class PaginationUtils {
    /**
     * Extracts the second-to-last key from the token stack (for previous page)
     * and returns an updated stack with the last token removed.
     */
    public static Map<String, String> getPreviousPageToken(String tokenStack) {
        Map<String, String> result = new HashMap<>();
        
        if (tokenStack == null || tokenStack.isEmpty()) {
            result.put("lastKey", null);
            result.put("tokenStack", null);
            return result;
        }
        
        String[] tokens = tokenStack.split(",");
        if (tokens.length == 1) {
            // If only one token, previous page is the first page (no lastKey needed)
            result.put("lastKey", ""); // Use empty string instead of null
            result.put("tokenStack", null);
            return result;
        }
        
        // Get the second-to-last key (this becomes the lastKey for previous page)
        String prevLastKey = tokens[tokens.length - 2];
        
        // Build new token stack WITHOUT the last two tokens
        // The new stack should only contain tokens before the previous page
        StringBuilder newStack = new StringBuilder();
        for (int i = 0; i < tokens.length - 2; i++) {
            if (i > 0) newStack.append(",");
            newStack.append(tokens[i]);
        }
        
        result.put("lastKey", prevLastKey);
        result.put("tokenStack", !newStack.isEmpty() ? newStack.toString() : null);
        return result;
    }
}

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {
    public static void main(String[] args){
        
        if (args.length != 2 || !args[0].equals("-E")) {
            System.out.println("Usage: ./your_program.sh -E <pattern>");
            System.exit(1);
        }

        String pattern = args[1];  
        Scanner scanner = new Scanner(System.in);
        String inputLine = scanner.nextLine();

        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.err.println("Logs from your program will appear here!");

        if (matchPattern(inputLine, pattern)) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
    
    private static final Map<String, Predicate<Character>> PATTERN_MAP = new HashMap<>();
    static {
        PATTERN_MAP.put("\\d", Character::isDigit);
        PATTERN_MAP.put("\\w", ch -> Character.isLetterOrDigit(ch) || ch == '_');
        PATTERN_MAP.put("\\s", Character::isWhitespace);
        PATTERN_MAP.put(".", ch -> true); // matches any character
    }
    
    public static String[] splitPattern(String pattern) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        
        while (i < pattern.length()) {
            if (pattern.charAt(i) == '\\' && i + 1 < pattern.length()) {
                // Escape sequence like \d, \w
                tokens.add(pattern.substring(i, i + 2));
                i += 2;
            } else if (pattern.charAt(i) == '[') {
                // Character class like [abc]
                int end = pattern.indexOf(']', i + 1);
                if (end == -1) {
                    // Malformed character class, treat as literal
                    tokens.add(String.valueOf(pattern.charAt(i)));
                    i++;
                } else {
                    tokens.add(pattern.substring(i, end + 1));
                    i = end + 1;
                }
            } else {
                // Regular character (including ^ and $)
                tokens.add(String.valueOf(pattern.charAt(i)));
                i++;
            }
        }
        
        return tokens.toArray(new String[0]);
    }
    
    public static boolean matchesToken(char ch, String token) {
        // Check if it's a known pattern
        if (PATTERN_MAP.containsKey(token)) {
            return PATTERN_MAP.get(token).test(ch);
        }
        
        // Handle character classes [abc] or [^abc]
        if (token.startsWith("[") && token.endsWith("]")) {
            if (token.length() <= 2) {
                return false; // Empty character class
            }
            
            if (token.charAt(1) == '^') {
                // Negated class [^abc]
                String forbidden = token.substring(2, token.length() - 1);
                return forbidden.indexOf(ch) < 0;
            } else {
                // Normal class [abc]
                String allowed = token.substring(1, token.length() - 1);
                return allowed.indexOf(ch) >= 0;
            }
        }
        
        // Literal character
        return ch == token.charAt(0);
    }
    
    public static boolean matchPattern(String input, String pattern) {
        if (pattern.isEmpty()) {
            return true; // Empty pattern matches everything
        }
        
        String[] tokens = splitPattern(pattern);
        if (tokens.length == 0) {
            return true;
        }
        
        // Determine anchor types
        boolean startAnchor = tokens.length > 0 && tokens[0].equals("^");
        boolean endAnchor = tokens.length > 0 && tokens[tokens.length - 1].equals("$");
        
        // Extract the actual pattern tokens (without anchors)
        int startIdx = startAnchor ? 1 : 0;
        int endIdx = endAnchor ? tokens.length - 1 : tokens.length;
        
        if (startIdx >= endIdx) {
            // Pattern is just anchors (like "^$" or "^" or "$")
            if (startAnchor && endAnchor) {
                // "^$" - matches only empty string
                return input.isEmpty();
            }
            // Just "^" or "$" alone - matches any string
            return true;
        }
        
        String[] patternTokens = new String[endIdx - startIdx];
        System.arraycopy(tokens, startIdx, patternTokens, 0, patternTokens.length);
        
        // Handle different anchor combinations
        if (startAnchor && endAnchor) {
            // Both anchors: pattern must match entire string
            return matchExact(input, patternTokens);
        } else if (startAnchor) {
            // Start anchor: pattern must match from beginning
            return matchFromStart(input, patternTokens);
        } else if (endAnchor) {
            // End anchor: pattern must match at end
            return matchAtEnd(input, patternTokens);
        } else {
            // No anchors: pattern can match anywhere
            return matchAnywhere(input, patternTokens);
        }
    }
    
    private static boolean matchExact(String input, String[] patternTokens) {
        if (input.length() != patternTokens.length) {
            return false;
        }
        
        for (int i = 0; i < patternTokens.length; i++) {
            if (!matchesToken(input.charAt(i), patternTokens[i])) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean matchFromStart(String input, String[] patternTokens) {
        if (input.length() < patternTokens.length) {
            return false;
        }
        
        for (int i = 0; i < patternTokens.length; i++) {
            if (!matchesToken(input.charAt(i), patternTokens[i])) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean matchAtEnd(String input, String[] patternTokens) {
        if (input.length() < patternTokens.length) {
            return false;
        }
        
        int startPos = input.length() - patternTokens.length;
        for (int i = 0; i < patternTokens.length; i++) {
            if (!matchesToken(input.charAt(startPos + i), patternTokens[i])) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean matchAnywhere(String input, String[] patternTokens) {
        if (patternTokens.length == 0) {
            return true;
        }
        
        // Try matching at every possible position
        for (int start = 0; start <= input.length() - patternTokens.length; start++) {
            boolean matches = true;
            
            for (int i = 0; i < patternTokens.length; i++) {
                if (!matchesToken(input.charAt(start + i), patternTokens[i])) {
                    matches = false;
                    break;
                }
            }
            
            if (matches) {
                return true;
            }
        }
        
        return false;
    }
}
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
    
    // Class to represent a pattern token with optional quantifier
    static class PatternToken {
        String token;
        boolean hasPlus;
        boolean hasQuestion; // Add this for ? quantifier

        PatternToken(String token, boolean hasPlus, boolean hasQuestion) {
            this.token = token;
            this.hasPlus = hasPlus;
            this.hasQuestion = hasQuestion;
        }
    }
    
    public static PatternToken[] splitPattern(String pattern) {
        List<PatternToken> tokens = new ArrayList<>();
        int i = 0;
        
        while (i < pattern.length()) {
            String currentToken;
            
            if (pattern.charAt(i) == '\\' && i + 1 < pattern.length()) {
                // Escape sequence like \d, \w
                currentToken = pattern.substring(i, i + 2);
                i += 2;
            } else if (pattern.charAt(i) == '[') {
                // Character class like [abc]
                int end = pattern.indexOf(']', i + 1);
                if (end == -1) {
                    // Malformed character class, treat as literal
                    currentToken = String.valueOf(pattern.charAt(i));
                    i++;
                } else {
                    currentToken = pattern.substring(i, end + 1);
                    i = end + 1;
                }
            } else {
                // Regular character (including ^ and $)
                currentToken = String.valueOf(pattern.charAt(i));
                i++;
            }
            
            // Check for + quantifier
            boolean hasPlus = false;
            boolean hasQuestion = false; // Add this for ? quantifier
            if (i < pattern.length() && pattern.charAt(i) == '+') {
                hasPlus = true;
                i++;
            } else if (i < pattern.length() && pattern.charAt(i) == '?') {
                hasQuestion = true;
                i++;
            }

            tokens.add(new PatternToken(currentToken, hasPlus, hasQuestion));
        }
        
        return tokens.toArray(new PatternToken[0]);
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
        
        PatternToken[] tokens = splitPattern(pattern);
        if (tokens.length == 0) {
            return true;
        }
        
        // Determine anchor types
        boolean startAnchor = tokens.length > 0 && tokens[0].token.equals("^");
        boolean endAnchor = tokens.length > 0 && tokens[tokens.length - 1].token.equals("$");
        
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
        
        PatternToken[] patternTokens = new PatternToken[endIdx - startIdx];
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
    
    private static boolean matchExact(String input, PatternToken[] patternTokens) {
        return matchTokensAtPosition(input, 0, patternTokens, 0, input.length());
    }
    
    private static boolean matchFromStart(String input, PatternToken[] patternTokens) {
        return matchTokensAtPosition(input, 0, patternTokens, 0, -1);
    }
    
    private static boolean matchAtEnd(String input, PatternToken[] patternTokens) {
        // Try to match backwards from the end
        for (int start = input.length(); start >= 0; start--) {
            if (matchTokensAtPosition(input, start, patternTokens, 0, input.length())) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean matchAnywhere(String input, PatternToken[] patternTokens) {
        if (patternTokens.length == 0) {
            return true;
        }
        
        // Try matching at every possible position
        for (int start = 0; start <= input.length(); start++) {
            if (matchTokensAtPosition(input, start, patternTokens, 0, -1)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Core matching function that handles quantifiers
    private static boolean matchTokensAtPosition(String input, int inputPos, PatternToken[] patternTokens, int patternPos, int requiredEndPos) {
        // Base case: we've matched all pattern tokens
        if (patternPos >= patternTokens.length) {
            // If we require exact match to end, check position
            return requiredEndPos == -1 || inputPos == requiredEndPos;
        }
        
        PatternToken currentToken = patternTokens[patternPos];
        
        if (currentToken.hasPlus) {
            // Handle + quantifier (one or more)
            // First, we must match at least once
            if (inputPos >= input.length() || !matchesToken(input.charAt(inputPos), currentToken.token)) {
                return false; // Must match at least once
            }
            
            // Try matching as many times as possible (greedy)
            for (int matchCount = 1; inputPos + matchCount <= input.length(); matchCount++) {
                // Check if we can still match this character
                if (inputPos + matchCount > input.length() || 
                    !matchesToken(input.charAt(inputPos + matchCount - 1), currentToken.token)) {
                    // Can't match more, try with current count
                    if (matchTokensAtPosition(input, inputPos + matchCount - 1, patternTokens, patternPos + 1, requiredEndPos)) {
                        return true;
                    }
                    break;
                }
                
                // Try matching the rest of the pattern after consuming this many characters
                if (matchTokensAtPosition(input, inputPos + matchCount, patternTokens, patternPos + 1, requiredEndPos)) {
                    return true;
                }
            }
            
            return false;
        } else if (currentToken.hasQuestion) {
            // Handle ? quantifier (zero or one)
            // Try matching the token once
            if (inputPos < input.length() && matchesToken(input.charAt(inputPos), currentToken.token)) {
                // If it matches, try the rest of the pattern
                if (matchTokensAtPosition(input, inputPos + 1, patternTokens, patternPos + 1, requiredEndPos)) {
                    return true;
                }
            }
            // If it doesn't match or we skip it, try the rest of the pattern
            return matchTokensAtPosition(input, inputPos, patternTokens, patternPos + 1, requiredEndPos);
        } else {
            // Regular token (no quantifier)
            if (inputPos >= input.length() || !matchesToken(input.charAt(inputPos), currentToken.token)) {
                return false;
            }
            
            return matchTokensAtPosition(input, inputPos + 1, patternTokens, patternPos + 1, requiredEndPos);
        }
    }
}
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

    // Uncomment this block to pass the first stage
    
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
        PATTERN_MAP.put("^", ch -> false); // matches start of line, handled separately
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
                tokens.add(pattern.substring(i, end + 1));
                i = end + 1;
            } else {
                // Regular character
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
        String[] tokens = splitPattern(pattern);
         if (tokens.length > 0 && tokens[0].equals("^")) {
        if (input.length() < tokens.length - 1) {
            return false;
        }
        for (int i = 1; i < tokens.length; i++) {
            if (!matchesToken(input.charAt(i - 1), tokens[i])) {
                return false;
            }
        }
        return true;
    }
        // Try matching at every position in the input
        for (int start = 0; start <= input.length() - tokens.length; start++) {
            boolean matches = true;
            
            for (int i = 0; i < tokens.length; i++) {
                if (!matchesToken(input.charAt(start + i), tokens[i])) {
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

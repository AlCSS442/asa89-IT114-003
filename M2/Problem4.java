package M2;

public class Problem4 extends BaseClass {
    private static String[] array1 = { "hello world!", "java programming", "special@#$%^&characters", "numbers 123 456",
            "mIxEd CaSe InPut!" };
    private static String[] array2 = { "hello world", "java programming", "this is a title case test",
            "capitalize every word", "mixEd CASE input" };
    private static String[] array3 = { "  hello   world  ", "java    programming  ",
            "  extra    spaces  between   words   ",
            "      leading and trailing spaces      ", "multiple      spaces" };
    private static String[] array4 = { "hello world", "java programming", "short", "a", "even" };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfoBasic(arr, arrayNumber);

        // Challenge 1: Remove non-alphanumeric characters except spaces
        // Challenge 2: Convert text to Title Case
        // Challenge 3: Trim leading/trailing spaces and remove duplicate spaces
        // Result 1-3: Assign final phrase to `placeholderForModifiedPhrase`
        // Challenge 4 (extra credit): Extract up to middle 3 characters when possible (beginning starts at middle of phrase excluding the first and last characters),
        // assign to 'placeholderForMiddleCharacters'
        
        // if not enough characters assign "Not enough characters"
 
        // Step 1: sketch out plan using comments (include ucid and date)
        //ass89 09-29-2025
        /*
         I plan to use regex to replace the symbols and puncuation for the first challenge.
         For the second challenge, I will split the string into words by spaces. I will iterate through
         the words and capitalize the first character of each word and make the rest lowercase.
         Then recombine the words into a single string and trim the trailing space.
         For challenge 3, I will use regex again to trim the leading and trailing spaces and replace multiple
         spacesa with a single space.
         */
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        String placeholderForModifiedPhrase = "";
        String placeholderForMiddleCharacters = "";
        
        for(int i = 0; i <arr.length; i++){
            // Start Solution Edits
            
            //challenge 1, removing non-alphanumeric except spaces
            String s = arr[i];
            
            s = s.replaceAll("[^a-zA-Z0-9 ]","");
            
            //challenge 2, convert text to title case
            String[] words = s.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0))); // capitalize first char
                    if (word.length() > 1) {
                        sb.append(word.substring(1).toLowerCase()); // rest lowercase
                    }
                    sb.append(" ");
                }
            }
            placeholderForModifiedPhrase = sb.toString().trim();

            //Challenge 3
            s = s.trim().replaceAll("\\s+", " ");

            //Challenge 4
            String phrase = placeholderForModifiedPhrase;
            if (phrase.length() >= 3){
                int mid = phrase.length() / 2;
                int start = Math.max(mid -1, 0);
                int end = Math.min(start + 3, phrase.length());
                placeholderForMiddleCharacters = phrase.substring(start, end);
            }else{
                placeholderForMiddleCharacters = "Not enough characters!";
            }

             // End Solution Edits
            System.out.println(String.format("Index[%d] \"%s\" | Middle: \"%s\"",i, placeholderForModifiedPhrase, placeholderForMiddleCharacters));
        }

       

        
        System.out.println("\n______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "ass89"; // <-- change to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);
        printFooter(ucid, 4);
    }

}
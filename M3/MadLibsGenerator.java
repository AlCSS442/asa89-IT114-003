package M3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Random;

/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "mt85"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }
        List<String> lines = new ArrayList<>();
        // Start edits
        File[] files = folder.listFiles();
        if (files == null || files.length == 0){
            System.out.println("No story files found in the folder!");
            return;
        }
        // load a random story file
        Random random = new Random();
        // parse the story lines
        File storyFile = files[random.nextInt(files.length)];
        System.out.println("Using story file: " + storyFile.getName());
        System.out.println();
        try (Scanner fileScanner = new Scanner(storyFile)){
            while (fileScanner.hasNextLine()){
                lines.add(fileScanner.nextLine());
            }
        } catch (FileNotFoundException e){
            System.out.println("Error: Could not read the story file.");
            return;
        }
        // iterate through the lines
        for (int i = 0; i < lines.size(); i++){
            String line = lines.get(i);
                while (line.contains("<") && line.contains(">")){
                    int start = line.indexOf("<");
                    int end = line.indexOf(">", start);

                    String placeholder = line.substring(start + 1, end);

                    String prompt = placeholder.replace("_", " ");

                    System.out.println("Enter a " + prompt + ": ");
                    String userWord = scanner.nextLine();
                    line = line.substring(0,start) + userWord + line.substring(end + 1);
                }
                lines.set(i, line);
        }
        System.out.println("\nYour completed story:\n");
        for (String line : lines){
            System.out.println(line);
        }
        // prompt the user for each placeholder (note: there may be more than one
        // placeholder in a line)

        // apply the update to the same collection slot

        // End edits
        System.out.println("\nYour Completed Mad Libs Story:\n");
        StringBuilder finalStory = new StringBuilder();
        for (String line : lines) {
            finalStory.append(line).append("\n");
        }
        System.out.println(finalStory.toString());

        printFooter(ucid, 3);
        scanner.close();
    }
}

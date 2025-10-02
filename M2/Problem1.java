package M2;
import java.util.ArrayList;

public class Problem1 extends BaseClass {
    private static int[] array1 = {0,1,2,3,4,5,6,7,8,9};   
    private static int[] array2 = {9,8,7,6,5,4,3,2,1,0};
    private static int[] array3 = {0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9};
    private static int[] array4 = {9,9,8,8,7,7,6,6,5,5,4,4,3,3,2,2,1,1,0,0}; 
    private static void printOdds(int[] arr, int arrayNumber){
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfo(arr, arrayNumber);

        // Challenge: Print odd values only in a single line separated by commas
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        System.out.print("Output Array: ");
        // Start Solution Edits
        //ass89 09-28-2025
       /* I need an array thats able to grow in size--> will use ArrayList
       Plan: i need to filter out the array so only odd #'s remain. I choose an 
       arraylist b/c I want it to work for any sized array example thats passed through
       So, I will loop though the array and if its odd, it will be added to the ArrayList
       Note to self: I cant use println b/c it would print a new line per number
       Will need a check for commas (there can't be a leading or trailing comma)
       */
       ArrayList<Integer> odds = new ArrayList<>();
       for (int n : arr){
        if (n % 2 != 0){
            odds.add(n);
        }
       }
       //need to loop through ArrayList and print them
       for (int i = 0; i < odds.size(); i++){
        System.out.print(odds.get(i));
        if (i < odds.size() -1){
            System.out.print(",");
        }
       }   
        

        // End Solution Edits
        System.out.println("");
        System.out.println("______________________________________");
    }
    public static void main(String[] args) {
        final String ucid = "ass89"; // <-- change to your UCID
        // no edits below this line
        printHeader(ucid, 1);
        printOdds(array1,1);
        printOdds(array2,2);
        printOdds(array3,3);
        printOdds(array4,4);
        printFooter(ucid, 1);
        
    }
}
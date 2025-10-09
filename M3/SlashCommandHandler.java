package M3;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "ass89"; // <-- change to your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);

        // Can define any variables needed here
        

        while (true) {
            System.out.print("Enter command: ");
            String originalInput = scanner.nextLine().trim();//used for the echo part
            String input = originalInput.toLowerCase();
            String[] originalParts = originalInput.split(" ", 2);
            // get entered text
            String[] parts = input.split(" ", 2);
            

            // check if greet and process if greet
            if (parts[0].equals("/greet")){
                if (parts.length < 2 || parts[1].isBlank()){
                    System.out.println("Missing name! Valid: /greet <name>");
                    continue;          
                }else{
                    String name = parts[1].trim();
                    String firstLetter = name.substring(0,1).toUpperCase();
                    String restChar = name.substring(1).toLowerCase();
                    System.out.println("Hello, " + firstLetter + restChar + "!");
                    continue;
                }
            }

            // check if roll
            //need to handle:
            /*
            1. if user inputs a negative number
            2. invalid format
            3. is blank
            4. are numbers
            5. are whole numbers --> integer.parseInt
            6. whitespaces around numbers and extra characters--> trim
             */
            if (parts[0].equals("/roll")){
                if (parts.length < 2 || parts[1].isBlank()){
                    System.out.println("Invalid format! Valid:/roll <num>d<sides>");
                    continue;
                } 
                String[] dice = parts[1].split("d");
                if (dice.length != 2){
                    System.out.println("Invalid format! Valid:/roll <num>d<sides>");
                    continue;
                }
            
                    try{
                        int numberofDice = Integer.parseInt(dice[0].trim()); //cant cast string to integer, need this method
                        int numberofSides = Integer.parseInt(dice[1].trim());
                        if (numberofDice <= 0 || numberofSides <= 0) {
                            System.out.println("Numbers must be positive!");
                            continue;
                        }
                        int total = 0;
                        for (int i = 0; i < numberofDice; i++){
                        int roll = (int)(Math.random() * numberofSides) + 1;
                        System.out.println("Roll " + (i + 1) + ": " + roll);
                        total += roll;
                        }
                        System.out.println("Rolled " + numberofDice + "d" + numberofSides + " and got total: " + total);
                        continue;
                        
                    } catch (NumberFormatException e){
                        System.out.println("/roll parameters must be numbers!");
                        continue;
                    }
                }
         
            

            // check if echo
             if (parts[0].equals("/echo")) {
                if (parts.length < 2 || parts[1].isBlank()) {
                    System.out.println("Invalid formatting! Valid: /echo <message>");
                    continue;
                }
                System.out.println(originalParts[1]);
                continue;
            }

            // check if quit
            if (parts[0].equals("/quit")){
                break;
            }
            

            // handle invalid commnads
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}


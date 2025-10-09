package M3;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "ass89"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }
        //ass89, 10-5-2025
        double result = 0.0;

        try {
            System.out.println("Calculating result...");
            //declaring variables first
            double num1 = Double.parseDouble(args[0]);
            double num2 = Double.parseDouble(args[2]);
            String operator = args[1];
            
            // extract the equation (format is <num1> <operator> <num2>)
            switch (operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                default:
                    System.out.println("Unsupported operator. Use only + or -");
                    printFooter(ucid, 1);
                    return;
            }
            int decimals1 = args[0].contains(".") ? args[0].length() - args[0].indexOf('.') - 1 : 0;   
            int decimals2 = args[2].contains(".") ? args[2].length() - args[2].indexOf('.') - 1 : 0; 
            int maxDecimals = Math.max(decimals1, decimals2);
            
            System.out.printf("Result: %." + maxDecimals + "f\n", result);

        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
            return;
        }    
        printFooter(ucid, 1);
    }
}

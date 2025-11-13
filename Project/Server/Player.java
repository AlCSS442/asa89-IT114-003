//ass89
// going to create a file to represent a player in the hangman game

package Project.Server;

public class Player {
    private String name;
    private int id;
    private int points;
    private int strikes;

    public Player(String name, int id){
        this.name = name;
        this.id = id;
        this.points = 0;
        this.strikes = 0;   
    }
    // the getters for player class
    public String getName(){
        return name;
    }

    public int getId(){
        return id;
    }

    public int getPoint(){
        return points;
    }

    public int getStrikes(){
        return strikes;
    }

    //methods for player class
    public void addPoints(int amount){
        points += amount;
    }

    public void addStrike(){
        strikes++;
    }

    public void reset(){
        points = 0;
        strikes = 0;
    }

    //override method to format the string output how i like it to be
    @Override
    public String toString(){
        return name + " (Points: " + points + ", Strikes: " + strikes + ")";
    }
}

/**
 * @title Northwest Lumber Company Simulation
 * @author Bobby Purcell
 * @description Prints to an output file the results of a 50 year simulation forest being kept by a lumber company.
 * Simulation runs four 50 year runs to test various techniques to improve the yield of trees cut each year.
 * Techniques types are as follows:
 * 1- Cloud seeding to increase rain fall.
 * 2- Spray three year old trees to prevent beetle infestation deaths.
 * 3- Spray 50% of three year old trees and 50% of four year old trees to prevent beetle deaths.
 * 4- Seed clouds and spray 50% of four year old trees.
 */

package CSC318.TimeStep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class Main {

    public static void main(String[] args) {

        //Initialize PrintWriter to output the simulation results
        PrintWriter output;
        try {
            output = new PrintWriter(new File("SimulationOutput.txt"));

            // starting variables
            int year, tech, currentYearRainfall;
            final int YEARSTORUN = 50;
            //starting objects
            Rain rainMaker = new Rain();
            ForestFire forestFire = new ForestFire();
            Beetles beetles = new Beetles();
            ForestryCompany company = new ForestryCompany();


            /*
            -------------The Simulation loop----------------
            for each technique, run the simulation loop
            */
            for (tech = 1; tech <= 4; tech++) {
                Forest forest = new Forest();
                System.out.println("\nNow Running Technique: " + tech);
                //the simulation loop runs for 50 years, calculates statistics for each year
                for (year = 1; year <= YEARSTORUN; year++) {
                    currentYearRainfall = rainMaker.getRainfall((tech == 1 || tech == 4)); //seed clouds on 1st and 4th
                    forest.Wither(currentYearRainfall);//drought deaths, determines age mults for grow
                    company.Spray(forest.acreage, forest.sprayedAcreage, tech); //company sprays trees to guard against bugs
                    beetles.Eat(forest.acreage, currentYearRainfall); //kill trees from beetles eating
                    forestFire.Burn(forest.acreage, forest.sprayedAcreage, tech == 1 || tech == 4);//fire burns things.

                    company.Harvest(forest.acreage, forest.sprayedAcreage, tech); //harvest five year old trees
                    forest.Grow();//age the trees in the forest that survived based on rainfall
                    company.Plant(forest.acreage);  //plant one year olds to replace all lost/harvested trees

                }
                company.CalculateStats((double) YEARSTORUN, tech, output); //calculate and print statistics
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        System.exit(0);
    }


}//end main

// does Forestry company actions, sprays trees, harvest, plants, and keeps the books
class ForestryCompany {
    //statistics tracking variables
    long currentYearsHarvest;
    long currentYearsLoss;
    long[] sumOfHarvestsByTech;
    long[] sumOfHarvestsByTech2;
    long[][] sumOfTreesByTechByYear;
    long[][] sumOfTreesByTechByYear2;

    public ForestryCompany() {

        sumOfHarvestsByTech = new long[4];
        sumOfHarvestsByTech2 = new long[4];
        sumOfTreesByTechByYear = new long[4][4];
        sumOfTreesByTechByYear2 = new long[4][4];
    }

    //Calculates and prints some stats for output each run
    public void CalculateStats(double yearsRan, int tech, PrintWriter output) {
        double avgHarvest, harvestVariance;
        double[] avgAcreageByYear = new double[4], acreageVarianceByYear = new double[4];
        //harvest avg and variance
        avgHarvest = sumOfHarvestsByTech[tech - 1] / yearsRan;
        harvestVariance = sumOfHarvestsByTech2[tech - 1] / yearsRan - avgHarvest * avgHarvest;
        //acreage avg and variance for each year
        for (int i = 0; i < 4; i++) {
            avgAcreageByYear[i] = sumOfTreesByTechByYear[tech - 1][i] / yearsRan;
            acreageVarianceByYear[i] = sumOfTreesByTechByYear2[tech - 1][i] / yearsRan - (avgAcreageByYear[i] * avgAcreageByYear[i]);
        }

        //formatting util for fancier number output
        DecimalFormat df = new DecimalFormat("#,###,###.00");
        //PrintWriter output
        output.printf("Forestry Policy %d:\n\tAverage Harvest(per year): %s \tHarvest Variance: %s\n",
                tech, df.format(avgHarvest), df.format(harvestVariance));

        output.printf("Population Information:\n" +
                        " \tAverage One years:   %s \t\t Variance: %s\n" +
                        " \tAverage Two years:   %s \t\t Variance: %s\n" +
                        " \tAverage Three years: %s \t\t Variance: %s\n" +
                        " \tAverage Four years:  %s \t\t Variance: %s\n"
                , df.format(avgAcreageByYear[0]), df.format(acreageVarianceByYear[0]),
                df.format(avgAcreageByYear[1]), df.format(acreageVarianceByYear[1]),
                df.format(avgAcreageByYear[2]), df.format(acreageVarianceByYear[2]),
                df.format(avgAcreageByYear[3]), df.format(acreageVarianceByYear[3])
        );
        output.flush();

    }

    public void Spray(long[] acreage, long[] sprayedAcreage, int tech) {
        long half3s;
        long half4s;
        //spray differently based on the current forest management policy/technique
        switch (tech) {
            case 1:
                //not spraying in the first mgmt technique
                break;
            case 2: //spray all three year olds
                //removes from acreage array and put into sprayed array
                sprayedAcreage[2] = acreage[2];
                acreage[2] = 0;
                break;
            case 3: //spray half the 3 and 4 years old trees
                half3s = Math.round(.5 * acreage[2]);
                half4s = Math.round(.5 * acreage[3]);
                //take 50% of the 3 year olds out of acreage and put into sprayed
                sprayedAcreage[2] = half3s;
                acreage[2] = half3s;
                //take 50% of the 4 year olds out of acreage and put into sprayed
                sprayedAcreage[3] = half4s;
                acreage[3] = half4s;
                break;
            case 4: //spray all three year olds
                //removes from acreage array and put into sprayed array
                half4s = Math.round(.5 * acreage[3]);
                sprayedAcreage[3] = half4s;
                acreage[3] = half4s;
                break;
        }
    }

    //harvest trees, calc stats
    public void Harvest(long[] acreage, long[] sprayedAcreage, int tech) {
        long currentTotalTrees = 0;
        //get this years harvest number and add to sum for technique
        currentYearsHarvest = sprayedAcreage[4] + acreage[4];
        sumOfHarvestsByTech[tech - 1] += currentYearsHarvest;

        //Sum Of HarvestsByTech squared
        long temp = currentYearsHarvest * currentYearsHarvest;
        sumOfHarvestsByTech2[tech - 1] += temp;

        //5 year olds are harvested, remove them
        acreage[4] = sprayedAcreage[4] = 0;

        //total acres of trees in the forest
        for (int i = 0; i < 4; i++) {
            currentTotalTrees += (acreage[i] + sprayedAcreage[i]);
            //running totals for each age group by technique
            sumOfTreesByTechByYear[tech - 1][i] += (acreage[i] + sprayedAcreage[i]);
            //sum of totals squared
            temp = ((acreage[i] + sprayedAcreage[i]) * (acreage[i] + sprayedAcreage[i]));
            sumOfTreesByTechByYear2[tech - 1][i] += temp;
        }
        currentYearsLoss = 1000000 - currentYearsHarvest - currentTotalTrees;
    }

    public void Plant(long[] acreage) {
        acreage[0] = currentYearsHarvest + currentYearsLoss;
    }

}//end ForestryCompany

//the forest itself, generates and holds acreage of trees, growth and a withering functionality
class Forest {
    //arrays to hold the acreage of trees by age, since sprayed and unsprayed trees have different behavior
    // they are kept completely separately
    public long[] acreage, sprayedAcreage;
    //arrays hold multipliers for a group of trees to be modified by in the growth method
    //ex: the number of trees that age from 3 to 4 is (#3yearolds * deathMult[2] * growthMult[2])
    private double[] growthMult, deathMult;


    // No arg constructor defines start state of the forest
    public Forest() {
        this.acreage = new long[]{400000, 300000, 200000, 100000, 0};
        this.sprayedAcreage = new long[5];
        this.deathMult = new double[4];
        this.growthMult = new double[4];
    }

    //method sets the growth and death multipliers that modifiy the number of trees that age in the grow method
    // multipliers vary based on years rainfall
    void SetTreeMultipliers(int rainfall) {
        if (rainfall >= 1 && rainfall <= 3) {//light rainfall (drought)
            growthMult[0] = .3;
            growthMult[1] = .25;
            growthMult[2] = .4;
            growthMult[3] = .35;
            deathMult[0] = .9;
            deathMult[1] = .9;
            deathMult[2] = .7;
            deathMult[3] = .65;
        } else if ((rainfall >= 4) && (rainfall <= 10)) {//moderate rainfall
            growthMult[0] = .99;
            growthMult[1] = .98;
            growthMult[2] = .98;
            growthMult[3] = .99;
            deathMult[0] = .95;
            deathMult[1] = .95;
            deathMult[2] = .95;
            deathMult[3] = .95;
        } else {//rainfall is greater than 10
            growthMult[0] = .98;
            growthMult[1] = .97;
            growthMult[2] = .97;
            growthMult[3] = .96;
            deathMult[0] = .98;
            deathMult[1] = .97;
            deathMult[2] = .97;
            deathMult[3] = .96;
        }
    }

    //kill trees with drought
    void Wither(int rainfall) {
        SetTreeMultipliers(rainfall);
        for (int i = 0; i < 4; i++) {
            acreage[i] = Math.round(acreage[i] * deathMult[i]);
            sprayedAcreage[i] = Math.round(sprayedAcreage[i] * deathMult[i]);
        }
    }

    //Ages The trees
    void Grow() {
        for (int i = 4; i > 0; i--) {
            acreage[i] = Math.round(acreage[i - 1] * growthMult[i - 1]);
            sprayedAcreage[i] = Math.round(sprayedAcreage[i - 1] * growthMult[i - 1]);
        }
        //one year olds aged, get rid of them
        acreage[0] = sprayedAcreage[0] = 0;
    }
}//end forest

//Represents the yearly forest fires
class ForestFire {
    //a multiplier to be multiplied into the number of trees to remove
    //a certain percentage of them
    private double fireMult;

    public ForestFire() {
        fireMult = 0.0;
    }

    //generate the fire multiplier, multiply the acreage to burn the forest
    void Burn(long[] acreage, long[] sprayedAcreage, boolean seeded) {
        GenerateFireMult(seeded);
        for (int i = 0; i < 4; i++) {
            acreage[i] = Math.round(acreage[i] * getFireMult());
            sprayedAcreage[i] = Math.round(sprayedAcreage[i] * getFireMult());
        }
    }

    void GenerateFireMult(boolean seeded) {
        double burnMult;
        int x;//random variant
        x = (int) (Math.random() * 100); //random whole number between 0-100
        //treat whole num x as percentage range, evaluate based on occurrence values specified in write up
        if (!seeded) {
            if (x <= 65)     //65% chance
                burnMult = 1.0;
            else if (x <= 80)//15% chance
                burnMult = .9;
            else if (x <= 90)//10% chance
                burnMult = .85;
            else             //10% chance
                burnMult = .7;
        } else {
            if (x <= 75)     //75% chance
                burnMult = 1.0;
            else if (x <= 90)//15% chance
                burnMult = .95;
            else             //10% chance
                burnMult = .9;
        }
        setFireMult(burnMult);
    }

    public double getFireMult() {
        return fireMult;
    }

    public void setFireMult(double fireMult) {
        this.fireMult = fireMult;
    }
}//end forestFire

//Represents the beetles
class Beetles {
    //a multiplier to be multiplied into the number of trees to remove
    //a certain percentage of them due to beetle infestation
    private double[] beetleMult;

    public Beetles() {
        beetleMult = new double[4];
    }

    //sets the multipliers based on the current year's rainfall
    void SetBeetleMultipliers(int rainfall) {
        if (rainfall >= 1 && rainfall <= 3) {//light rainfall (drought)
            beetleMult[0] = 0.9;
            beetleMult[1] = 0.85;
            beetleMult[2] = 0.7;
            beetleMult[3] = 0.7;
        } else if ((rainfall >= 4) && (rainfall <= 10)) {//moderate rainfall
            beetleMult[0] = 0.95;
            beetleMult[1] = 0.95;
            beetleMult[2] = 0.9;
            beetleMult[3] = 0.9;
        } else {//rainfall is greater than 10
            beetleMult[0] = 0.0;
            beetleMult[1] = 0.0;
            beetleMult[2] = 0.98;
            beetleMult[3] = 0.98;
        }
    }

    //call method to generate multipliers, multiply into acreage to simulate beetle destruction
    public void Eat(long[] acreage, int currentYearRainfall) {
        SetBeetleMultipliers(currentYearRainfall);
        for (int i = 0; i < 4; i++) {
            acreage[i] = (int) Math.round(acreage[i] * beetleMult[i]);
        }
    }
}//end beetles


//Rain
// -class that holds years rainfall and the method to calculate it

class Rain {

    private int rainfall;

    //constructor, start demand at zero
    public Rain() {
        rainfall = 0;
    }

    //gets years rainfall
    //rainfall is calculated at random based on hard coded, weighted ratios for each possible rainfall qty


    public int getRainfall(boolean seeded) {
        int x; //random variant
        x = (int) (Math.random() * 100); //random whole number between 0-100
        //treat whole num x as percentage range, evaluate based on occurrence values specified in write up
        if (!seeded) {
            if (x <= 1) //1% chance
                rainfall = 1;
            else if (x <= 6)
                rainfall = 2;
            else if (x <= 11)
                rainfall = 3;
            else if (x <= 14)//3% chance
                rainfall = 4;
            else if (x <= 24)
                rainfall = 5;
            else if (x <= 39)
                rainfall = 6;
            else if (x <= 59)//20% chance
                rainfall = 7;
            else if (x <= 73)
                rainfall = 8;
            else if (x <= 83)
                rainfall = 9;
            else if (x <= 93)
                rainfall = 10;
            else if (x <= 98)
                rainfall = 11;
            else     //x > 98
                rainfall = 12;
            return rainfall;
        } else {
            //seeded rainfall, different chances for each value. There are better solutions than an if for this,
            // but in this case it doesn't matter really as this isn't meant to be maintained, modular, clean etc
            if (x <= 1)
                rainfall = 1;
            else if (x <= 2)
                rainfall = 2;
            else if (x <= 3)
                rainfall = 3;
            else if (x <= 5)
                rainfall = 4;
            else if (x <= 15)
                rainfall = 5;
            else if (x <= 25)
                rainfall = 6;
            else if (x <= 45)
                rainfall = 7;
            else if (x <= 65)
                rainfall = 8;
            else if (x <= 75)
                rainfall = 9;
            else if (x <= 85)
                rainfall = 10;
            else if (x <= 95)
                rainfall = 11;
            else     //x > 95
                rainfall = 12;
            return rainfall;
        }
    }
}//end Rain
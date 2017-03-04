
import com.sun.org.apache.bcel.internal.generic.POP;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import java.util.*;

/**
 * Created by thebaker on 2/24/17.
 */
public class Population {
    private final int POPULATION_SIZE;
    private final Random random = new Random();
    private PriorityQueue<Genome> currentpopulation;
    private List<Genome> elites = new ArrayList<>();
    private Comparator<Genome> fitnessComparator;
    private double crossoverRate;
    private double mutatationRate;
    private int currentId;
    private int crossOversForIteration;
    private Constants.SELECTION_MODE selection_mode;
    private Constants.CROSSOVER_MODE crossover_mode;
    private Constants.MUTATION_MODE mutation_mode;

    Population() {
        this.POPULATION_SIZE = 1000;
        new Population(this.POPULATION_SIZE, 0.5, 0.01,
                Constants.SELECTION_MODE.ROULETTE,
                Constants.CROSSOVER_MODE.ONE_POINT_CROSSOVER,
                Constants.MUTATION_MODE.MUTATION);
    }

    Population(int POPULATION_SIZE, double crossoverRate, double mutatationRate) {
        this.POPULATION_SIZE = POPULATION_SIZE;
        new Population(POPULATION_SIZE, crossoverRate, mutatationRate,
                Constants.SELECTION_MODE.ROULETTE,
                Constants.CROSSOVER_MODE.ONE_POINT_CROSSOVER,
                Constants.MUTATION_MODE.MUTATION);
    }

    Population(int POPULATION_SIZE, double crossoverRate, double mutatationRate,
               Constants.SELECTION_MODE selection_mode,
               Constants.CROSSOVER_MODE crossover_mode,
               Constants.MUTATION_MODE mutation_mode)
    {
        this.POPULATION_SIZE = POPULATION_SIZE;
        this.crossoverRate = crossoverRate;
        this.mutatationRate = mutatationRate;
        this.currentId = POPULATION_SIZE;
        this.selection_mode = selection_mode;
        this.crossover_mode = crossover_mode;
        this.mutation_mode = mutation_mode;
        fitnessComparator = new FitnessComparator();
        currentpopulation = new PriorityQueue<>(POPULATION_SIZE, fitnessComparator);
        generatePopulation();
        runGeneticAlgorithm(10);
    }

    /**
     * initialize currentPopulation with random genomes.
     */
    private void generatePopulation() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Genome genome = new Genome(random, i);
            Warrior.makeWarrior(genome);
            float fitness = CommandLine.fitness();
            if (fitness > 1) {
                System.out.println("Fitness: " + fitness);
                genome.printGenome();
            }
            genome.setFitness(fitness);
            currentpopulation.add(genome);
        }

    }

    /**
     * This will implement the genetic algorithm by running iterations over time using selection,
     * crossovers, and mutations

     */
    private void runGeneticAlgorithm(int iterations)
    {
        for(int i = 0; i < iterations; i++)
        {
            generateNewPopulation(selection_mode,
                    crossover_mode,
                    mutation_mode);
        }

        evaluatePopulation();
        Genome g1 = currentpopulation.poll();
        Genome g2 = currentpopulation.poll();
        System.out.println("Fitness" + g1.getFitness());
        g1.printGenome();
        System.out.println("Fitness" + g2.getFitness());
        g2.printGenome();

        int i = 0;
        for(Genome g : currentpopulation)
        {
            i++;
            if(g.getFitness() > 0)
            {
                System.out.println(i + " fitness:" + g.getFitness());
            }
        }
    }

    private void generateNewPopulation(Constants.SELECTION_MODE selection_mode,
                                       Constants.CROSSOVER_MODE crossover_mode,
                                       Constants.MUTATION_MODE mutation_mode) {
        crossOversForIteration = 0;
        List<Genome> selected = new ArrayList<>();
        elitism();

        switch (selection_mode) {
            case RANDOM:
                selected = selectRandom();
                break;
            case ROULETTE:
                selected = selectRoulette();
                break;
            case TOURNAMENT:
                selected = selectTournament();
                break;
            default:
                System.out.println("Invalid Selection Mode");
                break;
        }

        selected.addAll(elites);

        switch (crossover_mode) {
            case NO_CROSSOVER:
                break;
            case ONE_POINT_CROSSOVER:
                onePointCrossover(selected);
                break;
            case UNIFORM_CROSSOVER:
                uniformCrossover(selected);
                break;
            default:
                System.out.println("Invalid Crossover Mode");
                break;
        }

        switch (mutation_mode){
            case MUTATION:
                mutatePopulation();
                break;
            case NO_MUTATION:
                break;
            default:
                System.out.println("Invalid Mutation");
                break;
        }

        currentpopulation.addAll(elites);
        elites.clear();
        System.out.println("Current Population: " + currentpopulation.size());
        evaluatePopulation();
        //removeWorst(crossOversForIteration);
    }

    private void removeWorst(int numToRemove)
    {
        System.out.println("Removing: " + numToRemove);
        int i = 0;
        List<Genome> toRemove = new ArrayList<>();

        for(Genome g : currentpopulation)
        {
            if(i >= POPULATION_SIZE - numToRemove)
            {
                toRemove.add(g);
            }
            i++;
        }

        while(toRemove.size() > 0)
        {
            currentpopulation.remove(toRemove.remove(0));
        }
    }

    private void elitism()
    {
        int topPercent = (int)(POPULATION_SIZE*Constants.ELITISM);
        for (int i = 0; i < topPercent; i++) {
            elites.add(currentpopulation.poll());
        }
    }

    private void onePointCrossover(List<Genome> selection) {
        int length, selectionSize;
        boolean odd = false;
        List<int[]> g1, g2;
        Genome child1 = null, child2 = null;
        selectionSize = selection.size();

        if(selectionSize % 2 != 0)
        {
            odd = true;
        }

        if(selectionSize == 1)
        {
            return;
        }

        System.out.println("Selection Size:" + selectionSize);

        //Combines the genomes based on one-point crossover
        for(int i = 0; i < selectionSize; i+=2)
        {
            if(odd && i >= selectionSize - 2)
            {
                child2 = selection.remove(0);
            }
            else
            {
                child1 = selection.remove(0);
                child2 = selection.remove(0);
            }

            crossOversForIteration++;
            g1 = child1.getGenome();
            g2 = child2.getGenome();

            length = g1.size();
            if(g2.size() < length) length = g2.size();

            if(g1.size() == 1 && g2.size() == 1)
            {
                g1.add(g2.get(0));
                g2.add(g1.get(0));
            }
            else
            {
                for (int j = length / 2; j < length; j++) {
                    int[] holder1;
                    holder1 = g1.get(j);
                    g1.set(j, g2.get(j));
                    g2.set(j, holder1);
                }
            }
            if(g1 != null)
            {
                child1.setGenome(g1);
            }

            if(g2 != null)
            {
                child2.setGenome(g2);
            }
            currentpopulation.add(child1);
            currentpopulation.add(child2);
        }

    }

    private void uniformCrossover(List<Genome> selected) {

    }

    //Random chance of a genome getting chosen for crossover, probably wont use.
    private List<Genome> selectRandom() {
        List<Genome> winners = new ArrayList<>();
        for(Genome g : currentpopulation)
        {
            if(random.nextDouble() > 0.85)
            {
                winners.add(g);
            }
        }

        return winners;
    }

    //This gives each genome the probability of fitness/totalfitness of being chosen for crossover.
    private List<Genome> selectRoulette() {
        double totalFitness = 0;
        List<Genome> winners = new ArrayList<>();
        for (Genome g : currentpopulation) {
            totalFitness+=g.getFitness();
        }

        for (Genome g : currentpopulation) {
            if(g.getFitness() > 0) {
                double prob = g.getFitness() / totalFitness;
                if (random.nextDouble() < prob) {
                    winners.add(g);
                }
            }
            else
            {
                if(random.nextDouble() > 0.99)
                {
                    winners.add(g);
                }
            }
        }

        for(Genome g : winners)
        {
            currentpopulation.remove(g);
        }

        return winners;
    }

    //A tournament is ran, and the winners will be use in crossovers.
    private List<Genome> selectTournament() {
        List<Genome> winners = new ArrayList<>();


        return winners;
    }


    private void mutatePopulation()
    {
        int numberMutated = 0;
        List<Genome> holder = new ArrayList<>();
        int populationSize = currentpopulation.size();
        for(int j = 0; j < populationSize; j++)
        {
            Double r = random.nextDouble();
            Genome g = currentpopulation.poll();
            if(r > 1.0 - mutatationRate)
            {
                numberMutated++;
                g.mutateGenome();
            }
            holder.add(g);
        }
        System.out.println("Number of Genomes Mutated: " + numberMutated);
        currentpopulation.clear();
        currentpopulation.addAll(holder);
    }

    /**
     * Evaluates each genome in the current population
     */
    private void evaluatePopulation() {
        List<Genome> holder = new ArrayList<>();

        for(int i = 0; i < POPULATION_SIZE; i++) {
            Genome g = currentpopulation.poll();
            Warrior.makeWarrior(g);
            float fitness = CommandLine.fitness();
            g.setFitness(fitness);
            holder.add(g);
        }
        while(holder.size() > 0)
        {
            currentpopulation.add(holder.remove(0));
        }
    }

    protected class FitnessComparator implements Comparator<Genome> {
        @Override
        public int compare(Genome genome1, Genome genome2) {
            if (genome1.getFitness() < genome2.getFitness()) return 1;
            else if (genome1.getFitness() > genome2.getFitness()) return -1;
            return 0;
        }
    }

}

package wordCounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

class Globals {
    static ForkJoinPool fjPool = new ForkJoinPool();
}

/**
 * Read in a book-length text file. Count how many times each word occurs in the
 * file. Print a table of the 25 most frequent words and a table of the 25 least
 * frequent words. Both tables should be ordered most frequent to least
 * frequent.
 * 
 * @author kevinlee
 * 
 */
public class WordCounter extends RecursiveTask<HashMap<String, Integer>> {

    private HashMap<String, Integer> words = new HashMap<String, Integer>();
    private HashMap<Integer, Node> rankings = new HashMap<Integer, Node>();
    private final String PUNCTUATION = "[\\[\\]\"*^$#@+|`~()&/:_{}=,.;!?<>%]";
    private final int REPEATS = 1;
    private String wordsToCount;
    private int segments;
    private int chunkSize;
    private boolean isParent;
    private boolean printTime = true;
    private boolean printStats = true;

    /**
     * No-parameter constructor for initialising and unit testing
     */
    public WordCounter() {
    }

    /**
     * Parametered constructor for each thread (including main thread)
     * 
     * @param wordsToCount
     *            Words that are to be counted
     * @param isParent
     *            Whether the current thread is the parent thread or not
     * @param segments
     *            Number of threads to run
     * @param chunkSize
     *            Pre-calculated size of each word chunk
     */
    public WordCounter(String wordsToCount, boolean isParent, int segments, int chunkSize) {
        this.wordsToCount = wordsToCount;
        this.isParent = isParent;
        this.segments = segments;
        this.chunkSize = chunkSize;
    }

    /**
     * Main function
     */
    public static void main(String args[]) {
        new WordCounter().run();
    }

    /**
     * Prompts user to select file and kicks off word counting
     */
    private void run() {
        JFileChooser chooser = new JFileChooser();
        String text = openFile(chooser);
        if (text == null) {
            System.out.println("Invalid file. Please try.");
            System.exit(1);
        }
        System.out.println("1 segment:");
        this.countWords(text, 1);
        System.out.println("\n2 segments:");
        this.countWords(text, 2);
        System.out.println("\n4 segments:");
        this.countWords(text, 4);
        System.out.println("\n8 segments:");
        this.countWords(text, 8);
        System.out.println("\n16 segments:");
        this.countWords(text, 16);
        System.out.println("\n32,767 segments:");
        this.countWords(text, 32767);
        this.populateRankings(); // Populates the rankings list
        if (printStats) {
            System.out.println("\nSummary stats:");
            this.printStats();
        }
    }

    /**
     * Reads in text and counts the number of occurrences for each word
     * 
     * @param text
     *            Source text
     * @param numberOfSegments
     *            Number of segments to split task into
     */
    public void countWords(String text, int numberOfSegments) {
        int textLength = text.length();
        int sizeOfChunk = textLength / numberOfSegments;
        if (numberOfSegments == 1){
            
            this.wordsToCount = text;
            for (int i = 0; i < REPEATS; i++) {
                System.gc();
                long startTime = System.nanoTime();
                words = conductWordCounting();
                double currentRunningTime = ((double) (System.nanoTime() - startTime)) / 1000000;
                if (printTime) {
                    System.out.println("Running time: " + currentRunningTime + " ms");
                }
            }
        } else {
            for (int i = 0; i < REPEATS; i++) {
                words = Globals.fjPool.invoke(new WordCounter(text, true, numberOfSegments, sizeOfChunk));
            }            
        }
    }

    /**
     * If isParent == true, kicks off child threads. If isParent != true,
     * calculates the frequency of the words, given a string of text
     */
    @Override
    protected HashMap<String, Integer> compute() {
        if (isParent) { // Parent thread splits up texts and starts new child
                        // threads
            WordCounter[] wordCounterArray = new WordCounter[segments];
            int textLength = wordsToCount.length();
            int startIndex = 0;
            int endPoint = 0;

            wordCounterArray = this.splitTextUpAndLaunchForks(startIndex, textLength, endPoint, wordCounterArray);

            HashMap[] wordCounterResultsArray = new HashMap[segments]; // HashMap
                                                                       // array
                                                                       // containing
                                                                       // the
                                                                       // resultant
                                                                       // counts
                                                                       // of all
                                                                       // threads
            System.gc();
            long startTime = System.nanoTime();

            for (int i = 0; i < segments; i++) {
                if (i == 0) {
                    wordCounterResultsArray[i] = wordCounterArray[i].compute(); // 1st
                                                                                // segment
                                                                                // is
                                                                                // computed
                                                                                // by
                                                                                // own
                                                                                // thread,
                                                                                // and
                                                                                // results
                                                                                // returned
                } else {
                    wordCounterArray[i].fork(); // Other segments will be
                                                // computed by separate threads
                }
            }

            double currentRunningTime = ((double) (System.nanoTime() - startTime)) / 1000000;
            if (printTime) {
                System.out.println("Running time: " + currentRunningTime + " ms");
            }

            // Now rejoin all of the child threads
            for (int i = 1; i < segments; i++) {
                wordCounterResultsArray[i] = wordCounterArray[i].join();
            }

            return combineResults(wordCounterResultsArray);

        } else { // Child thread
            return this.conductWordCounting();
        }
    }

    /**
     * Helper function to split up text and launch forks
     * 
     * @param startIndex
     *            Starting index of substring
     * @param textLength
     *            The size of text chunk for each child thread
     * @param endPoint
     *            The index of the last word of the entire text
     * @param wordCounterArray
     *            An array of empty wordCounter objects
     * @return An array of initialised wordCounter objects
     */
    private WordCounter[] splitTextUpAndLaunchForks(int startIndex, int textLength, int endPoint,
            WordCounter[] wordCounterArray) {
        for (int i = 0; i < segments; i++) {
            String newText = "";
            if (i == segments - 1) { // Last segment gets text from startIndex
                                     // to the end
                newText = wordsToCount.substring(startIndex, textLength);
                wordCounterArray[i] = new WordCounter(newText, false, segments, chunkSize);
            } else { // Split text according to splitFactor
                endPoint = startIndex + chunkSize;

                while (wordsToCount.charAt(endPoint) != ' ') {
                    endPoint--; // Decrement splitPoint until it points to a
                                // space - ie end of a word
                }

                newText = wordsToCount.substring(startIndex, endPoint);
                wordCounterArray[i] = new WordCounter(newText, false, segments, chunkSize);

            }

            startIndex = endPoint;

        }
        return wordCounterArray;
    }

    /**
     * Helper method to remove unwanted punctuation, and count the frequency of
     * the words
     * 
     * @return A hashMap with the frequency of words
     */
    private HashMap<String, Integer> conductWordCounting() {
        HashMap<String, Integer> listingToReturn = new HashMap<String, Integer>();
        String punctuationLessText = removePunctuation(wordsToCount);
        StringTokenizer tokenizer = new StringTokenizer(punctuationLessText);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (listingToReturn.containsKey(token))
                listingToReturn.put(token, listingToReturn.get(token) + 1);
            else
                listingToReturn.put(token, 1);
        }

        return listingToReturn;

    }

    /**
     * Given an array of results, combine them
     * 
     * @param wordCounterResultsArray
     *            An array of results
     * @return A combined HashMap with all word counts
     */
    private HashMap<String, Integer> combineResults(HashMap[] wordCounterResultsArray) {
        HashMap<String, Integer> allResults = new HashMap<String, Integer>();

        for (int i = 0; i < wordCounterResultsArray.length; i++) {
            HashMap<String, Integer> results = wordCounterResultsArray[i];

            Set<String> keys = results.keySet();
            for (String key : keys) {
                int count = results.get(key);
                if (allResults.containsKey(key)) {
                    allResults.put(key, allResults.get(key) + count);
                } else {
                    allResults.put(key, count);
                }
            }

        }
        return allResults;

    }

    /**
     * Prints the 25 most frequent and 25 least frequent words
     */
    private void printStats() {
        int numberOfDifferentWords = this.numberOfDifferentWords();
        if (numberOfDifferentWords >= 25) {
            System.out.println("\nMost frequent words:");
            this.print(1, 25);
            System.out.println("\nLeast frequent words:");
            this.print(this.numberOfDifferentWords() - 24, this.numberOfDifferentWords());
        } else if (numberOfDifferentWords == 0) {
            System.out.println("No words found");
        } else {
            this.print(1, numberOfDifferentWords);

        }

    }

    /**
     * Returns the number of different words found
     * 
     * @return number of different words
     */
    public int numberOfDifferentWords() {
        return words.size();
    }

    /**
     * Returns an array of words, where 1 <= from <= to <= number of different
     * words, and 1 indicates the most frequent word
     * 
     * @param from
     *            Starting rank
     * @param to
     *            Ending rank
     * @return Array of strings in the range of the starting to ending rank
     * @throws RuntimeException
     *             if parameters are invalid
     */
    public String[] getWords(int from, int to) {
        if (to < from)
            throw new RuntimeException("'To' must be larger than 'From'");
        if (from < 1)
            throw new RuntimeException("'From' must be larger than 0");
        if (!this.rankings.containsKey(to))
            throw new RuntimeException("'To' is larger than the maximum ranking");

        ArrayList<String> words = new ArrayList<String>();
        for (int i = from; i <= to; i++) {
            Node node = this.rankings.get(i);
            words.add(node.getWordFromNode());
        }
        String[] wordsArray = new String[words.size()];
        for (int i = 0; i < words.size(); i++) {
            wordsArray[i] = words.get(i);
        }
        return wordsArray;
    }

    /**
     * Returns an array of word counts, where 1 <= from <= to <= number of
     * different words, and 1 indicates the most frequent word
     * 
     * @param from
     *            Starting rank
     * @param to
     *            Ending rank
     * @return Array of strings in the range of the starting to ending rank
     * @throws RuntimeException
     *             if parameters are invalid
     */
    public int[] getWordCounts(int from, int to) {
        if (to < from)
            throw new RuntimeException("'To' must be larger than 'From'");
        if (from < 1)
            throw new RuntimeException("'From' must be larger than 0");
        if (!this.rankings.containsKey(to))
            throw new RuntimeException("'To' is larger than the maximum ranking");

        ArrayList<Integer> count = new ArrayList<Integer>();
        for (int i = from; i <= to; i++) {
            Node node = this.rankings.get(i);
            count.add(node.getCount());
        }
        int[] countArray = new int[count.size()];
        for (int i = 0; i < count.size(); i++) {
            countArray[i] = count.get(i);
        }
        return countArray;
    }

    /**
     * Prints to-from+1 lines, where each line has the form rank count word
     * 
     * @param from
     *            Starting rank
     * @param to
     *            Ending rank
     * @throws RuntimeException
     *             if parameters are invalid
     */
    public void print(int from, int to) {
        if (to < from)
            throw new RuntimeException("'To' must be larger than 'From'");
        if (from < 1)
            throw new RuntimeException("'From' must be larger than 0");
        if (!this.rankings.containsKey(to))
            throw new RuntimeException("'To' is larger than the maximum ranking");
        for (int i = from; i <= to; i++) {
            Node node = this.rankings.get(i);
            System.out.printf("%d %d %s\n", i, node.getCount(), node.getWordFromNode());
        }
    }

    /**
     * Given a populated frequency count of words, populates a ranking hashmap
     */
    private void populateRankings() {
        if (words.size() == 0)
            return;
        ArrayList<Node> index = createNodeIndex(); // Turn frequency table into
                                                   // a reverse sorted arrayList
                                                   // of nodes (each node
                                                   // contains a string and its
                                                   // frequency)
        int rank = 1;
        for (Node node : index) {
            this.rankings.put(rank, node);
            rank++;
        }
    }

    /**
     * Create a reverse sorted arrayList of nodes
     * 
     * @return An arrayList of nodes sorted by counts (in reverse)
     */
    private ArrayList<Node> createNodeIndex() {
        ArrayList<Node> index = new ArrayList<Node>();
        Set<String> wordSet = words.keySet();

        for (String word : wordSet) {
            int frequency = words.get(word);
            Node node = new Node(word, frequency);
            index.add(node);
        }
        Collections.sort(index);
        Collections.reverse(index);
        return index;
    }

    /**
     * Takes a string, converts it to lowercase, and strips out all numbers and
     * punctuation (except ' and - that are sandwiched by characters) All
     * removed characters are replaced with an empty space
     * 
     * @param text
     *            Input string
     * @return A string with all punctuation stripped out
     */
    private String removePunctuation(String text) {
        if (text.length() == 0)
            return text;

        text = text.toLowerCase(); // converts into lowercase
        text = removeNumbers(text); // removes all numbers

        if (text.charAt(0) == '\'' || text.charAt(0) == '-') { // Remove leading
                                                               // ' or - in
                                                               // string
            text = text.substring(1);
        }

        if (text.charAt(text.length() - 1) == '\'' || text.charAt(text.length() - 1) == '-') { // Remove
                                                                                               // trailing
                                                                                               // '
                                                                                               // or
                                                                                               // -
                                                                                               // in
                                                                                               // string
            text = text.substring(0, text.length() - 1);
        }

        Pattern punctuation = Pattern.compile(PUNCTUATION); // removes all
                                                            // punctuation
                                                            // except '-' and
                                                            // '''
        String intermediate = punctuation.matcher(text).replaceAll(" ").trim();
        Pattern hyphensAndApostrophesEdgeCases = Pattern.compile("-\\W|\\W-|\\W'|'\\W");
        String intermediate2 = hyphensAndApostrophesEdgeCases.matcher(intermediate).replaceAll(" ").trim();
        Pattern hyphensAndApostrophes = Pattern.compile(" '|' | -|- ");
        return hyphensAndApostrophes.matcher(intermediate2).replaceAll(" ").trim();
    }

    /**
     * Takes a string, and strips out all the numbers
     * 
     * @param text
     *            The input string
     * @return A string without any numbers
     */
    private String removeNumbers(String text) {
        Pattern numbers = Pattern.compile("\\d");
        return numbers.matcher(text).replaceAll(" ").trim();
    }

    /**
     * Getter for words
     * 
     * @return Frequency of words
     */
    public HashMap<String, Integer> getWordsHashMap() {
        return this.words;
    }

    /**
     * Getter for ranking
     * 
     * @return The ranking hashmap
     */
    public HashMap<Integer, Node> getIndex() {
        return this.rankings;
    }

    /**
     * Opens JFileChooser for user to select graph
     * 
     * @return A string with the entire text
     */
    private static String openFile(JFileChooser chooser) {

        int option = chooser.showOpenDialog(null);

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (file != null) {
                    String fileName = file.getCanonicalPath();
                    FileReader fileReader = new FileReader(fileName);
                    BufferedReader reader = new BufferedReader(fileReader);
                    StringBuilder stringbuilder = new StringBuilder();
                    try {
                        boolean flag = true;
                        while (flag) {
                            String s = reader.readLine();
                            if (s == null) { // at end of file, bufferedReader
                                             // will return null
                                flag = false;
                            } else {
                                stringbuilder.append(s);
                            }
                        }
                        stringbuilder.append(" "); // Add a space to the end of
                                                   // the text for ease of
                                                   // marking
                        return stringbuilder.toString();
                    } catch (IOException e) {
                        return null;
                    } finally {
                        fileReader.close();
                    }
                } else
                    return null;
            } catch (IOException e) {
                return null;
            }
        } else
            return null;
    }

    /**
     * Inner class to hold the word and its associated frequency count
     * 
     */
    private class Node implements Comparable<Node> {

        private String word;
        private int count;

        public Node(String word, int count) {
            this.word = word;
            this.count = count;
        }

        public int getCount() {
            return this.count;
        }

        public String getWordFromNode() {
            return this.word;
        }

        @Override
        public int compareTo(Node o) {
            if (this.count > o.count)
                return 1;
            else if (this.count < o.count)
                return -1;
            else
                return 0;
        }

        @Override
        public String toString() {
            return this.word + ": " + this.count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null)
                return false;
            if (o instanceof Node) {
                Node that = (Node) o;
                return this.word.equals(that.word) && this.count == that.count;
            }
            return false;
        }

    }

}

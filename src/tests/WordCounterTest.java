package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import wordCounter.WordCounter;

public class WordCounterTest {

    WordCounter w1, w2;

    @Before
    public void setUp() throws Exception {
        w1 = new WordCounter();
        w2 = new WordCounter();
        w2.countWords(
                "'tis the way\n tis the day?\n It isn't the only way! When your program is executed, it should use a JFileChooser to select a file. The actual counting should be done by a public void countWords(String text, int numberOfSegments) method, which is I/O free and saves its result in a global variable of some type. The program should then print out the time required (in milliseconds) by the above call, along with the 25 most frequent and 25 least frequent words. (If ties occur, words with equal counts may occur in any order.)",
                1);

    }
    

    @Test
    public void testCountWords1() {
        w1.countWords("hello world helloo world", 2);
        assertEquals(2, (int) w1.getWordsHashMap().get("world"));
    }

    @Test
    public void testCountWords() {
        w1.countWords("hello world helloo world", 2);
        assertEquals(2, (int) w1.getWordsHashMap().get("world"));
        w1 = new WordCounter();
        w1.countWords("hello world-helloo world", 1);
        assertEquals(1, (int) w1.getWordsHashMap().get("world"));
        assertFalse(w1.getWordsHashMap().containsKey("helloo"));
        w1 = new WordCounter();
        w1.countWords("hello! world'helloo\n world?", 1);
        assertEquals(1, (int) w1.getWordsHashMap().get("world"));
        assertEquals(1, (int) w1.getWordsHashMap().get("hello"));
        assertFalse(w1.getWordsHashMap().containsKey("helloo"));
        w1 = new WordCounter();
        w1.countWords("221B? France", 2);
        assertFalse(w1.getWordsHashMap().containsKey("221B"));
        assertFalse(w1.getWordsHashMap().containsKey("France"));
        assertFalse(w1.getWordsHashMap().containsKey("B"));

    }

    @Test
    public void testNumberOfDifferentWords() {
        assertEquals(68, w2.numberOfDifferentWords());
        w2 = new WordCounter();
        w2.countWords("", 1);
        assertEquals(0, w2.numberOfDifferentWords());
    }

    @Test
    public void testGetWords() {
        String[] response = w2.getWords(1, 2);
        assertTrue(response.length == 2);
        assertEquals("the", response[0]);
        assertEquals("a", response[1]);
        response = w2.getWords(1, 6);
        assertTrue(response.length == 6);
        assertEquals("the", response[0]);
        assertEquals("a", response[1]);
        assertEquals("frequent", response[5]);
    }

    @Test(expected = RuntimeException.class)
    public void testGetWordsWithException1() {
        w2.getWords(3, 1);
    }

    @Test(expected = RuntimeException.class)
    public void testGetWordsWithException2() {
        w2.getWords(3, 500);
    }

    @Test(expected = RuntimeException.class)
    public void testGetWordsWithException3() {
        w2.getWords(0, 30);
    }

    @Test
    public void testGetWordCounts() {
        int[] response = w2.getWordCounts(1, 2);
        assertTrue(response.length == 2);
        assertEquals(8, response[0]);
        assertEquals(4, response[1]);
        response = w2.getWordCounts(1, 6);
        assertTrue(response.length == 6);
        assertEquals(8, response[0]);
        assertEquals(4, response[1]);
        assertEquals(2, response[5]);
    }

    @Test(expected = RuntimeException.class)
    public void testGetCountWithException1() {
        w2.getWordCounts(3, 1);
    }

    @Test(expected = RuntimeException.class)
    public void testGetCountWithException2() {
        w2.getWordCounts(3, 500);
    }

    @Test(expected = RuntimeException.class)
    public void testGetCountWithException3() {
        w2.getWordCounts(0, 30);
    }
    
    // Tests commented out relate to private methods

//     @Test
//     public void testRemovePunctuation() {
//     assertEquals("elephant", w1.removePunctuation("Elephant["));
//     assertEquals("elephant  feet", w1.removePunctuation("elephant$ feet"));
//     assertEquals("elephant's feet", w1.removePunctuation("elephant's feet"));
//     assertEquals("elephant-feet", w1.removePunctuation("elephant-feet"));
//     assertEquals("elephants feet", w1.removePunctuation("elephants' feet"));
//     assertEquals("elephants feet", w1.removePunctuation("elephants -feet"));
//     assertEquals("elephants feet", w1.removePunctuation("elephants feet- "));
//     assertEquals("elephants feet", w1.removePunctuation("-Elephants feet"));
//     assertEquals("elephants feet", w1.removePunctuation("Elephants feet'"));
//     assertEquals("elephants feet", w1.removePunctuation("Elephants feet+"));
//     assertEquals("elephants feet",
//     w1.removePunctuation("-elephants' feet'"));
//     assertEquals("don't", w1.removePunctuation("don't"));
//     assertEquals("id  p", w1.removePunctuation("ID=\"P-00018"));
//     assertEquals("don  t", w1.removePunctuation("don'''''t "));
//     assertEquals("don      t", w1.removePunctuation("don------''----'t"));
//     }
    //
    // @Test
    // public void testRemoveNumbers(){
    // assertEquals("", w1.removeNumbers("  35   "));
    // assertEquals("D", w1.removeNumbers("  35D  "));
    // assertEquals("D  D", w1.removeNumbers("  D35D  "));
    // }


}

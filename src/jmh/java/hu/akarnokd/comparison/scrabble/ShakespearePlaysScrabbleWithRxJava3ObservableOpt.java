/*
 * Copyright (C) 2019 Jos� Paumard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package hu.akarnokd.comparison.scrabble;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import hu.akarnokd.rxjava3.math.MathObservable;
import hu.akarnokd.rxjava3.string.StringObservable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;

/**
 * Shakespeare plays Scrabble with RxJava 3 Observable optimized.
 * @author José
 * @author akarnokd
 */
public class ShakespearePlaysScrabbleWithRxJava3ObservableOpt extends ShakespearePlaysScrabble {

    static Observable<Integer> chars(String word) {
//        return Observable.range(0, word.length()).map(i -> (int)word.charAt(i));
        return StringObservable.characters(word);
    }

    @SuppressWarnings("unused")
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(
        iterations = 5, time = 1
    )
    @Measurement(
        iterations = 5, time = 1
    )
    @Fork(1)
    public List<Entry<Integer, List<String>>> measureThroughput() throws Throwable {

        //  to compute the score of a given word
        Function<Integer, Integer> scoreOfALetter = letter -> letterScores[letter - 'a'];

        // score of the same letters in a word
        Function<Entry<Integer, MutableLong>, Integer> letterScore =
                entry ->
                        letterScores[entry.getKey() - 'a'] *
                        Integer.min(
                                (int)entry.getValue().get(),
                                scrabbleAvailableLetters[entry.getKey() - 'a']
                            )
                    ;


        Function<String, Observable<Integer>> toIntegerObservable =
                string -> chars(string);

        // Histogram of the letters in a given word
        Function<String, Single<HashMap<Integer, MutableLong>>> histoOfLetters =
                word -> toIntegerObservable.apply(word)
                            .collect(
                                () -> new HashMap<>(),
                                (HashMap<Integer, MutableLong> map, Integer value) ->
                                    {
                                        MutableLong newValue = map.get(value) ;
                                        if (newValue == null) {
                                            newValue = new MutableLong();
                                            map.put(value, newValue);
                                        }
                                        newValue.incAndSet();
                                    }

                            ) ;

        // number of blanks for a given letter
        Function<Entry<Integer, MutableLong>, Long> blank =
                entry ->
                        Long.max(
                            0L,
                            entry.getValue().get() -
                            scrabbleAvailableLetters[entry.getKey() - 'a']
                        )
                    ;

        // number of blanks for a given word
        Function<String, Observable<Long>> nBlanks =
                word -> MathObservable.sumLong(
                            histoOfLetters.apply(word).flattenAsObservable(
                                    map -> map.entrySet())
                            .map(blank)
                            ) ;


        // can a word be written with 2 blanks?
        Function<String, Observable<Boolean>> checkBlanks =
                word -> nBlanks.apply(word)
                            .map(l -> l <= 2L) ;

        // score taking blanks into account letterScore1
        Function<String, Observable<Integer>> score2 =
                word -> MathObservable.sumInt(
                            histoOfLetters.apply(word).flattenAsObservable(
                                    map -> map.entrySet())
                            .map(letterScore)
                            ) ;

        // Placing the word on the board
        // Building the streams of first and last letters
        Function<String, Observable<Integer>> first3 =
                word -> chars(word).take(3) ;
        Function<String, Observable<Integer>> last3 =
                word -> chars(word).skip(3) ;


        // Stream to be maxed
        Function<String, Observable<Integer>> toBeMaxed =
            word -> Observable.concat(first3.apply(word), last3.apply(word))
            ;

        // Bonus for double letter
        Function<String, Observable<Integer>> bonusForDoubleLetter =
            word -> MathObservable.max(toBeMaxed.apply(word)
                        .map(scoreOfALetter)
                        ) ;

        // score of the word put on the board
        Function<String, Observable<Integer>> score3 =
            word ->
                MathObservable.sumInt(Observable.concat(
                        score2.apply(word),
                        bonusForDoubleLetter.apply(word)
                )
                ).map(v -> 2 * v + (word.length() == 7 ? 50 : 0)) ;

        Function<Function<String, Observable<Integer>>, Single<TreeMap<Integer, List<String>>>> buildHistoOnScore =
                score -> Observable.fromIterable(shakespeareWords)
                                .filter(scrabbleWords::contains)
                                .filter(word -> checkBlanks.apply(word).blockingFirst())
                                .collect(
                                    () -> new TreeMap<Integer, List<String>>(Comparator.reverseOrder()),
                                    (TreeMap<Integer, List<String>> map, String word) -> {
                                        Integer key = score.apply(word).blockingFirst() ;
                                        List<String> list = map.get(key) ;
                                        if (list == null) {
                                            list = new ArrayList<>() ;
                                            map.put(key, list) ;
                                        }
                                        list.add(word) ;
                                    }
                                ) ;

        // best key / value pairs
        List<Entry<Integer, List<String>>> finalList2 =
                buildHistoOnScore.apply(score3).flattenAsObservable(
                        map -> map.entrySet())
                    .take(3)
                    .collect(
                        () -> new ArrayList<Entry<Integer, List<String>>>(),
                        (list, entry) -> {
                            list.add(entry) ;
                        }
                    )
                    .blockingGet() ;


//        System.out.println(finalList2);

        return finalList2 ;
    }

    public static void main(String[] args) throws Throwable {
        ShakespearePlaysScrabbleWithRxJava3ObservableOpt s = new ShakespearePlaysScrabbleWithRxJava3ObservableOpt();
        s.init();
        System.out.println(s.measureThroughput());
    }
}
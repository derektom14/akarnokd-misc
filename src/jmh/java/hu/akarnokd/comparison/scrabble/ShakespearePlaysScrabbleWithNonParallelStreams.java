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

import java.util.stream.Stream;

/**
 * Shakespeare plays Scrabble with Java Streams.
 * @author José
 */
public class ShakespearePlaysScrabbleWithNonParallelStreams extends ShakespearePlaysScrabbleWithStreams {

    @Override
    Stream<String> buildShakerspeareWordsStream() {
        return shakespeareWords.stream() ;
    }

    public static void main(String[] args) throws Exception {
        ShakespearePlaysScrabbleWithNonParallelStreams s = new ShakespearePlaysScrabbleWithNonParallelStreams();
        s.init();
        System.out.println(s.measureThroughput());
    }
}
# muji

The goal is to use a MIDI keyboard to enter music for typesetting
via LilyPond.

Currently, muji reads incoming MIDI events and generate a LilyPond file for the
music and then (on Windows) opens the generated PDF.

## Usage

Make sure `lilypond` is on your `PATH`.

The output is written currently to `temp.ly` and `temp.pdf`.

Start up muji and it will wait for you to start playing. Then it will continue
to record until there is a 5 second delay. At that point it will write the
LilyPond file, create the PDF version and open the PDF.

    $ java -jar muji-0.1.0-standalone.jar

## Examples

    lein run
    lein repl
    (use 'muji.core :reload)

### Future plans

Rhythm: different duration for notes based on length.
Bass clef for low notes and guess which hand is where.
Tempo inference.

## License

Copyright © 2020 Harold Lee

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

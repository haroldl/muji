(ns muji.core
  (:gen-class)
  (:use [muji.midi :only [midi-start midi-stop]])
  (:use [muji.lily])
  (:import (java.util.concurrent TimeUnit)))

(defn format-octave
  [octave]
  ; LilyPond uses \' to go up an Octave or \, to go down an octave.
  ; Octave 4 is the home of middle C, so we format relative to that.
  (if (>= octave 4)
    (apply str (repeat (- octave 4) \'))
    (apply str (repeat (- 4 octave) \,))))

(defn format-midi-note-for-lilypond
  ""
  [note]
  ; TODO: A, A#, and B are treated as one octave higher by LilyPond which is
  ;       C centric. Need to adjust for that.
  (str (name (note :pitch)) (format-octave (note :octave))))

(defn format-for-lilypond
  "Each input element is a list of notes that happened together, output is a string for that note or chord for showing in LilyPond."
  [notes]
  (case (count notes)
    (0 "")
    (1 (str (format-midi-note-for-lilypond (first notes)) "4"))
    (str "<" (clojure.string/join " " (map format-midi-note-for-lilypond notes)) ">4")))

(defn get-timestamp-millis [note]
  (/ (note :timestamp) 1000))

(defn within-time-window [note1 note2 window-millis]
  (< (Math/abs (- (get-timestamp-millis note1) (get-timestamp-millis note2)))
    window-millis))

(defn group-within-time-window
  "Group together notes within a given time window to recognize chords."
  ([notes window-millis]
    (group-within-time-window notes window-millis '() '()))
  ([notes window-millis accum result]
    (if (empty? notes)
      (reverse (if (empty? accum) result (conj result accum)))
      (if (empty? accum)
        ; We have a note but haven't accumulated any, so just add it to accum
        (recur (rest notes) window-millis (conj accum (first notes)) result)
        ; Did this new note happen within a very short time of the last note(s)?
        (if (within-time-window (first accum) (first notes) window-millis)
          ; This new note happened at approximately the same time as the accum,
          ; so add it to the chord.
          (recur (rest notes) window-millis (conj accum (first notes)) result)
          ; There has been enough delay since the last note or chord to start a
          ; new time window in accum.
          (recur (rest notes) window-millis (list (first notes)) (conj result accum)))))))

(defn make-lilypond [notes]
  (let [notes-on (filter #(%1 :is-note-on) notes)
        notes-grouped (group-within-time-window notes-on 80)]
    (prn notes-grouped)
    (str
      "{ \\clef treble \\fixed c' { "
      (clojure.string/join " " (map format-for-lilypond notes-grouped))
      " } }")))

(defn process-score [notes]
  (let [lily-source (make-lilypond notes)]
    (println lily-source)
    (save-string-to-file "temp.ly" lily-source)
    (run-lilypond "temp.ly")
    (open-pdf-file "temp.pdf")))

(defn -main
  "Print out incoming MIDI note events until there's a 5 second pause between events, then exit."
  [& args]
  (let [message-queue (midi-start)]
    ; Wait as long as needed for the first message.
    (println "Waiting for first note...")
    (let [first-parsed (.take message-queue)]
      (println first-parsed)
      ; Loop until we timeout waiting for a new message.
      (loop [notes (if (nil? first-parsed) '() (list first-parsed))]
        (let [message (.poll message-queue 5 TimeUnit/SECONDS)]
          (if (nil? message)
            ; Timed out
            (process-score (reverse notes))
            ; Got a MIDI event
            (do
              ;(midi-message-show message)
              (println message)
              (let [new-notes (if (nil? message) notes (conj notes message))]
                (recur new-notes))))))))
  (println "No notes detected for 5 seconds, exiting...")
  (midi-stop))

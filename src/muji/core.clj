(ns muji.core
  (:gen-class)
  (:use [muji.midi :only [midi-start midi-stop midi-note-show]])
  (:use [muji.lily])
  (:import (java.util.concurrent TimeUnit)))

(defn -main
  "Print out incoming MIDI note events until there's a 5 second pause between events, then exit."
  [& args]
  (let [message-queue (midi-start)]
    ; Wait as long as needed for the first message.
    (println "Waiting for first note...")
    (let [message (.take message-queue)]
      ;(midi-message-show message)
      (midi-note-show message))
    ; Loop until we timeout waiting for a new message.
    (loop [n 0]
      (let [message (.poll message-queue 5 TimeUnit/SECONDS)]
        (if (nil? message)
          nil
          (do
            ;(midi-message-show message)
            (midi-note-show message)
            (recur n))))))
  (println "No notes detected for 5 seconds, exiting...")
  (midi-stop))

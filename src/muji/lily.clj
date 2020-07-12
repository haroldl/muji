(ns muji.lily
  (:gen-class)
  (:use [clojure.java.io]))

(defn save-string-to-file
  [filename string-content]
  (with-open [w (writer filename)]
    (.write w string-content)))

(defn run-program
  [command-vector]
  (let [runtime (Runtime/getRuntime)
        process (.exec runtime (into-array command-vector))]
    (.waitFor process)))

(defn run-lilypond
  "Run lilypond to process the sample file. Assumes lilypond is on the PATH."
  [input-filename]
  (run-program ["lilypond" input-filename]))

(defn open-pdf-file
  ""
  [pdf-filename]
  (run-program ["explorer" pdf-filename]))

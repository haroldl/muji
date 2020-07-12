(ns muji.lily
  (:gen-class))

(defn run-lilypond
  "Run lilypond to process the sample file. Assumes lilypond is on the PATH."
  []
  (let [runtime (Runtime/getRuntime)
        process (.exec runtime (into-array ["lilypond" "sample.ly"]))]
    (.waitFor process)))

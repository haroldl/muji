(ns muji.core
  (:gen-class))

(import '(javax.sound.midi MidiSystem Sequencer Sequence Track MidiEvent MidiDevice Transmitter Receiver MidiMessage Synthesizer ShortMessage))

(defn midi-message-show
  "Pretty print the details of a MidiMessage"
  [message]
  (println (.length message) (.data message)))

(defn midi-device-info-show
  "Pretty print the details of a MidiDevice.Info"
  [midi-device-info]
  (println (.getName midi-device-info))
  (println (.getDescription midi-device-info))
  (println (.getVendor midi-device-info) (.getVersion midi-device-info))
  (println))

(defn midi-device-infos
  "Return a vector of all MidiDevice.Info instances available currently."
  []
  (into (vector) (MidiSystem/getMidiDeviceInfo)))

(defn midi-devices
  "Return a vector of all MidiDevice instances available currently."
  []
  (map #(MidiSystem/getMidiDevice %1) (midi-device-infos)))

(defn show-midi-devices
  "Show information about all MidiDevices known to the system."
  []
  (map #(midi-device-info-show (.getDeviceInfo %1)) (midi-devices)))

; Per https://docs.oracle.com/javase/9/docs/api/javax/sound/midi/MidiDevice.html
; getMaxReceivers is 0 meaning this is not an output devices.
; getMaxTransmitters is -1 meaning we can register as many listeners as we want.
; getTransmitter will register a new listener, which will subsequently show up
;   in the output of getTransmitters.
(defn is-input-port
  "Test whether or not this MidiDevice is an input port."
  [midi-device]
  (and
    (not (instance? Sequencer midi-device))
    (not (instance? Synthesizer midi-device))
    ; For input, getMaxTransmitters will be -1 (no limit), or > 0
    (not (= 0 (.getMaxTransmitters midi-device)))))

; Check for input ports, since the TBOX has both an input and output port with the same name for the first port.
(defn midi-device-by-name
  "Find an input MidiDevice instance by name."
  [device-name]
  (first
    (filter #(= device-name (.getName (.getDeviceInfo %1)))
      (filter #(is-input-port %1)
        (midi-devices)))))

(def input1 (midi-device-by-name "MIDIPLUS TBOX 2x2"))
(def input2 (midi-device-by-name "MIDIIN2 (MIDIPLUS TBOX 2x2)"))

(defn is-note-on [message]
  (and
    (instance? ShortMessage message)
    (<= 144 (.getCommand message) 159)))

(defn is-note-off [message]
  (and
    (instance? ShortMessage message)
    (<= 128 (.getCommand message) 143)))

(defn printing-receiver
  "Create a Receiver that prints each MidiMessage it receives."
  [name]
  (reify Receiver
    (close [this] (println "Receiver" name "closed"))
    (send [this message timestamp]
      ; NB: My Roland keyboard sends a midi event with command 240 at about 120 bpm.
      (if (or (is-note-on message) (is-note-off message))
        (println name timestamp message)))))

(defn wire-up "" [midi-device-to-use]
  (let [transmitter (.getTransmitter midi-device-to-use)
        receiver (printing-receiver "MIDI Receiver for Printing Messages")]
    (.setReceiver transmitter receiver)))

(defn start []
  (wire-up input1)
  (wire-up input2)
  (.open input1)
  (.open input2))

(defn stop []
  (.close input1)
  (.close input2))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (let [runtime (Runtime/getRuntime)
        freeMem (.freeMemory runtime)
        totalMem (.totalMemory runtime)
        cores (.availableProcessors runtime)]
    (midi-devices)
    (prn #{1 2 "Hello" (vector 'free freeMem) ['cores cores] {'total totalMem}}))
    (start))

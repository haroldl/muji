(ns muji.midi
  (:gen-class))

(import 'java.util.concurrent.LinkedBlockingQueue)
(import '(javax.sound.midi MidiSystem Sequencer Sequence Track MidiEvent MidiDevice Transmitter Receiver MidiMessage Synthesizer ShortMessage))

(defn midi-message-show
  "Pretty print the details of a MidiMessage"
  [message]
  (println { :channel (.getChannel message)
             :command (.getCommand message)
             :data1 (.getData1 message)
             :data2 (.getData2 message) }))

(defn is-note-on [message]
  (and
    (instance? ShortMessage message)
    (<= 144 (.getCommand message) 159)))

(defn is-note-off [message]
  (and
    (instance? ShortMessage message)
    (<= 128 (.getCommand message) 143)))

(defn parse-midi-note
  "Determine the pitch and octave of a note-on or note-off event."
  [message timestamp]
  (let [is-on (is-note-on message) is-off (is-note-off message)]
    (if (or is-on is-off)
      ; Adjust to make A4 = 48 so that pitch and octave work out.
      (let [adjusted-note (- (.getData1 message) 9)
            pitch-num (rem adjusted-note 12)
            ; Use pitch names that are LilyPond friendly.
            pitch ([:a :ais :b :c :cis :d :dis :e :f :fis :g :gis] pitch-num)
            octave (quot adjusted-note 12)
            velocity (.getData2 message)]
        { :pitch pitch
          :octave octave
          :timestamp timestamp
          :is-note-on is-on
          :velocity velocity })
      nil)))

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

(defn midi-inputs
  "Find all input MidiDevice objects available."
  []
  (filter is-input-port (midi-devices)))

; Check for input ports, since the TBOX has both an input and output port with the same name for the first port.
(defn midi-device-by-name
  "Find an input MidiDevice instance by name."
  [device-name]
  (first
    (filter #(= device-name (.getName (.getDeviceInfo %1))) (midi-inputs))))

(defn printing-receiver
  "Create a Receiver that prints each MidiMessage it receives."
  [name]
  (reify Receiver
    (close [this] (println "Receiver" name "closed"))
    (send [this message timestamp]
      ; NB: My Roland keyboard sends a midi event with command 240 at about 120 bpm.
      (if (or (is-note-on message) (is-note-off message))
        (println name timestamp message)))))

(defn queueing-receiver
  "Create a Receiver that puts each incoming MidiMessage into the queue."
  [queue]
  (reify Receiver
    (close [this] nil)
    (send [this message timestamp]
      ; NB: My Roland keyboard sends a midi event with command 240 on channel 14
      ;    at about 120 bpm... but the parse function will return nil for that.
      (let [data (parse-midi-note message timestamp)]
        (if (not (nil? data))
          (.put queue data))))))

(defn midi-start
  "Start listening for event on every available input MidiDevice and return a LinkedBlockingQueue that messages will be put into."
  []
  (let [message-queue (LinkedBlockingQueue.)
        receiver (queueing-receiver message-queue)]
    (doall (map #(do (println "Opening MIDI input device" (.getName (.getDeviceInfo %1)))
                     (.setReceiver (.getTransmitter %1) receiver)
                     (.open %1))
         (midi-inputs)))
    ; For handing events off to the main thread.
    message-queue))

(defn midi-stop
  "Shut down all input MidiDevice objects to stop their worker threads that would keep the JVM running after exit."
  []
  (doall (map #(.close %1) (midi-inputs))))

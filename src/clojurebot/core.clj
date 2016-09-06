(ns clojurebot.core
  (:gen-class)
  (:import [java.net Socket]
           [java.io PrintWriter InputStreamReader BufferedReader]))

(def freenode {:name "irc.freenode.net" :port 6667})
(def user {:name "Slacker's clojure bot" :nick "lispbot"})

(declare conn-handler)

(defn connect [server]
  (let [socket (Socket. (:name server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out})]
    (.start (Thread. #(conn-handler conn)))
    conn))

(defn write [conn msg]
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn kill
  "Kill the given connection. This does not gracefully quit."
  [conn]
  (dosync (alter conn merge {:exit true})))

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (println msg)
      (cond
        (re-find #"^ERROR :Closing Link:" msg)
        (kill conn)
        (re-find #"PRIVMSG (.*) :lispbot: (.*)" msg)
        (let [[_ channel msg] (re-find #"PRIVMSG (.*) :lispbot: (.*)" msg)]
          (println "Writing to channel" channel "and msg" msg)
          (write conn (str "PRIVMSG " channel " " msg)))
        (re-find #"^PING" msg)
        (write conn (str "PONG " (re-find #":.*" msg)))))))

(defn login [conn user]
  (write conn (str "NICK " (:nick user)))
  (write conn (str "USER " (:nick user) " 0 * :" (:name user))))

(defn quit [conn]
  ;; This will send a :Closing link: message
  (write conn "QUIT"))

(defn join [conn channel]
  (write conn (str "JOIN " channel))
  #_(dosync (alter conn update :channels conj channel)))

(defn part [conn channel]
  (write conn (str "PART " channel))
  #_(dosync (alter conn update :channels dissoc)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [irc (connect freenode)]
    (login irc user)
    (write irc "JOIN ##system32")
    (write irc "Hello there!")
    (write irc "QUIT")))

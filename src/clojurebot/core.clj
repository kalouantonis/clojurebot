(ns clojurebot.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.repl]
            [clojail.core :as jail]
            [clojail.testers :as testers])
  (:import [java.net Socket]
           [java.io PrintWriter InputStreamReader BufferedReader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure eval environment
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sandbox (jail/sandbox testers/secure-tester :timeout 5000))

(defn eval-message [msg]
  (try
    (sandbox (read-string msg))
    (catch Exception e
      (.getMessage e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; IRC Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def freenode {:name "irc.freenode.net" :port 6667})
(def user {:name "The lispiest lisp bot" :nick "lispbot"})

(declare conn-handler)

(defn connect [server]
  (let [socket (Socket. (:name server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out :active? true :channels #{}})]
    (.start (Thread. #(conn-handler conn)))
    conn))

(defn write [conn msg]
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn kill
  "Kill the given connection. This does not gracefully quit."
  [conn]
  (dosync (alter conn merge {:active? false})))

(defn message [conn target & s]
  (->> (string/join " " s)
       (str "PRIVMSG " target " :")
       (write conn)))

(defn handle-message [msg]
  (cond
    (re-find #"^time$" msg)
    (.toString (java.util.Date.))
    (re-find #"^echo (.*)" msg)
    (last (re-find #"^echo (.*)" msg))
    (re-find #"^amirite\?$" msg)
    "Yep, you're the most correct of them all."
    (re-find #"^coin$" msg)
    (if (zero? (rand-int 2)) "Heads" "Tails")
    (re-find #"^eval (.*)" msg)
    (str "=> " (eval-message (last (re-find #"^eval (.*)" msg))))
    (re-find #"^doc (.*)$" msg)
    (let [doc-name (last (re-find #"^doc (.*)" msg))]
      (->> (symbol doc-name)
           (resolve)
           (meta)
           (:doc)))
    :else "ERROR: Unrecognised command"))

(defn conn-handler [conn]
  (while (:active? @conn)
    (let [msg (.readLine (:in @conn))]
      (println msg)
      (cond
        (re-find #"^ERROR :Closing Link:" msg)
        (kill conn)
        (re-find #"PRIVMSG (.*) :lispbot: (.*)" msg)
        (let [[_ channel msg] (re-find #"PRIVMSG (.*) :lispbot: (.*)" msg)]
          (message conn channel (handle-message msg)))
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
  (dosync (alter conn update :channels conj channel)))

(defn part [conn channel]
  (write conn (str "PART " channel))
  (dosync (alter conn update :channels disj channel)))

(defn -main
  "App entry point"
  [& args]
  (let [irc (connect freenode)]
    (login irc user)
    (Thread/sleep 3000) ;; Just wait for a connection
    (join irc "##system32")
    (message irc "##system32" "Hello there!")))

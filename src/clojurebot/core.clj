;; Application entry point and main app logic.
;;
;; Author: Antonis Kalou
(ns clojurebot.core
  (:gen-class)
  (:require [clojail.core :as jail]
            [clojail.testers :as testers]
            [clojure.edn :as edn]
            [clojurebot.irc :as irc]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Evaluating in a sandboxed environment
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sandbox (jail/sandbox testers/secure-tester :timeout 5000))

(defn eval-message
  "Evaluate a message received from IRC."
  [msg]
  (try
    (binding [*read-eval* false]
      (sandbox (read-string msg)))
    (catch Exception e
      (.getMessage e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def commands (atom {}))

(defn reg-command
  "Register a new command handler. When an IRC user sends a private
  message containing"
  [name handler]
  (swap! commands assoc name handler))

(defn handle-message [chan msg]
  (let [[_ cmd args] (last (re-find #"^(\w+) (.*)" msg))
        error-handler (fn [_] "ERROR: Unrecognised command")]
    ((get @commands cmd error-handler) args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(reg-command
 "time"
 (fn [_]
   (.toString (java.util.Date.))))

(reg-command
 "echo"
 identity)

(reg-command
 "amirite?"
 (constantly
  "Yep, you're the most correct of them all."))

(reg-command
 "coin"
 (fn [_]
   (if (zero? (rand-int 2))
     "Heads"
     "Tails")))

(reg-command
 "eval"
 (fn [msg]
   (str "=> " (pr-str (eval-message msg)))))

(reg-command
 "doc"
 (fn [sym-name]
   (->> (symbol sym-name)
        (resolve)
        (meta)
        (:doc))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry point
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn -main
  "App entry point"
  [& args]
  (let [config (edn/read-string (slurp "resources/config.edn"))
        conn (irc/connect (:server config) handle-message)]
    (irc/login conn (:user config))
    (Thread/sleep 3000) ;; Just wait for a connection
    (doseq [chan (:channels config)]
      (irc/join conn "##system32"))
    (irc/message conn "##system32" "Hello there!")))

;; Application entry point and main app logic.
;;
;; Author: Antonis Kalou
(ns clojurebot.core
  (:gen-class)
  (:require [clojail.core :as jail]
            [clojail.testers :as testers]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojurebot.irc :as irc]
            [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Evaluating in a sandboxed environment
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sandbox (jail/sandbox testers/secure-tester :timeout 5000))

(defn eval-string
  "Attempt to evaluate a string. If an exception is thrown,
  the message text is returned."
  [s]
  (try
    (binding [*read-eval* false]
      (sandbox (read-string s)))
    (catch Exception e
      (.getMessage e))))

(defn parse-message
  [msg]
  (let [[cmd & args] (string/split msg #" ")]
    [cmd (string/join " " args)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def commands (atom {}))

(defn reg-command
  "Register a new command handler. When an IRC user sends a private
  message containing a string the same as the command name."
  [name handler]
  (swap! commands assoc name handler))

(defn replace-ws
  "Replace occurences of \n from s with whitespace, so it doesn't trim the IRC output."
  [s]
  (clojure.string/replace s #"\s+" " "))

(defn doc->string
  [s]
  (format "%s ::: %s" (:arglists s) (:doc s)))

(defn handle-message [chan msg]
  (let [[cmd msg] (parse-message msg)
        error-handler (constantly "ERROR: Unrecognised command")]
    ((get @commands cmd error-handler) msg)))

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
  "work"
  #(case %
      "init" "Bleep bloop, initializing work..."
      "end"  "Bleep bloop, work terminated"
      "Get back to work!"))

(reg-command
 "coin"
 (fn [_]
   (if (zero? (rand-int 2))
     "Heads"
     "Tails")))

(reg-command
 "eval"
 (fn [msg]
   (str "=> " (pr-str (eval-string msg)))))

(reg-command
  "doc"
  (fn [sym-name]
    (if (not (clojure.string/blank? sym-name))
        (if-let [sym (resolve (symbol sym-name))]
          (->> sym
               (meta)
               (doc->string)
               (replace-ws))
          (str "Error: No symbol for " sym-name))
        "Error: No arguments provided")))

(reg-command
 "d20"
 (fn [n]
     (if (< n 0)
       "You can't roll a negative amount of dice, man"
       (str "You rolled a "
            (->> #(+ (rand-int 20) 1)
                 (repeatedly)
                 (take n)
                 (string/join " and a "))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry point
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn load-config [path]
  (-> (io/resource path)
      (slurp)
      (edn/read-string)))

(defn -main
  "App entry point"
  [& args]
  (let [config (load-config "config.edn")
        conn (irc/connect (:server config) handle-message)]
    (irc/login conn (:user config))
    (doseq [chan (:channels config)]
      (irc/join conn chan)
      (irc/message conn chan (:greeting config)))))

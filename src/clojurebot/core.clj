(ns clojurebot.core
  (:gen-class)
  (:require [clojail.core :as jail]
            [clojail.testers :as testers]
            [clojurebot.connection :as irc]))

(def sandbox (jail/sandbox testers/secure-tester :timeout 5000))

(defn eval-message
  "Evaluate a message received from IRC."
  [msg]
  (try
    (binding [*read-eval* false]
      (sandbox (read-string msg)))
    (catch Exception e
      (.getMessage e))))

(defn handle-message [channel msg]
  (cond
    (nil? msg)
    msg
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

;; TODO: Load from config
(def freenode {:name "irc.freenode.net" :port 6667})
(def user {:name "The lispiest lisp bot" :nick "lispbot"})

(defn -main
  "App entry point"
  [& args]
  (let [irc (irc/connect freenode handle-message)]
    (irc/login irc user)
    (Thread/sleep 3000) ;; Just wait for a connection
    (irc/join irc "##system32")
    (irc/message irc "##system32" "Hello there!")
    irc))

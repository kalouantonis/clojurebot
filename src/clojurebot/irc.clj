;; Utilities for creating and working with connections to IRC networks.
;;
;; Author: Antonis Kalou
(ns clojurebot.irc
  (:require [clojure.string :as string])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))

(defrecord Connection [in             ;; IRC input stream
                       out            ;; IRC output stream
                       active?        ;; True if connection is open
                       channels       ;; Channels currently connected to
                       msg-handler])  ;; Private message handler

(defn- make-connection [in out msg-handler]
  (map->Connection
   {:in in
    :out out
    :active? false
    :channels #{}
    :msg-handler msg-handler}))

(defn- write
  "Write a string to the IRC output stream."
  [conn msg]
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn message [conn target & s]
  (->> (string/join " " s)
       (str "PRIVMSG " target " :")
       (write conn)))

(defn- kill
  [conn]
  (dosync (alter conn merge {:active? false})))

(defn- conn-handler [conn]
  (while (:active? @conn)
    (let [msg (.readLine (:in @conn))]
      (println msg)
      (cond
        (re-find #"^ERROR :Closing Link:" msg)
        (kill conn)
        (re-find #"PRIVMSG .* :@.*" msg)
        ;; TODO: Add configurable prefix
        (let [[_ channel msg] (re-find #"PRIVMSG (.*) :@(.*)" msg)]
          (message conn channel ((:msg-handler @conn) channel msg)))
        (re-find #"^PING" msg)
        (write conn (str "PONG " (re-find #":.*" msg)))))))

(defn connect [server msg-handler]
  ;; TODO: Close socket on finish
  (let [socket (Socket. (:host server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref (make-connection in out msg-handler))]
    (dosync (alter conn merge {:active? true})) ;; Start
    (.start (Thread. #(conn-handler conn)))
    conn))

(defn login [conn user]
  (write conn (str "NICK " (:nick user)))
  (write conn (str "USER " (:nick user) " 0 * :" (:name user))))

(defn quit [conn]
  ;; This cause a :Closing link: message to be sent
  (write conn "QUIT"))

(defn join [conn channel]
  (write conn (str "JOIN " channel))
  (dosync (alter conn update :channels conj channel)))

(defn part [conn channel]
  (write conn (str "PART " channel))
  (dosync (alter conn update :channels disj channel)))

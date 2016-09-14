(ns tinder-finder.redis
  (:require [taoensso.carmine :as car :refer (wcar)])
  (:gen-class))

;conf
(def server1-conn {:pool {}
                   :spec {:host "127.0.0.1"
                          :port 6379
                          :db 4}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

; helper fns
; Carmine uses Nippy to serialize
(defn force-serialization [m] (into {} (for [[k v] m] [k (car/freeze v)])))
(defn hgetall-as-map [key] (wcar* (car/parse-map (car/hgetall key) :keywordize)))
(defn mk-user-key [user] (str "tinder-finder:user:" (user :_id)))
(def prof-pic-key "tinder-finder:prof-pic-keywords")

; read/write fns
(defn save-tags [tags]
  (println "saving tags:" tags)
  (wcar* :as-pipeline
    (mapv (partial car/lpush prof-pic-key) tags)))

(defn save-matches [matches]
  (println "adding matches:" (map #(str (select-keys % [:name :schools :bio :_id]) (get-in % [:photos 0 :url])) matches))
  (wcar* :as-pipeline
    (mapv (fn [key user] (car/hmset* key user))
          (mapv mk-user-key matches)
          (mapv force-serialization matches))))

(defn retrieve-matches []
  (let [keys  (wcar* (car/keys "tinder-finder:user:*"))]
    (map hgetall-as-map keys)))

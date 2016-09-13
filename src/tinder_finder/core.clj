(ns tinder-finder.core
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]])
  (:gen-class))

(timbre/refer-timbre)

(def x-recs (json/decode (env :search-sample) true))

; redis conf
(def server1-conn {:pool {}
                    :spec {:host "127.0.0.1"
                           :port 6379
                           :db 4}}) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))
(defn force-serialization [m] (into {} (for [[k v] m] [k (car/freeze v)])))
(defn hgetall-as-map
  [key]
  (wcar* (car/parse-map (car/hgetall key) :keywordize)))
(defn mk-user-key [user] (str "tinder-finder:user:" (user :_id)))

; api-call conf
(defmacro api-wrap
  [method url opts success-fn]
  ('println method url opts success-fn)
  (http/request (~merge {:method method
                        :url url} opts)
    ('fn [{:keys ['status 'headers 'body 'err]}]
      ('println "did i work?" 'status 'headers 'body 'err))))

; tinder conf
(def tinder-headers
  {"Content-Type" "application/json"
   "User-Agent" "Tinder/4.1.4 (iPhone; iOS 8.1.3; Scale/2.00)"
   "X-Auth-Token" (env :tinder-token)})

(def tinder-api-url
  "https://api.gotinder.com")

; tinder calls
(defn get-tinder-auth
  []
  (let [url (str tinder-api-url "/auth")
        opts {:headers {"Content-Type" "application/json"
                        "User-Agent" "Tinder/4.1.4 (iPhone; iOS 8.1.3; Scale/2.00)"}
              :body (json/encode {:facebook_token (env :fbook-token)
                                  :facebook_id (env :fbook-id)})}]
    (http/post url opts
      (fn [{:keys [status headers body err]}]
        (let [res (json/decode body true)]
        (if-not (= 200 status)
          (error "processing response" (res :error))
          res))))))

(defn set-preferences
  [gender gender-filter age-min age-max dist-max]
  (let [url (str tinder-api-url "/profile")
        opts {:headers tinder-headers
              :body (json/encode {:gender gender
                                  :gender-filter gender-filter
                                  :age-filter-min age-min
                                  :age-filter-max age-max
                                  :distanct-filter dist-max})}]
      (http/post url opts
        (fn [{:keys [status headers body err]}]
          (let [res (json/decode body true)]
          (if-not (= 200 status)
            (error "processing response" (res :error))
            res))))))

(defn set-location
  [lat lng]
  (let [url (str tinder-api-url "/user/ping")
        opts {:headers tinder-headers
              :body (json/encode {:lat lat
                                  :lng lng})}]
      (http/post url opts
        (fn [{:keys [status headers body err]}]
          (let [res (json/decode body true)]
          (if-not (= 200 status)
            (error "processing response" (res :error))
            res))))))

(defn get-recs
  []
  (let [url (str tinder-api-url "/user/recs")
        opts {:headers tinder-headers}]
      (http/get url opts
        (fn [{:keys [status headers body err]}]
          (let [res (json/decode body true)]
          (if-not (= 200 status)
            (error "processing response" (res :error))
            (res :results)))))))

(defn pass-mismatch
  [id]
  (let [url (str tinder-api-url "/pass/" id)
        opts {:headers tinder-headers}]
    (http/get url opts
      (fn [{:keys [status headers body err]}]
        (let [res (json/decode body true)]
        (if-not (= 200 status)
          (error "processing response" res)
          res))))))

; match filter
(defn query-clarifai
  [url]
  (let [api "https://api.clarifai.com/v1/tag/"
        opts {:headers {"Authorization" (str "Bearer " (env :clarifai-token))}
              :query-params {:url url}}]
    (http/get api opts
      (fn [{:keys [status headers body err]}]
        (let [res (json/decode body true)]
        (if-not (= 200 status)
          (error "processing response" (res :error))
          res))))))

(defn dups [seq]
  (for [[id freq] (frequencies seq)  ;; get the frequencies, destructure
        :when (> freq 1)]            ;; this is the filter condition
   id))                              ;; just need the id, not the frequency

(defn save-tags
  [tags]
  (let [key "tinder-finder:prof-pic-keywords"]
    (println "saving tags:" tags)
    (wcar* :as-pipeline
      (mapv #(car/lpush key %) tags))))

(defn img-match-found?
  [img-url]
  (let [res (query-clarifai img-url)
        tags (get-in @res [:results 0 :result :tag :classes])
        search-tags (read-string (env :search-tags))
        duplicates (dups (concat tags search-tags))]
    (save-tags tags)
    (=  (count search-tags) (count duplicates))))

(defn sort-recs
  [recs]
  (let [matches (transient [])
        mismatches (transient [])]
  (mapv (fn [r]
    (if (or
           (= (r :name) (env :search-name))
           (contains? (mapv :name (r :schools)) (env :search-school))
           (boolean (re-find (re-pattern (env :search-bio)) (r :bio)))
          ;  (img-match-found? ((first (r :photos)) :url))
           )
      (conj! matches r)
      (do
        (println "bye," (r :name))
        (conj! mismatches (r :_id))))) recs)
      {:matches (persistent! matches)
       :mismatches (persistent! mismatches)}))

(defn save-recs
  []
  (let [sorted (sort-recs x-recs)
        matches (sorted :matches)
        mismatches (sorted :mismatches)]
        (println "Matches:" (count matches)
                 "Mistmatches:" (count mismatches))
      (doseq [m mismatches]
        (let [pass (pass-mismatch m)]
          (println (@pass :status) ":passed on " m)))

      (when (> (count matches) 0)
        (println "adding matches" (select-keys matches [:name :schools :bio] (get-in matches [:photos 0 :url])))

        (wcar* :as-pipeline
          (mapv (fn [key user] (car/hmset* key user))
                (mapv mk-user-key matches)
                (mapv force-serialization matches))))))

(defn -main
  "Get catching"
  []
  (save recs))

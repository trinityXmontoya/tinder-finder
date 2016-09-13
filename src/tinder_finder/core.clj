(ns tinder-finder.core
  (:require [tinder-finder.tinder :as tinder]
            [tinder-finder.redis :as redis]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]])
  (:gen-class))

(timbre/refer-timbre)

;helper fns
; credit - http://stackoverflow.com/a/8056727/3481754
(defn dups [seq]
  (for [[id freq] (frequencies seq)  ;; get the frequencies, destructure
        :when (> freq 1)]            ;; this is the filter condition
   id))                              ;; just need the id, not the frequency

(defmacro api-wrap
  [method url opts success-fn]
  ('println method url opts success-fn)
  (http/request (~merge {:method method
                        :url url} opts)
    ('fn [{:keys ['status 'headers 'body 'err]}]
      ('println "did i work?" 'status 'headers 'body 'err))))

; match filter
(defn get-img-tags
  "query and parse Clarifai API for identifiying tags
   associated with img-url"
  [img-url]
  (let [api-url "https://api.clarifai.com/v1/tag/"
        opts {:headers {"Authorization" (str "Bearer " (env :clarifai-token))}
              :query-params {:url img-url}}]
    (http/get api-url opts
      (fn [{:keys [status headers body err]}]
        (let [res (json/decode body true)]
        (if-not (= 200 status)
          (error "processing response" (res :error))
          res))))))

(defn img-match-found?
  "determine whether img-url is similar to stored image based off
   respective identifying tags"
  [img-url]
  (let [img-tags (get-in @(get-img-tags img-url) [:results 0 :result :tag :classes])
        search-tags (read-string (env :search-tags))
        duplicates (dups (concat img-tags search-tags))]
    (redis/save-tags img-tags)
    (=  (count search-tags) (count duplicates))))

(defn sort-recs
  "sort recommendations based off provided filters
   returns {:matches [match1 match2] :passes [pass1 pass2]}"
  [recs]
  (let [matches (transient [])
        passes (transient [])]
    (mapv (fn [r]
      (if (or (= (r :name) (env :search-name))
              (contains? (mapv :name (r :schools)) (env :search-school))
              (boolean (re-find (re-pattern (env :search-bio)) (r :bio)))
              (img-match-found? ((first (r :photos)) :url)))
        (conj! matches r)
        (do (println "bye," (r :name))
            (conj! passes (r :_id))))) recs)
    {:matches (persistent! matches) :passes (persistent! passes)}))

(defn process-recs
  []
  (let [sorted (sort-recs @(tinder/get-recs))
        matches (sorted :matches)
        passes (sorted :passes)]
    (println "Matches:" (count matches) "Passes:" (count passes))
    (doseq [p passes]
      (let [pass (tinder/pass-user p)]
        (println (@pass :status) ":passed on " p)
        (Thread/sleep 200)))
    (when (> (count matches) 0)
      (redis/save-matches matches))))

(defn -main
  "Get catching"
  []
  (while true
    (process-recs)
    (Thread/sleep 1000)))

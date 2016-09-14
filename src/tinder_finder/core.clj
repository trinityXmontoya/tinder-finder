(ns tinder-finder.core
  (:require [tinder-finder.tinder :as tinder]
            [tinder-finder.redis :as redis]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [cheshire.core :as json])
  (:gen-class))

; helper fns
; credit - http://stackoverflow.com/a/8056727/3481754
(defn dups [seq]
  (for [[id freq] (frequencies seq)
        :when (> freq 1)]
   id))

; match filter
(defn get-img-tags
  "query and parse Clarifai API for identifiying tags
   associated with img-url"
  [img-url]
  (let [api-url "https://api.clarifai.com/v1/tag/"
        opts {:headers {"Authorization" (str "Bearer " (env :clarifai-token))}
              :query-params {:url img-url}}
        {:keys [status body error]} @(http/get api-url opts)
        body (json/decode body true)]
    (if (not (= status 200))
      (throw error)
      (get-in body [:results 0 :result :tag :classes]))))

(defn img-match-found?
  "determine whether img-url is similar to stored image based off
   respective identifying tags"
  [img-url]
  (let [img-tags (get-img-tags img-url)
        search-tags (read-string (env :search-tags))
        duplicates (dups (concat img-tags search-tags))]
    (redis/save-tags img-tags)
    (=  (count search-tags) (count duplicates))))

(defn sort-recs
  "sort recommendations based off provided filters
   returns {:matches [match1 match2] :pass_ids [pass1 pass2]}"
  [recs]
  (let [matches (transient [])
        pass_ids (transient [])]
    (mapv (fn [r]
      (if (or (= (r :name) (env :search-name))
              (contains? (mapv :name (r :schools)) (env :search-school))
              (boolean (re-find (re-pattern (env :search-bio)) (r :bio)))
              (img-match-found? ((first (r :photos)) :url)))
        (conj! matches r)
        (do (println "bye," (r :name))
            (conj! pass_ids (r :_id))))) recs)
    {:matches (persistent! matches) :pass_ids (persistent! pass_ids)}))

(defn process-recs
  []
  (let [sorted (sort-recs (tinder/get-recs))
        matches (sorted :matches)
        pass_ids (sorted :pass_ids)]
    (println "Matches:" (count matches) "Passes:" (count pass_ids))
    (doseq [id pass_ids]
      (let [res (tinder/pass-user id)]
        (println (res :status) ":passed on " id)))
    (when (> (count matches) 0)
      (redis/save-matches matches)
      ; optionally like user so they don't come back in results
      (map tinder/like-user (map :id matches)))))

(defn -main
  "Get catching"
  []
  (while true
    (process-recs)
    (Thread/sleep 1000)))

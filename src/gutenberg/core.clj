(ns gutenberg.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]))

(def InputDateFormat (java.text.SimpleDateFormat. "dd-MM-yyyy'T'HH"))

(defn is-post-file?
  "Determines if given filename conforms to post markdown file"
  [filename]
  (if (re-find #"\A[0-9]{2}-[0-9]{2}-[0-9]{4}-.+\.md\z" filename)
    true
    false))

(defn list-files
  "List files"
  [relative-path predicate]
  (let [files (file-seq (io/file relative-path))]
    (filter (fn [^java.io.File f] (predicate (.getName f))) files)))

(defn extract-date-title
  "Extracts date and title from file, returns vector with 
   date as first and title as second argument"
  [^java.io.File file]
  (let [[_ date title] (re-find #"\A([0-9]{2}-[0-9]{2}-[0-9]{4})-(.+)\.md\z" (.getName file))]
    [date title]))

(defn create-default-post-descriptor
  "Creates default post descriptor according to filename and author"
  [author ^java.io.File file]
  (let [[date title] (extract-date-title file)]
    {:author author
     :date date
     :title title
     :file file}))

(defn merge-with-explicit-descriptor
  "Merges the post descriptor map with explicit descriptor, 
   if such descriptor is found on given path"
  [posts-path {:keys [date title] :as file-descriptor}]
  (let [explicit-descriptor (io/file (str posts-path date "-" title ".edn"))]
    (if (.exists explicit-descriptor)
      (merge file-descriptor (edn/read-string (slurp explicit-descriptor)))
      file-descriptor)))

(defn get-post-descriptors
  "Returns sequence of post descriptors containing all necessary
   informations for next transformations"
  [author posts-path]
  (->> (list-files posts-path is-post-file?)
       (map (partial create-default-post-descriptor author))
       (map (partial merge-with-explicit-descriptor posts-path))
       (map (fn [descriptor]
              (update-in descriptor [:date] (fn [date-str]
                                              (. InputDateFormat (parse (str date-str "T12")))))))))

(defn sort-descriptors
  "Sort descriptors according to their dates, but aware of order 
   specified via :order key in descriptor, if present"
  [descriptors asc]
  (let [comparator (if asc compare (fn [x y] (compare y x)))]
    (->> (sort-by :date comparator descriptors)
         (map (fn [idx {order :order :as descriptor}]
                (if order descriptors (assoc descriptor :order idx))) (iterate inc 0))
         (sort-by :order))))

(defn create-blog-descriptor
  "Returns a map containing all data necessary to generate static blog site"
  [{:keys [author posts-path ascending-ordering] :as blog-descriptor}]
  (let [post-descriptors (-> (get-post-descriptors author posts-path)
                             (sort-descriptors ascending-ordering))]
    (-> (assoc blog-descriptor :posts post-descriptors)
        (dissoc :posts-path :ascending-ordering))))

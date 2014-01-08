(ns gutenberg.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [gutenberg.transformations :as t]))

(def InputDateFormat (java.text.SimpleDateFormat. "dd-MM-yyyy'T'HH"))
(def OutputDateFormat (java.text.SimpleDateFormat. "dd-MM-yyyy"))

(defn is-post-file?
  "Determines if given filename conforms to post markdown file"
  [filename]
  (if (re-find #"\A[0-9]{2}-[0-9]{2}-[0-9]{4}-.+\.md\z" filename)
    true
    false))

(defn list-files
  "Lists files"
  [relative-path predicate]
  (let [files (file-seq (io/file relative-path))]
    (filter (fn [^java.io.File f] (predicate (.getName f))) files)))

(defn extract-date-title
  "Extracts date and title from file, returns vector with
   date as first and title as second argument"
  [^java.io.File file]
  (let [[_ date title] (re-find #"\A([0-9]{2}-[0-9]{2}-[0-9]{4})-(.+)\.md\z" (.getName file))]
    [date title]))

(defn create-post-uri
  "Creates post uri"
  [title date]
  (str "/" title "-" (.format OutputDateFormat date) ".html"))

(defn create-default-post-descriptor
  "Creates default post descriptor according to filename and author"
  [author ^java.io.File file]
  (let [[date-str title] (extract-date-title file)]
    (let [date (.parse InputDateFormat (str date-str "T12"))]
      {:author author
       :date date
       :title title
       :file file
       :post-uri (create-post-uri title date)})))

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
       (map (partial merge-with-explicit-descriptor posts-path))))

(defn sort-descriptors
  "Sorts descriptors according to their dates, but aware of order
   specified via :order key in descriptor, if present"
  [descriptors asc]
  (let [comparator (if asc compare (fn [x y] (compare y x)))]
    (->> (sort-by :date comparator descriptors)
         (map (fn [idx {order :order :as descriptor}]
                (if order descriptor (assoc descriptor :order idx))) (iterate inc 0))
         (sort-by :order))))

(defn partition-previews
  "Partitions post-descriptors into preview sites according to number of post previews
   on each site"
  [post-descriptors previews-on-page]
  (let [posts-count (count post-descriptors)
        whole-posts-count (- posts-count (rem posts-count previews-on-page))
        whole-preview-sites (into []
                                  (partition previews-on-page
                                             (take whole-posts-count post-descriptors)))]
    (if (= whole-posts-count posts-count)
      whole-preview-sites
      (conj whole-preview-sites (drop whole-posts-count post-descriptors)))))

(defn create-preview-page-uri
  "Creates preview page uri"
  [page-name page-number]
  (str "/" page-name "-" page-number ".html"))

(defn add-preview-page-uris
  "Adds uris to preview pages"
  [partitioned-pages page-name]
  (map (fn [post-descriptors page-number]
         {:post-descriptors post-descriptors
          :preview-page-uri (create-page-uri page-name page-number)})
       partitioned-pages
       (iterate inc 1)))

(defn add-previous-next-page-links
  "Add links to previous/next pages for each page"
  [pages pages-shown]
  (let [page-uris (map :preview-page-uri pages)]
    (map (fn [page idx]
           (-> page
               (assoc :prev (take idx page-uris))
               (assoc :next (drop (+ 1 idx) page-uris)))) pages (iterate inc 0))))

(defn create-blog-descriptor
  "Returns a map containing all data necessary to generate static blog site"
  [{:keys [post posts-dir ascending-ordering outline-dir blog-template
           paging post-template] :as blog-descriptor}]
  (let [post-descriptors (-> (get-post-descriptors (:author post) posts-dir)
                             (sort-descriptors ascending-ordering))
        post-preview-descriptors (-> post-descriptors
                                     (partition-previews (:previews-on-page paging))
                                     (add-preview-page-uris "PREVIEW")
                                     (add-previous-next-page-links (:pages-shown paging)))
        blog-template-file (io/file (str outline-dir blog-template))
        post-template-file (io/file (str outline-dir post-template))]
    (-> blog-descriptor
        (assoc :posts post-descriptors)
        (assoc :previews post-preview-descriptors)
        (assoc :blog-template-file blog-template-file)
        (assoc :post-template-file post-template-file))))

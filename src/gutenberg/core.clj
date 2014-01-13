(ns gutenberg.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [gutenberg.transformations :as t]))

(def InputDateFormat (java.text.SimpleDateFormat. "dd-MM-yyyy'T'HH"))
(def PostLinkDateFormat (java.text.SimpleDateFormat. "dd-MM-yyyy"))

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
  [title ^java.util.Date date]
  (str "/" title "-" (.format PostLinkDateFormat date) ".html"))

(defn create-default-post-descriptor
  "Creates default post descriptor according to filename and author"
  [^java.io.File file]
  (let [[date-str title] (extract-date-title file)]
    {:date-str date-str
     :title title
     :file file}))

(defn generate-post-uri
  "Generates post uri and associates it into descriptor map"
  [{:keys [date title] :as post-descriptor}]
  (assoc post-descriptor :post-uri (create-post-uri title date)))

(defn parse-post-date-string
  "Parses date string from filename into java date object and
   associates it into descriptor map, in the same time, dissociates
   the date string from descriptor map because it's no longer needed"
  [{:keys [date-str] :as post-descriptor}]
  (-> post-descriptor
      (assoc :date (.parse InputDateFormat (str date-str "T12")))
      (dissoc :date-str)))

(defn merge-with-explicit-descriptor
  "Merges the post descriptor map with explicit descriptor,
   if such descriptor is found on given path"
  [posts-path {:keys [date-str title] :as file-descriptor}]
  (let [explicit-descriptor (io/file (str posts-path date-str "-" title ".edn"))]
    (if (.exists explicit-descriptor)
      (merge file-descriptor (edn/read-string (slurp explicit-descriptor)))
      file-descriptor)))

(defn get-post-descriptors
  "Returns sequence of post descriptors containing all necessary
   informations for next transformations"
  [posts-path]
  (->> (list-files posts-path is-post-file?)
       (map (comp generate-post-uri
                  parse-post-date-string
                  (partial merge-with-explicit-descriptor posts-path)
                  create-default-post-descriptor))))

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
  "Adds uris and numbering to preview pages"
  [partitioned-pages page-name]
  (map (fn [post-descriptors page-number]
         {:post-descriptors post-descriptors
          :page-number page-number
          :page-uri (create-preview-page-uri page-name page-number)})
       partitioned-pages
       (iterate inc 1)))

(defn add-previous-next-page-links
  "Add links to previous/next pages for each page"
  [pages pages-shown]
  (let [paging-vec (mapv #(select-keys % [:page-uri :page-number]) pages)
        max-idx (count pages)]
    (map (fn [{:keys [page-number] :as page}]
           (let [idx (dec page-number)
                 lower-bound (* pages-shown (quot idx pages-shown))
                 upper-bound (min max-idx (+ lower-bound pages-shown))]
             (-> page
                 (assoc :next-pages (subvec paging-vec page-number upper-bound))
                 (assoc :prev-pages (subvec paging-vec lower-bound idx))
                 (assoc :next-section (get paging-vec upper-bound))
                 (assoc :prev-section (get paging-vec (dec lower-bound))))))
         pages)))

(defn create-blog-descriptor
  "Returns a map containing all data necessary to generate static blog site"
  [{:keys [post posts-dir ascending-ordering outline-dir blog-template
           paging post-template] :as blog-descriptor}]
  (let [post-descriptors (-> (get-post-descriptors posts-dir)
                             (sort-descriptors ascending-ordering))
        post-preview-descriptors (-> post-descriptors
                                     (partition-previews (:previews-on-page paging))
                                     (add-preview-page-uris "PREVIEW")
                                     (add-previous-next-page-links (:pages-shown paging)))]
    (-> blog-descriptor
        (assoc :posts post-descriptors)
        (assoc :previews post-preview-descriptors)
        (assoc :blog-template-file (io/file (str outline-dir blog-template)))
        (assoc :post-template-file (io/file (str outline-dir post-template))))))

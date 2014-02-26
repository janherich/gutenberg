(ns gutenberg.transformations
  (:require [net.cgrand.enlive-html :as html]
            [markdown.core :as markdown]))

(def TITLE-SELECTOR [:head :title])
(def AUTHOR-SELECTOR [:head [:meta (html/attr= :name "author")]])
(def DESCRIPTION-SELECTOR [:head [:meta (html/attr= :name "description")]])
(def KEYWORDS-SELECTOR [:head [:meta (html/attr= :name "keywords")]])

(def PostTagDateFormat (java.text.SimpleDateFormat. "dd-MM-yyyy"))

(def ^:private flip #(fn [node & values] ((% values) node)))

(defn- select-element
  ([nodes element]
     (first (html/select nodes element)))
  ([nodes container element]
     (select-element nodes (into container element))))

(defmacro maybe-content
  [expr]
  `(if-let [x# ~expr] (html/content x#) identity))

(defmacro maybe-set-attr
  [attr-key expr]
  `(if-let [x# ~expr] (html/set-attr ~attr-key x#) identity))

(defn create-post
  [content-fn post-element-nodes tag-element-nodes
   {post-config-title :title post-config-date :date post-config-content :content
    {post-config-tags-container :container} :tags}
   {post-author :author post-title :title post-date :date post-tags :tags post-file :file}]
  (let [filled-tags (map (partial (flip html/content) tag-element-nodes) post-tags)]
    (html/at post-element-nodes
             post-config-title (html/content post-title)
             post-config-date (html/content (.format PostTagDateFormat post-date))
             post-config-tags-container (html/content filled-tags)
             post-config-content (html/content (content-fn post-file)))))

(defn create-posts
  [post-template-nodes
   {post-config-element :element
    {post-config-tags-container :container
     post-config-tags-element :element} :tags :as post-config}
   post-descriptors]
  (let [post-element-nodes (select-element post-template-nodes post-config-element)
        tag-element-nodes (select-element post-element-nodes
                                          post-config-tags-container
                                          post-config-tags-element)]
    (map (fn [{post-author :author
               post-title :title
               post-date :date
               post-tags :tags
               post-meta-desc :meta-description :as post-descriptor}]
           (let [filled-post (create-post (fn [f] "TEST") post-element-nodes
                                          tag-element-nodes post-config post-descriptor)
                 filled-site (html/at post-template-nodes
                                      TITLE-SELECTOR (html/content post-title)
                                      AUTHOR-SELECTOR (maybe-set-attr :content post-author)
                                      DESCRIPTION-SELECTOR (maybe-set-attr :content post-meta-desc)
                                      KEYWORDS-SELECTOR (maybe-set-attr :content
                                                                        (when post-tags
                                                                          (apply str
                                                                                 (interpose "," post-tags))))
                                      post-config-element (html/substitute filled-post))]
             (assoc post-descriptor :post-site filled-site)))
         post-descriptors)))

(defn create-paging-nodes [paging-template-nodes
                           {:keys [page-number prev-section next-section
                                   prev-pages next-pages]}
                           {:keys [element-page element-page-active
                                   element-page-disabled pages-before pages-next]}]
  (let [page-nodes (select-element paging-template-nodes element-page)
        active-page-nodes (select-element paging-template-nodes element-page-active)
        page-disabled-nodes (select-element paging-template-nodes element-page-disabled)
        content (flip html/content)
        set-attr (flip html/set-attr)
        fill-page (fn [{:keys [page-number page-uri]}]
                    (-> page-nodes
                        (content page-number)
                        (set-attr :href page-uri)))
        prev-section-filled (if prev-section
                              (-> page-nodes
                                  (content pages-before)
                                  (set-attr :href (:page-uri prev-section)))
                              (content page-disabled-nodes pages-before))
        next-section-filled (if next-section
                              (-> page-nodes
                                  (content pages-next)
                                  (set-attr :href (:page-uri next-section)))
                              (content page-disabled-nodes pages-next))
        page-filled (content active-page-nodes (str page-number))]
    (concat (list prev-section-filled)
            (map fill-page prev-pages)
            (list page-filled)
            (map fill-page next-pages)
            (list next-section-filled))))

(defn create-post-previews [blog-template-nodes
                            {preview-config-container :container
                             preview-config-element :element
                             preview-config-title :title
                             preview-config-date :date
                             preview-config-content :content
                             {preview-config-tags-container :container
                              preview-config-tags-element :element} :tags}
                            {paging-config-container :container :as paging-config}
                            preview-descriptors]
  (let [paging-container-nodes (select-element blog-template-nodes
                                               paging-config-container)
        preview-element-nodes (select-element blog-template-nodes
                                              preview-config-container
                                              preview-config-element)
        tag-element-nodes (select-element preview-element-nodes
                                          preview-config-tags-container
                                          preview-config-tags-element)]
    (->> preview-descriptors
         (map (fn [{:keys [posts page-number page-uri] :as preview-descriptor}]
                (let [filled-posts (map (fn [{post-title :title
                                              post-date :date
                                              post-tags :tags}]
                                          {})
                                        posts)]
                  (html/at blog-template-nodes
                           preview-config-container (html/content "PREVIEW")
                           paging-config-container (html/content (create-paging-nodes
                                                                  paging-container-nodes
                                                                  preview-descriptor
                                                                  paging-config)))))))))

(defn transform-html [{:keys [blog-template-file post-template-file posts
                              previews post post-preview paging]}]
  (let [post-tmpl-nodes (html/html-resource post-template-file)
        post-sites (create-posts post-tmpl-nodes post posts)
        blog-tmpl-nodes (html/html-resource blog-template-file)
        preview-sites (create-post-previews blog-tmpl-nodes post-preview paging previews)]
    post-sites))

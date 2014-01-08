(ns gutenberg.transformations
  (:require [net.cgrand.enlive-html :as html]
            [markdown.core :as markdown]))

(def TITLE-SELECTOR [:head :title])
(def AUTHOR-SELECTOR [:head [:meta (html/attr= :name "author")]])
(def DESCRIPTION-SELECTOR [:head [:meta (html/attr= :name "description")]])
(def KEYWORDS-SELECTOR [:head [:meta (html/attr= :name "keywords")]])

(defn select-element
  ([nodes element]
     (first (html/select nodes element)))
  ([nodes container element]
     (select-element nodes (into container element))))

(defn create-post-sites
  [post-template-nodes
   {post-config-element :element
    post-config-title :title
    post-config-date :date
    post-config-content :content
    {post-config-tags-container :container
     post-config-tags-element :element} :tags}
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
           (let [filled-tags (map #(html/at tag-element-nodes
                                            post-config-tags-element (html/content %))
                                  post-tags)
                 filled-post (html/at post-element-nodes
                                      post-config-title (html/content post-title)
                                      post-config-date (html/content (.format OutputDateFormat post-date))
                                      post-config-tags-container (html/content filled-tags)
                                      post-config-content (html/content "TEST"))
                 filled-site (html/at post-template-nodes
                                      TITLE-SELECTOR (html/content post-title)
                                      AUTHOR-SELECTOR (html/set-attr :content post-author)
                                      DESCRIPTION-SELECTOR (html/set-attr :content post-meta-desc)
                                      KEYWORDS-SELECTOR (html/set-attr :content
                                                                       (apply str
                                                                              (interpose "," post-tags)))
                                      post-config-element (html/substitute filled-post))]
             (assoc post-descriptor :post-site filled-site)))
         post-descriptors)))

(defn create-post-preview-sites [blog-template-nodes
                                 {preview-config-container :container
                                  preview-config-element :element
                                  preview-config-title :title
                                  preview-config-date :date
                                  preview-config-content :content
                                  {preview-config-tags-container :container
                                   preview-config-tags-element :element} :tags}
                                 {paging-config-container :container
                                  paging-config-element :element
                                  paging-config-page :page
                                  paging-config-page-active :page-active
                                  paging-config-page-disabled :page-disabled
                                  paging-config-pages-next :pages-next
                                  paging-config-page-before :pages-before}
                                 post-preview-descriptors]
  (let [paging-element-nodes (select-element blog-template-nodes
                                             paging-config-container
                                             paging-config-element)
        preview-element-nodes (select-element blog-template-nodes
                                              preview-config-container
                                              preview-config-element)
        tag-element-nodes (select-element preview-element-nodes
                                          preview-config-tags-container
                                          preview-config-tags-element)]
    (map (fn [{:keys [post-descriptors preview-page-uri]}]
           (let [filled-posts (map (fn [{post-title :title
                                         post-date :date
                                         post-tags :tags}]
                                     {})
                                   post-descriptors)]
             (html/at blog-template-nodes
                      preview-config-container (html/content "PREVIEWS")
                      paging-config-container (html/content "PAGING"))))
         post-preview-descriptors)))

(defn transform-html [{blog-template-file :blog-template-file
                       post-template-file :post-template-file
                       post-descriptors :posts
                       post-preview-descriptors :previews
                       config-post :post
                       config-post-preview :post-preview
                       config-paging :paging}]
  (let [post-template-nodes (html/html-resource post-template-file)
        post-sites (create-post-sites post-template-nodes
                                      config-post
                                      post-descriptors)
        blog-template-nodes (html/html-resource blog-template-file)
        post-preview-sites (create-post-preview-sites blog-template-nodes
                                                      config-post-preview
                                                      config-paging
                                                      post-preview-descriptors)]
    post-sites))

(ns gutenberg.transformations
  (:require [net.cgrand.enlive-html :as html]))

(defn select-element [nodes container element]
  (first (html/select nodes (into container element))))

(defn transform-html [{index-file :index-file
                       {post-container :container
                        post-element :element :as post-preview} :post-preview
                       {paging-container :container
                        paging-element :element :as paging} :paging :as blog-descriptor}]
  (let [index-nodes (html/html-resource index-file)
        post-preview-nodes (select-element index-nodes post-container post-element)
        paging-nodes (select-element index-nodes paging-container paging-element)]
    paging-nodes))

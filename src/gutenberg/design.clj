(ns gutenberg.design)

(comment
  "Gutenberg is a blog aware static site generator. It's data driven design, using files in
   extensible data notation (edn) and markdown (md) formats as input, producing output in html.
   It will use enlive library to extract and manipulate only those parts of the site-skeleton
   file which are necessary to create blog links, summary, etc.
   The exact location of those parts is determined in the config (no surprise conventions).")

(def example-site-config
  {:author "John"
   :title "Blog about funny life"
   :meta-description "Funny blog"
   :site-skeleton "index.html"
   :outline-dir "./resources/outline/"
   :posts-dir "./resources/posts/"
   :output-dir "./resources/output/"
   :ascending-ordering true
   :menu-container [:div#menu :ul]
   :menu-element [:div#menu :ul :li]
   :menu-element-selected [:div#menu :ul :li.selected]
   :post-container [:div#posts :ul]
   :post-element [:div#posts :ul :li]
   :post-title [:div#posts :ul :li :h1]
   :post-date [:div#posts :ul :li :h2]
   :post-tags [:div#posts :ul :li :h3]})

(def example-post-config
  {:author "John"
   :title "Summer funny story"
   :meta-description "Funny post"
   :order 0
   :tags #{"Fun" "Summer"}})

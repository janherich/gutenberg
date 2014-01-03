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
   :template "index.html"
   :outline-dir "./resources/outline/"
   :posts-dir "./resources/posts/"
   :output-dir "./resources/output/"
   :ascending-ordering true
   :post-preview {:container []
                  :element []
                  :title []
                  :date []
                  :tags []
                  :content []
                  :max-characters 140}
   :paging {:container []
            :page []
            :page-active []
            :nav []
            :nav-active []}})

(def example-post-config
  {:author "John"
   :title "Summer funny story"
   :meta-description "Funny post"
   :order 0
   :tags #{"Fun" "Summer"}})

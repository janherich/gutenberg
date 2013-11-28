(ns gutenberg.design)

(comment
  "Gutenberg is a blog aware static site generator. It's data driven design, using files in 
   extensible data notation (edn) and markdown (md) formats as input, producing output in html.")

(def example-site-config
  {:author "John"
   :title "Blog about funny life"
   :meta-description "Funny blog"
   :blog-skeleton "index.html"
   :outline-dir "./resources/outline/"
   :posts-dir "./resources/posts/"
   :output-dir "./resources/output/"
   :ascending-ordering true})

(def example-post-config
  {:author "John"
   :title "Summer funny story"
   :meta-description "Funny post"
   :order 0
   :tags #{"Fun" "Summer"}})


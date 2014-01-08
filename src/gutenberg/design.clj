(ns gutenberg.design)

(comment
  "Gutenberg is a blog aware static site generator. It's data driven design, using files in
   extensible data notation (edn) and markdown (md) formats as input, producing output in html.
   It will use enlive library to extract and manipulate only those parts of the site-skeleton
   file which are necessary to create blog links, summary, etc.
   The exact location of those parts is determined in the config (no surprise conventions).")

(def example-site-config
  {:blog-template "blog.html"
   :post-template "post.html"
   :outline-dir "./resources/outline/"
   :posts-dir "./resources/posts/"
   :output-dir "./resources/output/"
   :ascending-ordering true
   :post {:element [:div#post]
          :title [:div.panel-heading :span.panel-title.post-title]
          :date [:div.panel-heading :span.post-date]
          :tags {:container [:div.panel-heading :span.post-tags]
                 :element [:span.label.label-info.post-tag]}
          :content [:div.panel-body :p]}
   :post-preview {:container [:div#posts]
                  :element [:div.panel.panel-info]
                  :title [:div.panel-heading :span.panel-title.post-title]
                  :date [:div.panel-heading :span.post-date]
                  :tags {:container [:div.panel-heading :span.post-tags]
                         :element [:span.label.label-info.post-tag]}
                  :content [:div.panel-body :p]
                  :max-characters 140}
   :paging {:container [:div#pages]
            :element [:ul.pagination]
            :page [:li :a]
            :page-active [:li.active :span]
            :page-disabled [:li.disabled :span]
            :previews-on-page 5
            :pages-shown 5
            :pages-next "&raquo;"
            :pages-before "&laquo;"}})

(def example-post-config
  {:author "John"
   :title "Summer funny story"
   :meta-description "Funny post"
   :order 0
   :tags #{"Fun" "Summer"}})

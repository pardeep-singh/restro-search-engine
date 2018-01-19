(defproject restro-search-engine "0.1.0-SNAPSHOT"
  :description "Restaurants Search Engine powered by Elasticsearch"
  :url "http://example.com/FIXME"
  :license {:name "Proprietary"}
  :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA)$"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]]
  :source-paths ["src"]
  :global-vars {*warn-on-reflection* true}
  :manifest {"Project-Name" ~#(:name %)
             "Project-Version" ~#(:version %)
             "Build-Date" ~(str (java.util.Date.))}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]]}})

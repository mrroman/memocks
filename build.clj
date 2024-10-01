(ns build
  (:refer-clojure :exclude [test])
  (:require
   [cemerick.pomegranate.aether :as aether]
   [clojure.tools.build.api :as b]))

(def lib 'org.clojars.mrroman/memocks)
(def git-tag (b/git-process {:git-args "describe --tags"}))
(def version git-tag)
(def class-dir "target/classes")

(defn test "Run all the tests."
  [opts]
  (doseq [variant [[] [:malli-10] [:malli-12] [:malli-14] [:malli-16]]]
    (println)
    (println "Testing variant..." variant)
    (let [basis    (b/create-basis {:aliases (into [:test] variant)})
          cmds     (b/java-command
                    {:basis      basis
                     :main      'clojure.main
                     :main-args ["-m" "cognitect.test-runner"]})
          {:keys [exit]} (b/process cmds)]
      (when-not (zero? exit) (throw (ex-info "Tests failed" {})))))
  opts)

(defn- jar-opts
  [opts]
  (assoc opts
         :lib lib :version version
         :jar-file (format "target/%s-%s.jar" lib version)
         :scm {:tag git-tag}
         :basis (b/create-basis {})
         :class-dir class-dir
         :target "target"
         :src-dirs ["src"]
         :pom-data [[:licenses
                     [:license
                      [:name "Eclipse Public License 1.0"]
                      [:url "https://opensource.org/license/epl-1-0/"]]]]))

(defn ci "Run the CI pipeline of tests (and build the JAR)."
  [opts]
  (test opts)
  (b/delete {:path "target"})
  (let [opts (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
    (println "\nBuilding JAR...")
    (b/jar opts))
  opts)

(defn install "Install the JAR locally."
  [opts]
  (let [opts (jar-opts opts)]
    (b/install opts))
  opts)

(defn deploy "Deploy the JAR to Clojars."
  [opts]
  (assert (re-matches #"\d+\.\d+.\d+" version)
          (str "Version " version " should match number.number.number pattern"))
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (aether/deploy :coordinates [lib version]
                   :jar-file (b/resolve-path jar-file)
                   :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))
                   :repository {"clojars" {:url "https://repo.clojars.org"
                                           :username (System/getenv "CLOJARS_USERNAME")
                                           :password (System/getenv "CLOJARS_PASSWORD")}}))
  opts)

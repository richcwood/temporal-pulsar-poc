(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir "classes")
(def basis (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path class-dir}))

(defn compile-java [_]
  (clean nil)
  (b/javac {:src-dirs ["src"]
            :class-dir class-dir
            :basis basis
            :javac-opts ["-source" "11" "-target" "11"]})
  ;; First compile the interface namespaces
;;   (b/compile-clj {:basis basis
;;                   :src-dirs ["src"]
;;                   :class-dir class-dir
;;                   :compile-opts {:direct-linking true}
;;                   :ns-compile ['com.bamboohr.interfaces.employee_workflow
;;                                'com.bamboohr.interfaces.employee_activities
;;                                'com.bamboohr.interfaces.department_workflow]})
  ;; Then compile the rest
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :compile-opts {:direct-linking true}
                  :ns-compile ['com.bamboohr.employee-workflow
                               'com.bamboohr.department-workflow
                               'com.bamboohr.worker]}))
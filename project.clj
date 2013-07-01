(defproject patagonia/java.client "0.4.0"
  :description "Nothing of note."
  :dependencies [[org.apache.httpcomponents/httpcore "4.2.4"]
                 [org.apache.httpcomponents/httpcore-nio "4.2.4"]
                 [org.apache.httpcomponents/httpclient "4.2.4"]
                 [commons-logging/commons-logging "1.1.3"]]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :omit-source true
  :jar-exclusions [#"(?:^|/).svn/"])


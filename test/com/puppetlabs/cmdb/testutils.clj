(ns com.puppetlabs.cmdb.testutils
  (:import (org.apache.activemq.broker BrokerService))
  (:require [com.puppetlabs.mq :as mq]
            [fs.core :as fs]))

(defn test-db
  "Return a map of connection attrs for an in-memory database"
  []
  {:classname "org.hsqldb.jdbcDriver"
   :subprotocol "hsqldb"
   :subname     (str "mem:"
                     (java.util.UUID/randomUUID)
                     ";shutdown=true;hsqldb.tx=mvcc;sql.syntax_pgs=true")})

(defmacro with-test-broker
  "Constructs and starts an embedded MQ, and evaluates `body` inside a
  `with-open` expression that takes care of connection cleanup and MQ
  tear-down.

  `name` - The name to use for the embedded MQ

  `conn-var` - Inside of `body`, the variable named `conn-var`
  contains an active connection to the embedded broker.

  Example:

      (with-test-broker \"my-broker\" the-connetion
        ;; Do something with the connection
        (prn the-connection))
  "
  [name conn-var & body]
  `(let [dir#                   (fs/absolute-path (fs/temp-dir))
         broker-name#           ~name
         conn-str#              (str "vm://" ~name)
         ^BrokerService broker# (mq/build-embedded-broker broker-name# dir#)]

     (.setUseJmx broker# false)
     (mq/start-broker! broker#)

     (try
       (with-open [~conn-var (mq/connect! conn-str#)]
         ~@body)
       (finally
        (mq/stop-broker! broker#)
        (fs/delete-dir dir#)))))

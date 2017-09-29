(ns clojodlee.core
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [environ.core :as env]))

;; this will be set by calling (def login (login! cobrand-creds))
(def ^:dynamic +token+ nil)

(def fastlink-appId 10003600)

(def cobrand-creds {:api-url (env/env "YODLEE_API_URL_DEV")
                    :fastlink-url (env/env "YODLEE_FASTLINK_NODE_URL_DEV")
                    :cobrand-name "restserver"
                    :cobrand-login (env/env "YODLEE_COBRAND_LOGIN_DEV")
                    :cobrand-pw (env/env "YODLEE_COBRAND_PASSWORD_DEV")
                    :cobrand-id (env/env "YODLEE_COBRAND_ID_DEV")
                    :cobrand-app-id (env/env "YODLEE_COBRAND_APP_ID")})

(def yendpoints {:login "v1/cobrand/login"
                 :logout "v1/cobrand/logout"
                 :public-key "v1/cobrand/publicKey"
                 :events "v1/cobrand/config/notification/events"

                 :user "v1/user"
                 :user-register "v1/user/register"
                 :unregister-user "v1/user/unregister"
                 :user-login "v1/user/login"
                 :user-logout "v1/user/logout"
                 :user-saml-register "v1/user/samlRegister"
                 :user-saml-login "v1/user/samlLogin"
                 :user-access-tokens "v1/user/accessTokens"
                 :user-credentials "v1/user/credentials"
                 :user-token "v1/user/credentials/token"

                 :accounts "v1/accounts"
                 :account-summary "v1/account/summary/all"
                 :investment-options "v1/accounts/investmentPlan/investmentOptions"
                 :historical-balances "v1/accounts/historicalBalances"

                 :providers "v1/providers"
                 :provider-token "v1/providers/token"

                 :statements "v1/statements"

                 :transactions "v1/transactions"
                 :transactions-count "v1/transactions/count"
                 :transactions-categories "v1/transactions/categories"})

(defn login!
  "
  Get the cobSession
  Supply the username and password from Yodlee Developer Portal
   config is a map in the form
   - username USERNAME
   - password PASSWORD
  "
  [{:keys [cobrand-login cobrand-pw]}]
  (let [auth-url (str (:api-url cobrand-creds) (:login yendpoints))
        params {:cobrandLogin cobrand-login
                :cobrandPassword cobrand-pw
                :locale "en_US"}
        resp (client/post auth-url {:form-params params})]
    (json/decode (:body resp) true)))

(defn login-user!
  [token user-login user-pw]
  (let [user-login-url (str (:api-url cobrand-creds) (:user-login yendpoints))
        params {:user
                {:loginName user-login
                 :password user-pw
                 :locale "en_US"}}
        cobSession (:cobSession (:session token))
        resp (client/post user-login-url {:headers {"Authorization" (str "{cobSession=" cobSession "}")}
                                          :body (json/encode params)})]
    (json/decode (:body resp) true)))

(defn get-endpoint
  [endpoint token user-token & query-params]
  (let [cobSession (:cobSession (:session token))
        userSession (:userSession (:session (:user user-token)))
        headers {:headers {"Authorization" (format "{userSession=%s,cobSession=%s}" userSession cobSession)}}
        url (str (:api-url cobrand-creds) (yendpoints (keyword endpoint)))
        resp (client/get url (merge headers {:query-params (or (first query-params) {})}))]
    (json/decode (:body resp) true)))

(defn fastlink
  "
  ref: https://developer.yodlee.com/Yodlee_API/FastLInk_Integration_Guide_For_Aggregation
  Entry point URL format:https://<node_domain>/authenticate/<cob_app_name>/?<channel-cob-app-name> 
  "
  [token user]
  (let [resp (get-endpoint "user-access-tokens" token user {:appIds fastlink-appId})
        fl-token (-> resp :user :accessTokens first :value)
        user-session (:userSession (:session (:user user)))
        params {:rsession user-session :token fl-token :app fastlink-appId :redirectReq true  :url "https://node.developer.yodlee.com/authenticate/restserver/"}
        post-resp (client/post "https://node.developer.yodlee.com/authenticate/restserver/" {:form-params params})]
   post-resp))

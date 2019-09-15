import {logFactory} from "../util/ConsoleLog";
import {localAxios} from "../util/Ajax";

const log = logFactory("FacebookIdentityProvider");

export default class FacebookIdentityProvider {
    constructor() {
        this._available = false;
        this._availableListeners = [];
        this._fbSdkLoaded = __api_hooks.facebookSdk.promise
            .then(() => {
                this._markAvailable();
            });
    }

    start() {
    }

    stop() {
    }

    onAvailable(listener) {
        if (this._available)
            listener();
        else
            this._availableListeners.push(listener);
    }

    onUserUpdate(listener) {
        // not supported
    }

    _markAvailable() {
        this._available = true;
        this._availableListeners.forEach(listener => {
            listener();
        });
    }

    probe() {
        return this._fbSdkLoaded
            .then(() => {
                log.info("start probing for Facebook user");
                return this._fbGetLoginStatus();
            })
            .then(({ status }) => {
                log.info("got Facebook login status", status);
                return status === "connected";
            });
    }

    autoLogin() {
        log.info("Go ahead with auto-login");
        return this._associate(FB.getAuthResponse());
    }

    confirmUser(userInfo) {
        return this._fbSdkLoaded.then(() => {
                log.info("confirm Facebook user", userInfo);
                return this._fbGetLoginStatus();
            })
            .then(({ status, authResponse }) => {
                if (status === "connected") {
                    log.info("login-status produced user response", authResponse);
                    return this._associate(authResponse);
                }
                else {
                    log.info("login-status did not produce a user", status);
                    return null;
                }
            });
    }

    signIn() {
        return this._fbLogin().then(({ status, authResponse }) => {
            if (status === "connected") {
                log.info("sign-in produced user response", authResponse);
                return this._associate(authResponse);
            }
            else {
                log.info("sign-in did not produce a user", status);
                return null;
            }
        });
    }

    signOut() {
        log.info("Sign out");
        return this._fbLogout();
    }

    _associate(authResponse) {
        return localAxios.post('/_api/user/identity/facebook', authResponse.accessToken, { headers: { "Content-Type": "text/plain" } })
            .then(({data}) => {
                return this._fbApi("/me/picture").then(({ data: pictureData }) => {
                    if (pictureData.url !== data.picture) {
                        log.info("got a different picture over JS API", data.picture, pictureData.url);
                        return { ...data, picture: pictureData.url };
                    }
                    else {
                        return data;
                    }
                })
            });
    }

    _fbGetLoginStatus() {
        return new Promise(resolve => {
            FB.getLoginStatus(facebookStatus => {
                resolve(facebookStatus);
            });
        });
    }

    _fbApi(path) {
        return new Promise(resolve => {
            FB.api(path, response => {
                resolve(response);
            });
        });
    }

    _fbLogin() {
        return new Promise(resolve => {
            FB.login(response => {
                resolve(response);
            });
        });
    }

    _fbLogout() {
        return new Promise(resolve => {
            FB.logout(response => {
                resolve(response);
            });
        });
    }
}

import axios from "axios";
import {logFactory} from "../util/ConsoleLog";

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
            .then(facebookStatus => {
                log.info("got Facebook login status", facebookStatus);
                return facebookStatus.status === "connected";
            });
    }

    autoLogin() {
        log.info("Go ahead with auto-login");
        return this._pullUserData();
    }

    confirmUser(userInfo) {
        return this._fbSdkLoaded.then(() => {
                log.info("confirm Facebook user", userInfo);
                return this._fbGetLoginStatus();
            })
            .then(facebookStatus => {
                log.info("Got Facebook user", facebookStatus);
                if (facebookStatus.status === "connected") {
                    return this._pullUserData();
                }
                else {
                    return null;
                }
            });
    }

    signIn() {
        return this._fbLogin().then(facebookStatus => {
            log.info("Signed in; got Facebook status", facebookStatus);
            if (facebookStatus.status === "connected") {
                return this._pullUserData();
            }
            else {
                return null;
            }
        });
    }

    signOut() {
        log.info("Sign out");
        return this._fbLogout();
    }

    _pullUserData() {
        return Promise.all([this._fbApi("/me"), this._fbApi("/me/picture")]).then(([me, myPicture]) => {
            log.info("me", me, myPicture);
            return axios.post('/_api/user/identity/facebook', { id: me.id, name: me.name, picture: myPicture.data.url }).then(({data}) => data)
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

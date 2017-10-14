import {Datum, StoreBase} from "../util/Stores";
import {logFactory} from "../util/ConsoleLog";
import BUS from "../message-bus";

const log = logFactory("FacebookIdentityProvider");

export default class FacebookIdentityProvider extends StoreBase {
    constructor() {
        super();
        this._available = new Datum(this, "available");
        if ('FB' in window) {
            log.info("Facebook API loaded early");
            this._fbSdkLoaded = Promise.resolve();
        }
        else {
            this._fbSdkLoaded = new Promise(resolve => {
                BUS.subscribe('FacebookSdk.Loaded', () => {
                    log.info("Facebook API loaded late");
                    resolve();
                });
            });
        }
        this._fbSdkLoaded.then(() => {
            this._available.value = true;
        });
    }

    get available() {
        return this._available.facade();
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
            return this.callAjax({
                url: '/_api/user/identity/facebook',
                type: 'POST',
                data: { id: me.id, name: me.name, picture: myPicture.data.url }
            });
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

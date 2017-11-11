import {combineReducers} from "redux";
import {reducer as userIdentity} from "./stores/IdentityStore";
import {reducer as preferences} from "./stores/PreferencesStore";
import {reducer as purchases} from "./stores/PurchasesStore";

export default combineReducers({
    userIdentity,
    preferences,
    purchases
});
